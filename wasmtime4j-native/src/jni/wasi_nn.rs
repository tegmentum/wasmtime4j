//! JNI bindings for WASI-NN host-side inference.
//!
//! Provides JNI native method implementations for NnContext, NnGraph, and
//! NnGraphExecutionContext Java classes.

// Feature detection imports (always available when jni-bindings is enabled)
#[cfg(feature = "jni-bindings")]
use jni::objects::JClass;
#[cfg(feature = "jni-bindings")]
use jni::sys::{jboolean, JNI_FALSE, JNI_TRUE};
#[cfg(feature = "jni-bindings")]
use jni::JNIEnv;

// Full WASI-NN imports (only when both features are enabled)
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
use jni::objects::{JByteArray, JIntArray, JObjectArray, JString};
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
use jni::sys::{jbyteArray, jint, jintArray, jlong, jstring};

#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
use crate::wasi_nn::{NnContext, NnExecCtx, NnGraph};

// ============================================================================
// Feature detection (always compiled when jni-bindings is enabled)
// ============================================================================

/// Returns whether the wasi-nn feature is compiled in.
#[cfg(feature = "jni-bindings")]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnContextFactory_nativeIsFeatureEnabled(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    #[cfg(feature = "wasi-nn")]
    {
        JNI_TRUE
    }
    #[cfg(not(feature = "wasi-nn"))]
    {
        JNI_FALSE
    }
}

// ============================================================================
// NnContext JNI bindings
// ============================================================================

/// Creates a new NnContext and returns its handle.
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnContext_nativeCreate(
    _env: JNIEnv,
    _class: JClass,
) -> jlong {
    let ctx = Box::new(NnContext::new());
    Box::into_raw(ctx) as jlong
}

/// Returns whether any backends are available.
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnContext_nativeIsAvailable(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jboolean {
    let ctx = unsafe { &*(handle as *const NnContext) };
    if ctx.is_available() {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

/// Returns the supported encoding codes as an int array.
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnContext_nativeSupportedEncodings(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jintArray {
    let ctx = unsafe { &*(handle as *const NnContext) };
    let codes = ctx.supported_encoding_codes();

    match env.new_int_array(codes.len() as i32) {
        Ok(arr) => {
            if let Err(e) = env.set_int_array_region(&arr, 0, &codes) {
                log::error!("Failed to set int array region: {}", e);
            }
            arr.into_raw()
        }
        Err(e) => {
            log::error!("Failed to create int array: {}", e);
            std::ptr::null_mut()
        }
    }
}

/// Loads a graph from multiple byte array parts.
/// Returns the graph handle, or throws NnException on error.
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnContext_nativeLoadGraph(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    parts: JObjectArray,
    encoding_code: jint,
    target_code: jint,
) -> jlong {
    let ctx = unsafe { &mut *(handle as *mut NnContext) };

    // Parse encoding and target
    let encoding = match crate::wasi_nn::graph_encoding_from_java(encoding_code) {
        Ok(e) => e,
        Err(msg) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/wasi/nn/NnException", &msg);
            return 0;
        }
    };
    let target = match crate::wasi_nn::execution_target_from_java(target_code) {
        Ok(t) => t,
        Err(msg) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/wasi/nn/NnException", &msg);
            return 0;
        }
    };

    // Extract byte arrays from JObjectArray
    let num_parts = match env.get_array_length(&parts) {
        Ok(n) => n as usize,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/wasi/nn/NnException",
                &format!("Failed to get parts array length: {}", e),
            );
            return 0;
        }
    };

    let mut part_vecs: Vec<Vec<u8>> = Vec::with_capacity(num_parts);
    for i in 0..num_parts {
        let part_obj = match env.get_object_array_element(&parts, i as i32) {
            Ok(obj) => obj,
            Err(e) => {
                let _ = env.throw_new(
                    "ai/tegmentum/wasmtime4j/wasi/nn/NnException",
                    &format!("Failed to get part {}: {}", i, e),
                );
                return 0;
            }
        };
        let byte_array: JByteArray = part_obj.into();
        match env.convert_byte_array(byte_array) {
            Ok(bytes) => part_vecs.push(bytes),
            Err(e) => {
                let _ = env.throw_new(
                    "ai/tegmentum/wasmtime4j/wasi/nn/NnException",
                    &format!("Failed to convert part {} bytes: {}", i, e),
                );
                return 0;
            }
        }
    }

    // Build slice-of-slices for load
    let part_slices: Vec<&[u8]> = part_vecs.iter().map(|v| v.as_slice()).collect();

    match ctx.load_graph(&part_slices, encoding, target) {
        Ok(graph) => {
            let boxed = Box::new(graph);
            Box::into_raw(boxed) as jlong
        }
        Err(msg) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/wasi/nn/NnException", &msg);
            0
        }
    }
}

