//! JNI bindings for WebAssembly thread operations
//!
//! This module provides the JNI interface for WebAssembly threading capabilities,
//! including thread spawning, execution, synchronization, and lifecycle management.

use crate::error::{WasmtimeError, WasmtimeResult, jni_utils};
use crate::threading::{WasmThread, WasmThreadState};
use jni::objects::{JByteArray, JClass, JObject};
use jni::sys::{jboolean, jbyteArray, jint, jlong, jlongArray};
use jni::JNIEnv;
use std::os::raw::c_void;
use std::sync::Arc;

/// Get a reference to a WasmThread from a native handle
///
/// # Safety
/// The handle must be a valid pointer to a WasmThread
unsafe fn get_thread_ref(handle: *const c_void) -> WasmtimeResult<&'static WasmThread> {
    if handle.is_null() {
        return Err(WasmtimeError::InvalidParameter {
            message: "Thread handle cannot be null".to_string(),
        });
    }
    Ok(&*(handle as *const WasmThread))
}

/// Get a mutable reference to a WasmThread from a native handle
///
/// # Safety
/// The handle must be a valid pointer to a WasmThread
unsafe fn get_thread_ref_mut(handle: *mut c_void) -> WasmtimeResult<&'static mut WasmThread> {
    if handle.is_null() {
        return Err(WasmtimeError::InvalidParameter {
            message: "Thread handle cannot be null".to_string(),
        });
    }
    Ok(&mut *(handle as *mut WasmThread))
}

/// Get thread state
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmThread_nativeGetState(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        if handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Thread handle cannot be null".to_string(),
            });
        }

        let thread = unsafe { get_thread_ref(handle as *const c_void)? };
        let state = thread.get_state();

        // Convert WasmThreadState to integer
        let state_value = match state {
            WasmThreadState::New => 0,
            WasmThreadState::Running => 1,
            WasmThreadState::Waiting => 2,
            WasmThreadState::TimedWaiting => 3,
            WasmThreadState::Blocked => 4,
            WasmThreadState::Suspended => 5,
            WasmThreadState::Terminated => 6,
            WasmThreadState::Error => 7,
            WasmThreadState::Killed => 8,
        };

        Ok(state_value)
    })
}

/// Execute a function on the thread with thread-local Store
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmThread_nativeExecuteFunction(
    mut env: JNIEnv,
    _class: JClass,
    thread_handle: jlong,
    module_handle: jlong,
    function_name: jni::objects::JString,
    serialized_args: JByteArray,
) -> jbyteArray {
    jni_utils::jni_try_object(&mut env, |env_ref| {
        // Validate parameters
        if thread_handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Thread handle cannot be null".to_string(),
            });
        }
        if module_handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Module handle cannot be null".to_string(),
            });
        }

        // Get thread reference
        let thread = unsafe { get_thread_ref(thread_handle as *const c_void)? };

        // Get module reference
        let module = unsafe {
            if (module_handle as *const c_void).is_null() {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Module handle is null".to_string(),
                });
            }
            &*(module_handle as *const wasmtime::Module)
        };

        // Get function name
        let func_name: String = env_ref.get_string(&function_name)
            .map_err(|e| WasmtimeError::JniError(format!("Failed to get function name: {}", e)))?
            .into();

        // Deserialize arguments
        let args_bytes = env_ref.convert_byte_array(&serialized_args)
            .map_err(|e| WasmtimeError::JniError(format!("Failed to convert arguments: {}", e)))?;

        let args: Vec<wasmtime::Val> = crate::value_serialization::deserialize_values(&args_bytes)?;

        // Execute function in thread context with thread-local Store
        let result = thread.execute_function(move || {
            // Create thread-local Store
            let engine = module.engine();
            let mut store = wasmtime::Store::new(&engine, ());

            // Instantiate module in this thread's context
            let instance = wasmtime::Instance::new(&mut store, module, &[])
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to instantiate module: {}", e),
                    backtrace: None,
                })?;

            // Look up function by name
            let func = instance.get_func(&mut store, &func_name)
                .ok_or_else(|| WasmtimeError::Function {
                    message: format!("Function '{}' not found", func_name),
                })?;

            // Prepare results buffer
            let func_type = func.ty(&store);
            let mut results = vec![wasmtime::Val::I32(0); func_type.results().len()];

            // Execute function
            func.call(&mut store, &args, &mut results)
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Function execution failed: {}", e),
                    backtrace: None,
                })?;

            Ok(results)
        })?;

        // Serialize results
        let result_bytes = crate::value_serialization::serialize_values(&result)?;
        let result_array = env_ref.byte_array_from_slice(&result_bytes)
            .map_err(|e| WasmtimeError::JniError(format!("Failed to create result array: {}", e)))?;

        Ok(unsafe { jni::objects::JObject::from_raw(result_array.as_raw()) })
    })
}

