//! JNI bindings for WASI operations
//!
//! This module provides JNI-compatible functions for WASI context management.

#[cfg(feature = "jni-bindings")]
use jni::objects::JByteArray;
#[cfg(feature = "jni-bindings")]
use jni::objects::{JClass, JObject, JObjectArray, JString};
#[cfg(feature = "jni-bindings")]
use jni::sys::{jboolean, jbyteArray, jint, jlong, jobjectArray};
#[cfg(feature = "jni-bindings")]
use jni::JNIEnv;

/// JNI bindings for WASI operations
#[cfg(feature = "jni-bindings")]
pub mod jni_wasi {
    use super::*;
    use crate::error::jni_utils;
    use crate::wasi;
    use std::ffi::CString;
    use std::os::raw::{c_char, c_void};

    /// Create a new WASI context with specified configuration (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiContext_nativeCreate(
        mut env: JNIEnv,
        _class: JClass,
        environment: jobjectArray,
        arguments: jobjectArray,
        preopen_dirs: jobjectArray,
        working_dir: JString,
    ) -> jlong {
        match (|| -> crate::error::WasmtimeResult<*mut c_void> {
            // Convert working directory string
            let _working_dir_str = if working_dir.is_null() {
                None
            } else {
                let wd_string: String = env.get_string(&working_dir)?.into();
                Some(wd_string)
            };

            // Create default WASI context first
            let ctx_ptr = unsafe { wasi::wasi_ctx_new() };
            if ctx_ptr.is_null() {
                return Err(crate::error::WasmtimeError::Wasi {
                    message: "Failed to create WASI context".to_string(),
                });
            }

            // Add environment variables if provided
            if !environment.is_null() {
                let env_array = JObjectArray::from(unsafe { JObject::from_raw(environment) });
                let env_len = env.get_array_length(&env_array)?;

                for i in 0..env_len {
                    let env_entry = env.get_object_array_element(&env_array, i)?;
                    if !env_entry.is_null() {
                        let env_str: String = env.get_string(&JString::from(env_entry))?.into();

                        // Parse "KEY=VALUE" format
                        if let Some(eq_pos) = env_str.find('=') {
                            let key = &env_str[..eq_pos];
                            let value = &env_str[eq_pos + 1..];

                            let key_cstr = CString::new(key).map_err(|_| {
                                crate::error::WasmtimeError::Wasi {
                                    message: format!("Invalid environment key: {}", key),
                                }
                            })?;
                            let value_cstr = CString::new(value).map_err(|_| {
                                crate::error::WasmtimeError::Wasi {
                                    message: format!("Invalid environment value: {}", value),
                                }
                            })?;

                            let result = unsafe {
                                wasi::wasi_ctx_set_env(
                                    ctx_ptr,
                                    key_cstr.as_ptr(),
                                    value_cstr.as_ptr(),
                                )
                            };

                            if result != 0 {
                                unsafe {
                                    wasi::wasi_ctx_destroy(ctx_ptr);
                                }
                                return Err(crate::error::WasmtimeError::Wasi {
                                    message: format!(
                                        "Failed to set environment variable {}={}",
                                        key, value
                                    ),
                                });
                            }
                        }
                    }
                }
            }

            // Add command line arguments if provided
            if !arguments.is_null() {
                let args_array = JObjectArray::from(unsafe { JObject::from_raw(arguments) });
                let args_len = env.get_array_length(&args_array)?;

                let mut arg_cstrs = Vec::new();
                let mut arg_ptrs = Vec::new();

                for i in 0..args_len {
                    let arg_entry = env.get_object_array_element(&args_array, i)?;
                    if !arg_entry.is_null() {
                        let arg_str: String = env.get_string(&JString::from(arg_entry))?.into();
                        let arg_cstr = CString::new(arg_str).map_err(|_| {
                            crate::error::WasmtimeError::Wasi {
                                message: "Invalid argument string".to_string(),
                            }
                        })?;
                        arg_ptrs.push(arg_cstr.as_ptr());
                        arg_cstrs.push(arg_cstr);
                    }
                }

                if !arg_ptrs.is_empty() {
                    let result = unsafe {
                        wasi::wasi_ctx_set_args(ctx_ptr, arg_ptrs.as_ptr(), arg_ptrs.len())
                    };

                    if result != 0 {
                        unsafe {
                            wasi::wasi_ctx_destroy(ctx_ptr);
                        }
                        return Err(crate::error::WasmtimeError::Wasi {
                            message: "Failed to set command line arguments".to_string(),
                        });
                    }
                }
            }

            // Add pre-opened directories if provided
            if !preopen_dirs.is_null() {
                let dirs_array = JObjectArray::from(unsafe { JObject::from_raw(preopen_dirs) });
                let dirs_len = env.get_array_length(&dirs_array)?;

                for i in 0..dirs_len {
                    let dir_entry = env.get_object_array_element(&dirs_array, i)?;
                    if !dir_entry.is_null() {
                        let dir_str: String = env.get_string(&JString::from(dir_entry))?.into();

                        // Parse "HOST_PATH:GUEST_PATH" format
                        if let Some(colon_pos) = dir_str.find(':') {
                            let host_path = &dir_str[..colon_pos];
                            let guest_path = &dir_str[colon_pos + 1..];

                            let host_cstr = CString::new(host_path).map_err(|_| {
                                crate::error::WasmtimeError::Wasi {
                                    message: format!("Invalid host path: {}", host_path),
                                }
                            })?;
                            let guest_cstr = CString::new(guest_path).map_err(|_| {
                                crate::error::WasmtimeError::Wasi {
                                    message: format!("Invalid guest path: {}", guest_path),
                                }
                            })?;

                            // Default permissions: read-only access
                            let result = unsafe {
                                wasi::wasi_ctx_add_dir(
                                    ctx_ptr,
                                    host_cstr.as_ptr(),
                                    guest_cstr.as_ptr(),
                                    0,
                                    1,
                                    0, // dir perms: no create, read, no remove
                                    1,
                                    0,
                                    0,
                                    0, // file perms: read only
                                )
                            };

                            if result != 0 {
                                unsafe {
                                    wasi::wasi_ctx_destroy(ctx_ptr);
                                }
                                return Err(crate::error::WasmtimeError::Wasi {
                                    message: format!(
                                        "Failed to add directory mapping {}:{}",
                                        host_path, guest_path
                                    ),
                                });
                            }
                        }
                    }
                }
            }

            Ok(ctx_ptr)
        })() {
            Ok(ptr) => ptr as jlong,
            Err(error) => {
                jni_utils::throw_jni_exception(&mut env, &error);
                0
            }
        }
    }

    /// Close a WASI context and free its resources (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiContext_nativeClose(
        _env: JNIEnv,
        _class: JClass,
        handle: jlong,
    ) {
        if handle != 0 {
            unsafe {
                wasi::wasi_ctx_destroy(handle as *mut c_void);
            }
        }
    }

    /// Add a directory mapping to an existing WASI context (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiContext_nativeAddDirectory(
        mut env: JNIEnv,
        _class: JClass,
        handle: jlong,
        host_path: JString,
        guest_path: JString,
        can_read: jboolean,
        can_write: jboolean,
        can_create: jboolean,
    ) -> jboolean {
        // Extract strings outside the closure to avoid borrowing conflicts
        let host_str_result = env.get_string(&host_path);
        let guest_str_result = env.get_string(&guest_path);

        jni_utils::jni_try_with_default(&mut env, 0, || {
            if handle == 0 {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "WASI context handle cannot be null".to_string(),
                });
            }

            let host_str: String = host_str_result?.into();
            let guest_str: String = guest_str_result?.into();

            let host_cstr =
                CString::new(host_str).map_err(|_| crate::error::WasmtimeError::Wasi {
                    message: "Invalid host path string".to_string(),
                })?;
            let guest_cstr =
                CString::new(guest_str).map_err(|_| crate::error::WasmtimeError::Wasi {
                    message: "Invalid guest path string".to_string(),
                })?;

            let result = unsafe {
                wasi::wasi_ctx_add_dir(
                    handle as *mut c_void,
                    host_cstr.as_ptr(),
                    guest_cstr.as_ptr(),
                    can_create as i32,
                    can_read as i32,
                    0, // dir perms
                    can_read as i32,
                    can_write as i32,
                    can_create as i32,
                    0, // file perms
                )
            };

            if result == 0 {
                Ok(1) // Success
            } else {
                Err(crate::error::WasmtimeError::Wasi {
                    message: "Failed to add directory mapping".to_string(),
                })
            }
        })
    }

    /// Set an environment variable in an existing WASI context (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiContext_nativeSetEnvironmentVariable(
        mut env: JNIEnv,
        _class: JClass,
        handle: jlong,
        key: JString,
        value: JString,
    ) -> jboolean {
        // Extract strings outside the closure to avoid borrowing conflicts
        let key_str_result = env.get_string(&key);
        let value_str_result = env.get_string(&value);

        jni_utils::jni_try_with_default(&mut env, 0, || {
            if handle == 0 {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "WASI context handle cannot be null".to_string(),
                });
            }

            let key_str: String = key_str_result?.into();
            let value_str: String = value_str_result?.into();

            let key_cstr =
                CString::new(key_str).map_err(|_| crate::error::WasmtimeError::Wasi {
                    message: "Invalid environment key string".to_string(),
                })?;
            let value_cstr =
                CString::new(value_str).map_err(|_| crate::error::WasmtimeError::Wasi {
                    message: "Invalid environment value string".to_string(),
                })?;

            let result = unsafe {
                wasi::wasi_ctx_set_env(
                    handle as *mut c_void,
                    key_cstr.as_ptr(),
                    value_cstr.as_ptr(),
                )
            };

            if result == 0 {
                Ok(1) // Success
            } else {
                Err(crate::error::WasmtimeError::Wasi {
                    message: "Failed to set environment variable".to_string(),
                })
            }
        })
    }

    /// Check if a path is allowed by the WASI context (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiContext_nativeIsPathAllowed(
        mut env: JNIEnv,
        _class: JClass,
        handle: jlong,
        path: JString,
    ) -> jboolean {
        // Extract string outside the closure to avoid borrowing conflicts
        let path_str_result = env.get_string(&path);

        jni_utils::jni_try_with_default(&mut env, 0, || {
            if handle == 0 {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "WASI context handle cannot be null".to_string(),
                });
            }

            let path_str: String = path_str_result?.into();
            let path_cstr =
                CString::new(path_str).map_err(|_| crate::error::WasmtimeError::Wasi {
                    message: "Invalid path string".to_string(),
                })?;

            let result = unsafe {
                wasi::wasi_ctx_is_path_allowed(handle as *const c_void, path_cstr.as_ptr())
            };

            // wasi_ctx_is_path_allowed returns 0 or 1, which always fits in jboolean (u8)
            Ok(result.try_into().expect("boolean result always fits in u8"))
        })
    }

    /// Get the number of environment variables in the WASI context (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiContext_nativeGetEnvironmentCount(
        mut env: JNIEnv,
        _class: JClass,
        handle: jlong,
    ) -> jint {
        jni_utils::jni_try_with_default(&mut env, 0, || {
            if handle == 0 {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "WASI context handle cannot be null".to_string(),
                });
            }

            let count = unsafe { wasi::wasi_ctx_get_env_count(handle as *const c_void) };

            Ok(count as jint)
        })
    }

    /// Get the number of command line arguments in the WASI context (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiContext_nativeGetArgumentCount(
        mut env: JNIEnv,
        _class: JClass,
        handle: jlong,
    ) -> jint {
        jni_utils::jni_try_with_default(&mut env, 0, || {
            if handle == 0 {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "WASI context handle cannot be null".to_string(),
                });
            }

            let count = unsafe { wasi::wasi_ctx_get_args_count(handle as *const c_void) };

            Ok(count as jint)
        })
    }

    /// Get the number of directory mappings in the WASI context (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiContext_nativeGetDirectoryCount(
        mut env: JNIEnv,
        _class: JClass,
        handle: jlong,
    ) -> jint {
        jni_utils::jni_try_with_default(&mut env, 0, || {
            if handle == 0 {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "WASI context handle cannot be null".to_string(),
                });
            }

            let count = unsafe { wasi::wasi_ctx_get_dir_count(handle as *const c_void) };

            Ok(count as jint)
        })
    }

    /// Add a WASI context to a Store for WebAssembly instance creation (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiContext_nativeAddToStore(
        mut env: JNIEnv,
        _class: JClass,
        wasi_handle: jlong,
        store_handle: jlong,
    ) -> jboolean {
        jni_utils::jni_try_with_default(&mut env, 0, || {
            if wasi_handle == 0 {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "WASI context handle cannot be null".to_string(),
                });
            }
            if store_handle == 0 {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }

            let result = unsafe {
                wasi::wasi_ctx_add_to_store(
                    wasi_handle as *const c_void,
                    store_handle as *mut c_void,
                )
            };

            if result == 0 {
                Ok(1) // Success
            } else {
                Err(crate::error::WasmtimeError::Wasi {
                    message: "Failed to add WASI context to Store".to_string(),
                })
            }
        })
    }

    /// Get the WASI context from a Store if one is attached (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiContext_nativeGetFromStore(
        mut env: JNIEnv,
        _class: JClass,
        store_handle: jlong,
    ) -> jlong {
        jni_utils::jni_try_with_default(&mut env, 0, || {
            if store_handle == 0 {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }

            let wasi_ptr = unsafe { wasi::wasi_ctx_get_from_store(store_handle as *const c_void) };

            Ok(wasi_ptr as jlong)
        })
    }

    /// Check if a Store has a WASI context attached (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiContext_nativeStoreHasWasi(
        mut env: JNIEnv,
        _class: JClass,
        store_handle: jlong,
    ) -> jboolean {
        jni_utils::jni_try_with_default(&mut env, 0, || {
            if store_handle == 0 {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }

            let result = unsafe { wasi::wasi_ctx_store_has_wasi(store_handle as *const c_void) };

            // wasi_ctx_store_has_wasi returns 0 or 1, which always fits in jboolean (u8)
            Ok(result.try_into().expect("boolean result always fits in u8"))
        })
    }

    // JniWasiContextImpl implementations that delegate to wasi module C FFI functions

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativeSetArgv(
        mut env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
        argv: jobjectArray,
    ) -> jint {
        if context_handle == 0 || argv.is_null() {
            return -1;
        }

        match (|| -> crate::error::WasmtimeResult<i32> {
            let argv_array = JObjectArray::from(unsafe { JObject::from_raw(argv) });
            let argv_len = env.get_array_length(&argv_array)? as usize;

            let mut arg_cstrs = Vec::new();
            let mut arg_ptrs = Vec::new();

            for i in 0..argv_len {
                let arg_entry = env.get_object_array_element(&argv_array, i as i32)?;
                if !arg_entry.is_null() {
                    let arg_str: String = env.get_string(&JString::from(arg_entry))?.into();
                    let arg_cstr = CString::new(arg_str).map_err(|_| {
                        crate::error::WasmtimeError::InvalidParameter {
                            message: "Invalid argument string".to_string(),
                        }
                    })?;
                    arg_ptrs.push(arg_cstr.as_ptr());
                    arg_cstrs.push(arg_cstr);
                }
            }

            let result = unsafe {
                wasi::wasmtime4j_wasi_context_set_argv(
                    context_handle as *mut c_void,
                    arg_ptrs.as_ptr(),
                    arg_ptrs.len(),
                )
            };
            Ok(result)
        })() {
            Ok(result) => result,
            Err(_) => -1,
        }
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativeSetEnv(
        mut env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
        key: JString,
        value: JString,
    ) -> jint {
        if context_handle == 0 {
            return -1;
        }

        match (|| -> crate::error::WasmtimeResult<i32> {
            let key_str: String = env.get_string(&key)?.into();
            let value_str: String = env.get_string(&value)?.into();

            let key_cstr = CString::new(key_str).map_err(|_| {
                crate::error::WasmtimeError::InvalidParameter {
                    message: "Invalid key string".to_string(),
                }
            })?;
            let value_cstr = CString::new(value_str).map_err(|_| {
                crate::error::WasmtimeError::InvalidParameter {
                    message: "Invalid value string".to_string(),
                }
            })?;

            let result = unsafe {
                wasi::wasmtime4j_wasi_context_set_env(
                    context_handle as *mut c_void,
                    key_cstr.as_ptr(),
                    value_cstr.as_ptr(),
                )
            };
            Ok(result)
        })() {
            Ok(result) => result,
            Err(_) => -1,
        }
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativeInheritEnv(
        _env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
    ) -> jint {
        if context_handle == 0 {
            return -1;
        }
        unsafe { wasi::wasmtime4j_wasi_context_inherit_env(context_handle as *mut c_void) }
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativeInheritArgs(
        _env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
    ) -> jint {
        if context_handle == 0 {
            return -1;
        }
        unsafe { wasi::wasmtime4j_wasi_context_inherit_args(context_handle as *mut c_void) }
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativeInheritStdio(
        _env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
    ) -> jint {
        if context_handle == 0 {
            return -1;
        }
        unsafe { wasi::wasmtime4j_wasi_context_inherit_stdio(context_handle as *mut c_void) }
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativeSetStdin(
        mut env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
        path: JString,
    ) -> jint {
        if context_handle == 0 {
            return -1;
        }

        match (|| -> crate::error::WasmtimeResult<i32> {
            let path_str: String = env.get_string(&path)?.into();
            let path_cstr = CString::new(path_str).map_err(|_| {
                crate::error::WasmtimeError::InvalidParameter {
                    message: "Invalid path string".to_string(),
                }
            })?;

            let result = unsafe {
                wasi::wasmtime4j_wasi_context_set_stdin(
                    context_handle as *mut c_void,
                    path_cstr.as_ptr(),
                )
            };
            Ok(result)
        })() {
            Ok(result) => result,
            Err(_) => -1,
        }
    }

    /// Set stdin from binary byte array data (supports binary data with null bytes)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativeSetStdinBytes(
        mut env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
        data: jbyteArray,
    ) -> jint {
        match (|| -> crate::error::WasmtimeResult<i32> {
            if context_handle == 0 {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "WASI context handle cannot be null".to_string(),
                });
            }

            // Handle null or empty array
            if data.is_null() {
                let result = unsafe {
                    wasi::wasmtime4j_wasi_context_set_stdin_bytes(
                        context_handle as *mut c_void,
                        std::ptr::null(),
                        0,
                    )
                };
                return Ok(result);
            }

            // Get byte array data
            let data_ref = unsafe { JByteArray::from_raw(data) };
            let data_len = env.get_array_length(&data_ref).map_err(|e| {
                crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Failed to get byte array length: {}", e),
                }
            })? as usize;

            if data_len == 0 {
                let result = unsafe {
                    wasi::wasmtime4j_wasi_context_set_stdin_bytes(
                        context_handle as *mut c_void,
                        std::ptr::null(),
                        0,
                    )
                };
                return Ok(result);
            }

            // Copy byte array to Rust Vec
            let mut bytes = vec![0i8; data_len];
            env.get_byte_array_region(&data_ref, 0, &mut bytes)
                .map_err(|e| crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Failed to copy byte array: {}", e),
                })?;

            // Convert i8 to u8 and call native function
            let bytes_u8: Vec<u8> = bytes.into_iter().map(|b| b as u8).collect();
            let result = unsafe {
                wasi::wasmtime4j_wasi_context_set_stdin_bytes(
                    context_handle as *mut c_void,
                    bytes_u8.as_ptr(),
                    bytes_u8.len(),
                )
            };
            Ok(result)
        })() {
            Ok(result) => result,
            Err(e) => {
                jni_utils::throw_jni_exception(&mut env, &e);
                -1
            }
        }
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativeSetStdout(
        mut env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
        path: JString,
    ) -> jint {
        if context_handle == 0 {
            return -1;
        }

        match (|| -> crate::error::WasmtimeResult<i32> {
            let path_str: String = env.get_string(&path)?.into();
            let path_cstr = CString::new(path_str).map_err(|_| {
                crate::error::WasmtimeError::InvalidParameter {
                    message: "Invalid path string".to_string(),
                }
            })?;

            let result = unsafe {
                wasi::wasmtime4j_wasi_context_set_stdout(
                    context_handle as *mut c_void,
                    path_cstr.as_ptr(),
                )
            };
            Ok(result)
        })() {
            Ok(result) => result,
            Err(_) => -1,
        }
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativeSetStderr(
        mut env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
        path: JString,
    ) -> jint {
        if context_handle == 0 {
            return -1;
        }

        match (|| -> crate::error::WasmtimeResult<i32> {
            let path_str: String = env.get_string(&path)?.into();
            let path_cstr = CString::new(path_str).map_err(|_| {
                crate::error::WasmtimeError::InvalidParameter {
                    message: "Invalid path string".to_string(),
                }
            })?;

            let result = unsafe {
                wasi::wasmtime4j_wasi_context_set_stderr(
                    context_handle as *mut c_void,
                    path_cstr.as_ptr(),
                )
            };
            Ok(result)
        })() {
            Ok(result) => result,
            Err(_) => -1,
        }
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativeSetStdoutAppend(
        mut env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
        path: JString,
    ) -> jint {
        if context_handle == 0 {
            return -1;
        }

        match (|| -> crate::error::WasmtimeResult<i32> {
            let path_str: String = env.get_string(&path)?.into();
            let path_cstr = CString::new(path_str).map_err(|_| {
                crate::error::WasmtimeError::InvalidParameter {
                    message: "Invalid path string".to_string(),
                }
            })?;

            let result = unsafe {
                wasi::wasmtime4j_wasi_context_set_stdout_append(
                    context_handle as *mut c_void,
                    path_cstr.as_ptr(),
                )
            };
            Ok(result)
        })() {
            Ok(result) => result,
            Err(_) => -1,
        }
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativeSetStderrAppend(
        mut env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
        path: JString,
    ) -> jint {
        if context_handle == 0 {
            return -1;
        }

        match (|| -> crate::error::WasmtimeResult<i32> {
            let path_str: String = env.get_string(&path)?.into();
            let path_cstr = CString::new(path_str).map_err(|_| {
                crate::error::WasmtimeError::InvalidParameter {
                    message: "Invalid path string".to_string(),
                }
            })?;

            let result = unsafe {
                wasi::wasmtime4j_wasi_context_set_stderr_append(
                    context_handle as *mut c_void,
                    path_cstr.as_ptr(),
                )
            };
            Ok(result)
        })() {
            Ok(result) => result,
            Err(_) => -1,
        }
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativePreopenedDir(
        mut env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
        host_path: JString,
        guest_path: JString,
    ) -> jint {
        if context_handle == 0 {
            return -1;
        }

        match (|| -> crate::error::WasmtimeResult<i32> {
            let host_str: String = env.get_string(&host_path)?.into();
            let guest_str: String = env.get_string(&guest_path)?.into();

            let host_cstr = CString::new(host_str).map_err(|_| {
                crate::error::WasmtimeError::InvalidParameter {
                    message: "Invalid host path string".to_string(),
                }
            })?;
            let guest_cstr = CString::new(guest_str).map_err(|_| {
                crate::error::WasmtimeError::InvalidParameter {
                    message: "Invalid guest path string".to_string(),
                }
            })?;

            let result = unsafe {
                wasi::wasmtime4j_wasi_context_preopen_dir(
                    context_handle as *mut c_void,
                    host_cstr.as_ptr(),
                    guest_cstr.as_ptr(),
                )
            };
            Ok(result)
        })() {
            Ok(result) => result,
            Err(_) => -1,
        }
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativePreopenedDirReadOnly(
        mut env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
        host_path: JString,
        guest_path: JString,
    ) -> jint {
        if context_handle == 0 {
            return -1;
        }

        match (|| -> crate::error::WasmtimeResult<i32> {
            let host_str: String = env.get_string(&host_path)?.into();
            let guest_str: String = env.get_string(&guest_path)?.into();

            let host_cstr = CString::new(host_str).map_err(|_| {
                crate::error::WasmtimeError::InvalidParameter {
                    message: "Invalid host path string".to_string(),
                }
            })?;
            let guest_cstr = CString::new(guest_str).map_err(|_| {
                crate::error::WasmtimeError::InvalidParameter {
                    message: "Invalid guest path string".to_string(),
                }
            })?;

            let result = unsafe {
                wasi::wasmtime4j_wasi_context_preopen_dir_readonly(
                    context_handle as *mut c_void,
                    host_cstr.as_ptr(),
                    guest_cstr.as_ptr(),
                )
            };
            Ok(result)
        })() {
            Ok(result) => result,
            Err(_) => -1,
        }
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativeSetWorkingDirectory(
        mut env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
        working_dir: JString,
    ) -> jint {
        if context_handle == 0 {
            return -1;
        }

        match (|| -> crate::error::WasmtimeResult<i32> {
            let dir_str: String = env.get_string(&working_dir)?.into();
            let dir_cstr = CString::new(dir_str).map_err(|_| {
                crate::error::WasmtimeError::InvalidParameter {
                    message: "Invalid working directory string".to_string(),
                }
            })?;

            // Use preopen_dir to add working directory access
            let result = unsafe {
                wasi::wasmtime4j_wasi_context_preopen_dir(
                    context_handle as *mut c_void,
                    dir_cstr.as_ptr(),
                    dir_cstr.as_ptr(),
                )
            };
            Ok(result)
        })() {
            Ok(result) => result,
            Err(_) => -1,
        }
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativeSetNetworkEnabled(
        _env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
        enabled: jboolean,
    ) -> jint {
        if context_handle == 0 {
            return -1;
        }
        let wasi_tuple = unsafe {
            &mut *(context_handle
                as *mut (
                    crate::wasi::WasiContext,
                    crate::wasi::WasiFileDescriptorManager,
                ))
        };
        wasi_tuple.0.network_enabled = enabled != 0;
        0
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativeSetMaxOpenFiles(
        _env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
        _max_fds: jint,
    ) -> jint {
        // Max open files is handled by resource limits in wasmtime
        // This is a no-op as limits are set at store level
        if context_handle == 0 {
            return -1;
        }
        0 // Success
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativeSetAsyncIoEnabled(
        _env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
        enabled: jboolean,
    ) -> jint {
        if context_handle == 0 {
            return -1;
        }
        let wasi_tuple = unsafe {
            &mut *(context_handle
                as *mut (
                    crate::wasi::WasiContext,
                    crate::wasi::WasiFileDescriptorManager,
                ))
        };
        wasi_tuple.0.async_io_enabled = enabled != 0;
        0
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativeSetMaxAsyncOperations(
        _env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
        max_ops: jint,
    ) -> jint {
        if context_handle == 0 {
            return -1;
        }
        let wasi_tuple = unsafe {
            &mut *(context_handle
                as *mut (
                    crate::wasi::WasiContext,
                    crate::wasi::WasiFileDescriptorManager,
                ))
        };
        wasi_tuple.0.max_async_operations = max_ops;
        0
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativeSetAsyncTimeout(
        _env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
        timeout_ms: jlong,
    ) -> jint {
        if context_handle == 0 {
            return -1;
        }
        let wasi_tuple = unsafe {
            &mut *(context_handle
                as *mut (
                    crate::wasi::WasiContext,
                    crate::wasi::WasiFileDescriptorManager,
                ))
        };
        wasi_tuple.0.async_timeout_ms = timeout_ms;
        0
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativeSetComponentModelEnabled(
        _env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
        enabled: jboolean,
    ) -> jint {
        if context_handle == 0 {
            return -1;
        }
        let wasi_tuple = unsafe {
            &mut *(context_handle
                as *mut (
                    crate::wasi::WasiContext,
                    crate::wasi::WasiFileDescriptorManager,
                ))
        };
        wasi_tuple.0.component_model_enabled = enabled != 0;
        0
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativeSetProcessEnabled(
        _env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
        enabled: jboolean,
    ) -> jint {
        if context_handle == 0 {
            return -1;
        }
        let wasi_tuple = unsafe {
            &mut *(context_handle
                as *mut (
                    crate::wasi::WasiContext,
                    crate::wasi::WasiFileDescriptorManager,
                ))
        };
        wasi_tuple.0.process_enabled = enabled != 0;
        0
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativeSetFilesystemWorkingDir(
        mut env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
        working_dir: JString,
    ) -> jint {
        if context_handle == 0 {
            return -1;
        }

        match (|| -> crate::error::WasmtimeResult<i32> {
            let dir_str: String = env.get_string(&working_dir)?.into();
            let dir_cstr = CString::new(dir_str).map_err(|_| {
                crate::error::WasmtimeError::InvalidParameter {
                    message: "Invalid working directory string".to_string(),
                }
            })?;

            // Use preopen_dir to set filesystem working directory
            let result = unsafe {
                wasi::wasmtime4j_wasi_context_preopen_dir(
                    context_handle as *mut c_void,
                    dir_cstr.as_ptr(),
                    b".\0".as_ptr() as *const c_char,
                )
            };
            Ok(result)
        })() {
            Ok(result) => result,
            Err(_) => -1,
        }
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativePreopenedDirWithPermissions(
        mut env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
        host_path: JString,
        guest_path: JString,
        can_read: jboolean,
        can_write: jboolean,
        can_create: jboolean,
        can_delete: jboolean,
    ) -> jint {
        if context_handle == 0 {
            return -1;
        }

        match (|| -> crate::error::WasmtimeResult<i32> {
            let host_str: String = env.get_string(&host_path)?.into();
            let guest_str: String = env.get_string(&guest_path)?.into();

            let host_cstr = CString::new(host_str).map_err(|_| {
                crate::error::WasmtimeError::InvalidParameter {
                    message: "Invalid host path string".to_string(),
                }
            })?;
            let guest_cstr = CString::new(guest_str).map_err(|_| {
                crate::error::WasmtimeError::InvalidParameter {
                    message: "Invalid guest path string".to_string(),
                }
            })?;

            // Note: can_delete is not supported in the underlying API, using can_create instead
            let _ = can_delete; // Suppress unused warning
            let result = unsafe {
                wasi::wasmtime4j_wasi_context_preopen_dir_with_perms(
                    context_handle as *mut c_void,
                    host_cstr.as_ptr(),
                    guest_cstr.as_ptr(),
                    can_read as i32,
                    can_write as i32,
                    can_create as i32,
                )
            };
            Ok(result)
        })() {
            Ok(result) => result,
            Err(_) => -1,
        }
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativeCleanup(
        _env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
    ) {
        // Cleanup is handled by the WASI context destructor
        // This is a no-op as Java will call nativeClose separately
        if context_handle != 0 {
            // Resources are cleaned up when the context is closed
        }
    }

    // ===== Output Capture JNI methods =====

    /// Enable output capture for stdout and stderr
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativeEnableOutputCapture(
        _env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
    ) -> jint {
        if context_handle == 0 {
            return -1;
        }
        unsafe {
            wasi::wasmtime4j_wasi_context_enable_output_capture(context_handle as *mut c_void)
        }
    }

    /// Get captured stdout data as byte array
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativeGetStdoutCapture(
        mut env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
    ) -> jbyteArray {
        if context_handle == 0 {
            return std::ptr::null_mut();
        }

        let mut len: usize = 0;
        let data_ptr = unsafe {
            wasi::wasmtime4j_wasi_context_get_stdout_capture(
                context_handle as *const c_void,
                &mut len as *mut usize,
            )
        };

        if data_ptr.is_null() || len == 0 {
            return std::ptr::null_mut();
        }

        // Create Java byte array and copy data
        match env.new_byte_array(len as i32) {
            Ok(byte_array) => {
                let data_slice = unsafe { std::slice::from_raw_parts(data_ptr, len) };
                // Convert u8 to i8 for JNI
                let data_i8: Vec<i8> = data_slice.iter().map(|&b| b as i8).collect();
                if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                    // Free the native buffer
                    unsafe {
                        wasi::wasmtime4j_wasi_free_capture_buffer(data_ptr, len);
                    }
                    byte_array.into_raw()
                } else {
                    unsafe {
                        wasi::wasmtime4j_wasi_free_capture_buffer(data_ptr, len);
                    }
                    std::ptr::null_mut()
                }
            }
            Err(_) => {
                unsafe {
                    wasi::wasmtime4j_wasi_free_capture_buffer(data_ptr, len);
                }
                std::ptr::null_mut()
            }
        }
    }

    /// Get captured stderr data as byte array
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativeGetStderrCapture(
        mut env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
    ) -> jbyteArray {
        if context_handle == 0 {
            return std::ptr::null_mut();
        }

        let mut len: usize = 0;
        let data_ptr = unsafe {
            wasi::wasmtime4j_wasi_context_get_stderr_capture(
                context_handle as *const c_void,
                &mut len as *mut usize,
            )
        };

        if data_ptr.is_null() || len == 0 {
            return std::ptr::null_mut();
        }

        // Create Java byte array and copy data
        match env.new_byte_array(len as i32) {
            Ok(byte_array) => {
                let data_slice = unsafe { std::slice::from_raw_parts(data_ptr, len) };
                // Convert u8 to i8 for JNI
                let data_i8: Vec<i8> = data_slice.iter().map(|&b| b as i8).collect();
                if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                    // Free the native buffer
                    unsafe {
                        wasi::wasmtime4j_wasi_free_capture_buffer(data_ptr, len);
                    }
                    byte_array.into_raw()
                } else {
                    unsafe {
                        wasi::wasmtime4j_wasi_free_capture_buffer(data_ptr, len);
                    }
                    std::ptr::null_mut()
                }
            }
            Err(_) => {
                unsafe {
                    wasi::wasmtime4j_wasi_free_capture_buffer(data_ptr, len);
                }
                std::ptr::null_mut()
            }
        }
    }

    /// Check if stdout capture is enabled
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativeHasStdoutCapture(
        _env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
    ) -> jint {
        if context_handle == 0 {
            return -1;
        }
        unsafe { wasi::wasmtime4j_wasi_context_has_stdout_capture(context_handle as *const c_void) }
    }

    /// Check if stderr capture is enabled
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativeHasStderrCapture(
        _env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
    ) -> jint {
        if context_handle == 0 {
            return -1;
        }
        unsafe { wasi::wasmtime4j_wasi_context_has_stderr_capture(context_handle as *const c_void) }
    }

    /// Get environment variables from WASI context as a String ("key=value\n" pairs)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativeGetEnvironment(
        mut env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
    ) -> jni::sys::jstring {
        if context_handle == 0 {
            return std::ptr::null_mut();
        }

        let mut out_ptr: *mut u8 = std::ptr::null_mut();
        let mut out_len: usize = 0;
        let result = unsafe {
            wasi::wasmtime4j_wasi_context_get_environment(
                context_handle as *mut c_void,
                &mut out_ptr as *mut *mut u8,
                &mut out_len as *mut usize,
            )
        };

        if result != 0 || out_ptr.is_null() || out_len == 0 {
            if !out_ptr.is_null() {
                unsafe { wasi::wasmtime4j_wasi_free_capture_buffer(out_ptr, out_len); }
            }
            // Return empty string
            match env.new_string("") {
                Ok(s) => s.into_raw(),
                Err(_) => std::ptr::null_mut(),
            }
        } else {
            let data_slice = unsafe { std::slice::from_raw_parts(out_ptr, out_len) };
            let string = String::from_utf8_lossy(data_slice).into_owned();
            unsafe { wasi::wasmtime4j_wasi_free_capture_buffer(out_ptr, out_len); }

            match env.new_string(&string) {
                Ok(s) => s.into_raw(),
                Err(_) => std::ptr::null_mut(),
            }
        }
    }

    /// Get arguments from WASI context as a String ("arg1\narg2\n..." newline-separated)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasiContextImpl_nativeGetArguments(
        mut env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
    ) -> jni::sys::jstring {
        if context_handle == 0 {
            return std::ptr::null_mut();
        }

        let mut out_ptr: *mut u8 = std::ptr::null_mut();
        let mut out_len: usize = 0;
        let result = unsafe {
            wasi::wasmtime4j_wasi_context_get_arguments(
                context_handle as *mut c_void,
                &mut out_ptr as *mut *mut u8,
                &mut out_len as *mut usize,
            )
        };

        if result != 0 || out_ptr.is_null() || out_len == 0 {
            if !out_ptr.is_null() {
                unsafe { wasi::wasmtime4j_wasi_free_capture_buffer(out_ptr, out_len); }
            }
            // Return empty string
            match env.new_string("") {
                Ok(s) => s.into_raw(),
                Err(_) => std::ptr::null_mut(),
            }
        } else {
            let data_slice = unsafe { std::slice::from_raw_parts(out_ptr, out_len) };
            let string = String::from_utf8_lossy(data_slice).into_owned();
            unsafe { wasi::wasmtime4j_wasi_free_capture_buffer(out_ptr, out_len); }

            match env.new_string(&string) {
                Ok(s) => s.into_raw(),
                Err(_) => std::ptr::null_mut(),
            }
        }
    }
}