/// Gets backend info as a JSON string: {"version":"...","backends":["..."],"default":"..."}
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnContext_nativeGetBackendInfo(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jstring {
    let ctx = unsafe { &*(handle as *const NnContext) };
    let names = ctx.backend_names();
    let default = ctx.default_backend_name().unwrap_or_default();
    let json = format!(
        "{{\"version\":\"{}\",\"backends\":[{}],\"default\":\"{}\"}}",
        crate::WASMTIME_VERSION,
        names
            .iter()
            .map(|n| format!("\"{}\"", n))
            .collect::<Vec<_>>()
            .join(","),
        default
    );
    match env.new_string(&json) {
        Ok(s) => s.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

/// Closes and frees an NnContext.
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnContext_nativeClose(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle != 0 {
        let _ = unsafe { Box::from_raw(handle as *mut NnContext) };
        log::debug!("NnContext closed");
    }
}

// ============================================================================
// NnGraph JNI bindings
// ============================================================================

/// Gets the graph encoding code.
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnGraph_nativeGetEncoding(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jint {
    let graph = unsafe { &*(handle as *const NnGraph) };
    graph.encoding_code()
}

/// Gets the execution target code.
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnGraph_nativeGetTarget(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jint {
    let graph = unsafe { &*(handle as *const NnGraph) };
    graph.target_code()
}

/// Creates an execution context from the graph.
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnGraph_nativeCreateExecCtx(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jlong {
    let graph = unsafe { &*(handle as *const NnGraph) };
    match graph.create_execution_context() {
        Ok(exec) => {
            let boxed = Box::new(exec);
            Box::into_raw(boxed) as jlong
        }
        Err(msg) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/wasi/nn/NnException", &msg);
            0
        }
    }
}

/// Closes and frees an NnGraph.
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnGraph_nativeClose(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle != 0 {
        let _ = unsafe { Box::from_raw(handle as *mut NnGraph) };
        log::debug!("NnGraph closed");
    }
}

// ============================================================================
// NnGraphExecutionContext JNI bindings
// ============================================================================

/// Sets an input tensor by index.
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnGraphExecutionContext_nativeSetInputByIndex(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    index: jint,
    dims: JIntArray,
    tensor_type: jint,
    data: JByteArray,
) {
    let exec = unsafe { &mut *(handle as *mut NnExecCtx) };

    // Convert dims
    let dims_len = match env.get_array_length(&dims) {
        Ok(n) => n as usize,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/wasi/nn/NnException",
                &format!("Failed to get dims length: {}", e),
            );
            return;
        }
    };
    let mut dims_buf = vec![0i32; dims_len];
    if let Err(e) = env.get_int_array_region(&dims, 0, &mut dims_buf) {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/wasi/nn/NnException",
            &format!("Failed to read dims: {}", e),
        );
        return;
    }
    let dims_u32: Vec<u32> = dims_buf.iter().map(|&d| d as u32).collect();

    // Convert tensor type
    let ty = match crate::wasi_nn::tensor_type_from_java(tensor_type) {
        Ok(t) => t,
        Err(msg) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/wasi/nn/NnException", &msg);
            return;
        }
    };

    // Convert data
    let data_bytes = match env.convert_byte_array(data) {
        Ok(b) => b,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/wasi/nn/NnException",
                &format!("Failed to convert data: {}", e),
            );
            return;
        }
    };

    if let Err(msg) = exec.set_input_by_index(index as u32, &dims_u32, ty, &data_bytes) {
        let _ = env.throw_new("ai/tegmentum/wasmtime4j/wasi/nn/NnException", &msg);
    }
}