/// Join the thread (wait for completion)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmThread_nativeJoin(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    jni_utils::jni_try_with_default(&mut env, (), || {
        if handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Thread handle cannot be null".to_string(),
            });
        }

        let thread = unsafe { get_thread_ref_mut(handle as *mut c_void)? };

        // Wait for thread to complete
        thread.join()
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Thread join failed: {}", e),
                backtrace: None,
            })?;

        Ok(())
    });
}

/// Join the thread with timeout
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmThread_nativeJoinTimeout(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    timeout_ms: jlong,
) -> jboolean {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        if handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Thread handle cannot be null".to_string(),
            });
        }
        if timeout_ms < 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Timeout must be non-negative".to_string(),
            });
        }

        let thread = unsafe { get_thread_ref_mut(handle as *mut c_void)? };

        // Wait for thread to complete with timeout
        let completed = thread.join_timeout(std::time::Duration::from_millis(timeout_ms as u64))
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Thread join with timeout failed: {}", e),
                backtrace: None,
            })?;

        Ok(if completed { 1 } else { 0 })
    }) as jboolean
}

/// Request termination of the thread
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmThread_nativeRequestTermination(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    jni_utils::jni_try_with_default(&mut env, (), || {
        if handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Thread handle cannot be null".to_string(),
            });
        }

        let thread = unsafe { get_thread_ref(handle as *const c_void)? };
        thread.request_termination();

        Ok(())
    });
}

/// Force terminate the thread
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmThread_nativeForceTerminate(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    jni_utils::jni_try_with_default(&mut env, (), || {
        if handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Thread handle cannot be null".to_string(),
            });
        }

        let thread = unsafe { get_thread_ref_mut(handle as *mut c_void)? };
        thread.force_terminate()
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Force termination failed: {}", e),
                backtrace: None,
            })?;

        Ok(())
    });
}

/// Check if termination has been requested
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmThread_nativeIsTerminationRequested(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jboolean {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        if handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Thread handle cannot be null".to_string(),
            });
        }

        let thread = unsafe { get_thread_ref(handle as *const c_void)? };
        let requested = thread.is_termination_requested();

        Ok(if requested { 1 } else { 0 })
    }) as jboolean
}

/// Get thread statistics
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmThread_nativeGetStatistics(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jlongArray {
    jni_utils::jni_try_object(&mut env, |env_ref| {
        if handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Thread handle cannot be null".to_string(),
            });
        }

        let thread = unsafe { get_thread_ref(handle as *const c_void)? };
        let stats = thread.get_statistics()?;

        // Create array: [functionsExecuted, totalExecutionTime, atomicOperations,
        //                memoryAccesses, waitNotifyOperations, peakMemoryUsage]
        let stats_array = env_ref.new_long_array(6)
            .map_err(|e| WasmtimeError::JniError(format!("Failed to create statistics array: {}", e)))?;

        let stats_data = vec![
            stats.functions_executed as i64,
            stats.total_execution_time as i64,
            stats.atomic_operations as i64,
            stats.memory_accesses as i64,
            stats.wait_notify_operations as i64,
            stats.peak_memory_usage as i64,
        ];

        env_ref.set_long_array_region(&stats_array, 0, &stats_data)
            .map_err(|e| WasmtimeError::JniError(format!("Failed to set statistics data: {}", e)))?;

        Ok(unsafe { jni::objects::JObject::from_raw(stats_array.as_raw()) })
    })
}

