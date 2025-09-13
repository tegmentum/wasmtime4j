//! JNI bindings for WASI operations
//! 
//! This module provides JNI-compatible functions for WASI context management.

#[cfg(feature = "jni-bindings")]
use jni::JNIEnv;
#[cfg(feature = "jni-bindings")]
use jni::objects::{JClass, JObjectArray, JString};
#[cfg(feature = "jni-bindings")]
use jni::sys::{jlong, jint, jboolean, jobjectArray};

/// JNI bindings for WASI operations
#[cfg(feature = "jni-bindings")]
pub mod jni_wasi {
    use super::*;
    use crate::wasi;
    use crate::error::jni_utils;
    use jni::objects::{JObjectArray, JString};
    use jni::sys::{jobjectArray, jlong, jint, jboolean};
    use std::ffi::CString;
    use std::os::raw::c_void;

    /// Create a new WASI context with specified configuration (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiContext_nativeCreate(
        env: JNIEnv,
        _class: JClass,
        environment: jobjectArray,
        arguments: jobjectArray,
        preopen_dirs: jobjectArray,
        working_dir: JString,
    ) -> jlong {
        match (|| -> crate::error::WasmtimeResult<*mut c_void> {
            // Convert working directory string
            let working_dir_str = if working_dir.is_null() {
                None
            } else {
                let wd_string: String = env.get_string(working_dir)?.into();
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
                let env_array = JObjectArray::from(environment);
                let env_len = env.get_array_length(env_array)?;
                
                for i in 0..env_len {
                    let env_entry = env.get_object_array_element(env_array, i)?;
                    if !env_entry.is_null() {
                        let env_str: String = env.get_string(JString::from(env_entry))?.into();
                        
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
                                unsafe { wasi::wasi_ctx_destroy(ctx_ptr); }
                                return Err(crate::error::WasmtimeError::Wasi {
                                    message: format!("Failed to set environment variable {}={}", key, value),
                                });
                            }
                        }
                    }
                }
            }

            // Add command line arguments if provided
            if !arguments.is_null() {
                let args_array = JObjectArray::from(arguments);
                let args_len = env.get_array_length(args_array)?;
                
                let mut arg_cstrs = Vec::new();
                let mut arg_ptrs = Vec::new();
                
                for i in 0..args_len {
                    let arg_entry = env.get_object_array_element(args_array, i)?;
                    if !arg_entry.is_null() {
                        let arg_str: String = env.get_string(JString::from(arg_entry))?.into();
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
                        unsafe { wasi::wasi_ctx_destroy(ctx_ptr); }
                        return Err(crate::error::WasmtimeError::Wasi {
                            message: "Failed to set command line arguments".to_string(),
                        });
                    }
                }
            }

            // Add pre-opened directories if provided
            if !preopen_dirs.is_null() {
                let dirs_array = JObjectArray::from(preopen_dirs);
                let dirs_len = env.get_array_length(dirs_array)?;
                
                for i in 0..dirs_len {
                    let dir_entry = env.get_object_array_element(dirs_array, i)?;
                    if !dir_entry.is_null() {
                        let dir_str: String = env.get_string(JString::from(dir_entry))?.into();
                        
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
                                    0, 1, 0, // dir perms: no create, read, no remove
                                    1, 0, 0, 0, // file perms: read only
                                )
                            };
                            
                            if result != 0 {
                                unsafe { wasi::wasi_ctx_destroy(ctx_ptr); }
                                return Err(crate::error::WasmtimeError::Wasi {
                                    message: format!("Failed to add directory mapping {}:{}", host_path, guest_path),
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
                jni_utils::throw_wasmtime_exception(&env, &error);
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
        env: JNIEnv,
        _class: JClass,
        handle: jlong,
        host_path: JString,
        guest_path: JString,
        can_read: jboolean,
        can_write: jboolean,
        can_create: jboolean,
    ) -> jboolean {
        jni_utils::jni_try_default(&env, 0, || {
            if handle == 0 {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "WASI context handle cannot be null".to_string(),
                });
            }

            let host_str: String = env.get_string(host_path)?.into();
            let guest_str: String = env.get_string(guest_path)?.into();
            
            let host_cstr = CString::new(host_str).map_err(|_| {
                crate::error::WasmtimeError::Wasi {
                    message: "Invalid host path string".to_string(),
                }
            })?;
            let guest_cstr = CString::new(guest_str).map_err(|_| {
                crate::error::WasmtimeError::Wasi {
                    message: "Invalid guest path string".to_string(),
                }
            })?;

            let result = unsafe {
                wasi::wasi_ctx_add_dir(
                    handle as *mut c_void,
                    host_cstr.as_ptr(),
                    guest_cstr.as_ptr(),
                    can_create as i32, can_read as i32, 0, // dir perms
                    can_read as i32, can_write as i32, can_create as i32, 0, // file perms
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
        env: JNIEnv,
        _class: JClass,
        handle: jlong,
        key: JString,
        value: JString,
    ) -> jboolean {
        jni_utils::jni_try_default(&env, 0, || {
            if handle == 0 {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "WASI context handle cannot be null".to_string(),
                });
            }

            let key_str: String = env.get_string(key)?.into();
            let value_str: String = env.get_string(value)?.into();
            
            let key_cstr = CString::new(key_str).map_err(|_| {
                crate::error::WasmtimeError::Wasi {
                    message: "Invalid environment key string".to_string(),
                }
            })?;
            let value_cstr = CString::new(value_str).map_err(|_| {
                crate::error::WasmtimeError::Wasi {
                    message: "Invalid environment value string".to_string(),
                }
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
        env: JNIEnv,
        _class: JClass,
        handle: jlong,
        path: JString,
    ) -> jboolean {
        jni_utils::jni_try_default(&env, 0, || {
            if handle == 0 {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "WASI context handle cannot be null".to_string(),
                });
            }

            let path_str: String = env.get_string(path)?.into();
            let path_cstr = CString::new(path_str).map_err(|_| {
                crate::error::WasmtimeError::Wasi {
                    message: "Invalid path string".to_string(),
                }
            })?;

            let result = unsafe {
                wasi::wasi_ctx_is_path_allowed(handle as *const c_void, path_cstr.as_ptr())
            };

            Ok(result)
        })
    }

    /// Get the number of environment variables in the WASI context (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiContext_nativeGetEnvironmentCount(
        env: JNIEnv,
        _class: JClass,
        handle: jlong,
    ) -> jint {
        jni_utils::jni_try_default(&env, 0, || {
            if handle == 0 {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "WASI context handle cannot be null".to_string(),
                });
            }

            let count = unsafe {
                wasi::wasi_ctx_get_env_count(handle as *const c_void)
            };

            Ok(count as jint)
        })
    }

    /// Get the number of command line arguments in the WASI context (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiContext_nativeGetArgumentCount(
        env: JNIEnv,
        _class: JClass,
        handle: jlong,
    ) -> jint {
        jni_utils::jni_try_default(&env, 0, || {
            if handle == 0 {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "WASI context handle cannot be null".to_string(),
                });
            }

            let count = unsafe {
                wasi::wasi_ctx_get_args_count(handle as *const c_void)
            };

            Ok(count as jint)
        })
    }

    /// Get the number of directory mappings in the WASI context (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiContext_nativeGetDirectoryCount(
        env: JNIEnv,
        _class: JClass,
        handle: jlong,
    ) -> jint {
        jni_utils::jni_try_default(&env, 0, || {
            if handle == 0 {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "WASI context handle cannot be null".to_string(),
                });
            }

            let count = unsafe {
                wasi::wasi_ctx_get_dir_count(handle as *const c_void)
            };

            Ok(count as jint)
        })
    }

    /// Add a WASI context to a Store for WebAssembly instance creation (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiContext_nativeAddToStore(
        env: JNIEnv,
        _class: JClass,
        wasi_handle: jlong,
        store_handle: jlong,
    ) -> jboolean {
        jni_utils::jni_try_default(&env, 0, || {
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
        env: JNIEnv,
        _class: JClass,
        store_handle: jlong,
    ) -> jlong {
        jni_utils::jni_try_default(&env, 0, || {
            if store_handle == 0 {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }

            let wasi_ptr = unsafe {
                wasi::wasi_ctx_get_from_store(store_handle as *const c_void)
            };

            Ok(wasi_ptr as jlong)
        })
    }

    /// Check if a Store has a WASI context attached (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiContext_nativeStoreHasWasi(
        env: JNIEnv,
        _class: JClass,
        store_handle: jlong,
    ) -> jboolean {
        jni_utils::jni_try_default(&env, 0, || {
            if store_handle == 0 {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }

            let result = unsafe {
                wasi::wasi_ctx_store_has_wasi(store_handle as *const c_void)
            };

            Ok(result)
        })
    }
}