/// Sets an input tensor by name.
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnGraphExecutionContext_nativeSetInputByName(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    name: JString,
    dims: JIntArray,
    tensor_type: jint,
    data: JByteArray,
) {
    let exec = unsafe { &mut *(handle as *mut NnExecCtx) };

    // Convert name
    let name_str: String = match env.get_string(&name) {
        Ok(s) => s.into(),
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/wasi/nn/NnException",
                &format!("Failed to get name string: {}", e),
            );
            return;
        }
    };

    // Convert dims
    let dims_len = match env.get_array_length(&dims) {
        Ok(n) => n as usize,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/wasi/nn/NnException",
                &format!("Failed to get dims length: {}", e),
            );
            return;
        }
    };
    let mut dims_buf = vec![0i32; dims_len];
    if let Err(e) = env.get_int_array_region(&dims, 0, &mut dims_buf) {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/wasi/nn/NnException",
            &format!("Failed to read dims: {}", e),
        );
        return;
    }
    let dims_u32: Vec<u32> = dims_buf.iter().map(|&d| d as u32).collect();

    // Convert tensor type
    let ty = match crate::wasi_nn::tensor_type_from_java(tensor_type) {
        Ok(t) => t,
        Err(msg) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/wasi/nn/NnException", &msg);
            return;
        }
    };

    // Convert data
    let data_bytes = match env.convert_byte_array(data) {
        Ok(b) => b,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/wasi/nn/NnException",
                &format!("Failed to convert data: {}", e),
            );
            return;
        }
    };

    if let Err(msg) = exec.set_input_by_name(&name_str, &dims_u32, ty, &data_bytes) {
        let _ = env.throw_new("ai/tegmentum/wasmtime4j/wasi/nn/NnException", &msg);
    }
}

/// Runs inference.
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnGraphExecutionContext_nativeCompute(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    let exec = unsafe { &mut *(handle as *mut NnExecCtx) };
    if let Err(msg) = exec.compute() {
        let _ = env.throw_new("ai/tegmentum/wasmtime4j/wasi/nn/NnException", &msg);
    }
}

/// Gets an output tensor by index as serialized bytes.
/// Format: [num_dims: i32][dim0: i32]...[dimN: i32][tensor_type: i32][data bytes]
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnGraphExecutionContext_nativeGetOutputByIndex(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    index: jint,
) -> jbyteArray {
    let exec = unsafe { &mut *(handle as *mut NnExecCtx) };
    match exec.get_output_by_index(index as u32) {
        Ok(serialized) => match env.byte_array_from_slice(&serialized) {
            Ok(arr) => arr.into_raw(),
            Err(e) => {
                let _ = env.throw_new(
                    "ai/tegmentum/wasmtime4j/wasi/nn/NnException",
                    &format!("Failed to create byte array: {}", e),
                );
                std::ptr::null_mut()
            }
        },
        Err(msg) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/wasi/nn/NnException", &msg);
            std::ptr::null_mut()
        }
    }
}

/// Gets an output tensor by name as serialized bytes.
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnGraphExecutionContext_nativeGetOutputByName(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    name: JString,
) -> jbyteArray {
    let exec = unsafe { &mut *(handle as *mut NnExecCtx) };

    let name_str: String = match env.get_string(&name) {
        Ok(s) => s.into(),
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/wasi/nn/NnException",
                &format!("Failed to get name string: {}", e),
            );
            return std::ptr::null_mut();
        }
    };

    match exec.get_output_by_name(&name_str) {
        Ok(serialized) => match env.byte_array_from_slice(&serialized) {
            Ok(arr) => arr.into_raw(),
            Err(e) => {
                let _ = env.throw_new(
                    "ai/tegmentum/wasmtime4j/wasi/nn/NnException",
                    &format!("Failed to create byte array: {}", e),
                );
                std::ptr::null_mut()
            }
        },
        Err(msg) => {
            let _ = env.throw_new("ai/tegmentum/wasmtime4j/wasi/nn/NnException", &msg);
            std::ptr::null_mut()
        }
    }
}

/// Closes and frees an NnExecCtx.
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnGraphExecutionContext_nativeClose(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle != 0 {
        let _ = unsafe { Box::from_raw(handle as *mut NnExecCtx) };
        log::debug!("NnExecCtx closed");
    }
}