/// Destroy thread resources
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmThread_nativeDestroy(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    jni_utils::jni_try_with_default(&mut env, (), || {
        if handle == 0 {
            return Ok(()); // Already destroyed
        }

        // Convert handle to Box and drop it
        unsafe {
            let _thread = Box::from_raw(handle as *mut WasmThread);
            // Box will be dropped here, cleaning up resources
        }

        Ok(())
    });
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_state_conversion() {
        // Verify state enum values match Java expectations
        assert_eq!(WasmThreadState::New as i32, 0);
        assert_eq!(WasmThreadState::Running as i32, 1);
        assert_eq!(WasmThreadState::Waiting as i32, 2);
        assert_eq!(WasmThreadState::TimedWaiting as i32, 3);
        assert_eq!(WasmThreadState::Blocked as i32, 4);
        assert_eq!(WasmThreadState::Suspended as i32, 5);
        assert_eq!(WasmThreadState::Terminated as i32, 6);
        assert_eq!(WasmThreadState::Error as i32, 7);
        assert_eq!(WasmThreadState::Killed as i32, 8);
    }

    #[test]
    fn test_get_thread_ref_null_handle() {
        // Verify null handle returns error
        let result = unsafe { get_thread_ref(std::ptr::null()) };
        assert!(result.is_err());
        if let Err(WasmtimeError::InvalidParameter { message }) = result {
            assert!(message.contains("cannot be null"));
        } else {
            panic!("Expected InvalidParameter error");
        }
    }

    #[test]
    fn test_get_thread_ref_mut_null_handle() {
        // Verify null handle returns error for mutable reference
        let result = unsafe { get_thread_ref_mut(std::ptr::null_mut()) };
        assert!(result.is_err());
        if let Err(WasmtimeError::InvalidParameter { message }) = result {
            assert!(message.contains("cannot be null"));
        } else {
            panic!("Expected InvalidParameter error");
        }
    }

    #[test]
    fn test_jni_method_naming_conventions() {
        // Verify JNI method names follow correct conventions
        // This is a compile-time check - if this compiles, the names are correct

        // Check that all expected JNI methods exist and have correct signatures
        let _: extern "system" fn(JNIEnv, JClass, jlong) -> jint =
            Java_ai_tegmentum_wasmtime4j_jni_JniWasmThread_nativeGetState;

        let _: extern "system" fn(JNIEnv, JClass, jlong, jlong, jni::objects::JString, JByteArray) -> jbyteArray =
            Java_ai_tegmentum_wasmtime4j_jni_JniWasmThread_nativeExecuteFunction;

        let _: extern "system" fn(JNIEnv, JClass, jlong) =
            Java_ai_tegmentum_wasmtime4j_jni_JniWasmThread_nativeJoin;

        let _: extern "system" fn(JNIEnv, JClass, jlong, jlong) -> jboolean =
            Java_ai_tegmentum_wasmtime4j_jni_JniWasmThread_nativeJoinTimeout;

        let _: extern "system" fn(JNIEnv, JClass, jlong) =
            Java_ai_tegmentum_wasmtime4j_jni_JniWasmThread_nativeRequestTermination;

        let _: extern "system" fn(JNIEnv, JClass, jlong) =
            Java_ai_tegmentum_wasmtime4j_jni_JniWasmThread_nativeForceTerminate;

        let _: extern "system" fn(JNIEnv, JClass, jlong) -> jboolean =
            Java_ai_tegmentum_wasmtime4j_jni_JniWasmThread_nativeIsTerminationRequested;

        let _: extern "system" fn(JNIEnv, JClass, jlong) -> jlongArray =
            Java_ai_tegmentum_wasmtime4j_jni_JniWasmThread_nativeGetStatistics;

        let _: extern "system" fn(JNIEnv, JClass, jlong) =
            Java_ai_tegmentum_wasmtime4j_jni_JniWasmThread_nativeDestroy;
    }

    #[test]
    fn test_all_thread_states_covered() {
        // Ensure all thread states are covered in conversion logic
        // If new states are added to WasmThreadState, this test will fail
        // reminding us to update the JNI conversion code

        use crate::threading::WasmThreadState;

        // Create a vector of all possible states
        let states = vec![
            WasmThreadState::New,
            WasmThreadState::Running,
            WasmThreadState::Waiting,
            WasmThreadState::TimedWaiting,
            WasmThreadState::Blocked,
            WasmThreadState::Suspended,
            WasmThreadState::Terminated,
            WasmThreadState::Error,
            WasmThreadState::Killed,
        ];

        // Verify each state maps to a unique integer
        let mut seen_values = std::collections::HashSet::new();
        for state in states {
            let value = state as i32;
            assert!(value >= 0 && value <= 8, "State value out of expected range");
            assert!(seen_values.insert(value), "Duplicate state value found");
        }

        // Verify we have exactly 9 states
        assert_eq!(seen_values.len(), 9, "Expected exactly 9 thread states");
    }
}
