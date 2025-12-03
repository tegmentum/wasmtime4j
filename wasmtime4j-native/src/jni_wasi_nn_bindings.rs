//! JNI bindings for WASI-NN (WebAssembly System Interface for Neural Networks)
//!
//! This module provides JNI bindings for the WASI-NN machine learning extension,
//! enabling Java applications to use neural network inference through Wasmtime.

#![allow(unused_variables)]
#![allow(clippy::too_many_arguments)]

#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
use jni::JNIEnv;
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
use jni::objects::{JByteArray, JClass, JIntArray, JObject, JObjectArray, JString};
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
use jni::sys::{jbyteArray, jint, jintArray, jlong, jobjectArray, jstring};
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
use std::sync::Arc;

#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
use crate::error::jni_utils;
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
use crate::wasi_nn::{
    NnGraphEncoding, NnExecutionTarget, NnTensorType, NnTensor,
    WasiNnContext, WasiNnConfig, NnGraph, NnExecutionContext
};

// ===== JniNnContextFactory native methods =====

/// Create a new WASI-NN context
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnContextFactory_nativeCreateContext(
    mut env: JNIEnv,
    _class: JClass,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let context = WasiNnContext::with_defaults()?;
        Ok(Box::into_raw(Box::new(context)) as jlong)
    })
}

/// Check if WASI-NN is available
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnContextFactory_nativeIsNnAvailable(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    // WASI-NN is available when compiled with the wasi-nn feature
    1
}

/// Get default execution target
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnContextFactory_nativeGetDefaultExecutionTarget(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    NnExecutionTarget::Cpu as jint
}

// ===== JniNnContext native methods =====

/// Load a graph from model data
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnContext_nativeLoadGraph(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    model_data: JByteArray,
    encoding_ordinal: jint,
    target_ordinal: jint,
) -> jlong {
    // Extract JNI data before the closure
    let data = match env.convert_byte_array(&model_data) {
        Ok(d) => d,
        Err(_) => return 0,
    };

    jni_utils::jni_try_with_default(&mut env, 0, || {
        let context = unsafe { &*(context_handle as *const WasiNnContext) };

        let encoding = NnGraphEncoding::from_native_code(encoding_ordinal)
            .unwrap_or(NnGraphEncoding::Autodetect);
        let target = NnExecutionTarget::from_native_code(target_ordinal)
            .unwrap_or(NnExecutionTarget::Cpu);

        let graph = context.load_graph(&data, encoding, target)?;
        Ok(Arc::into_raw(graph) as jlong)
    })
}

/// Load a graph from multiple model data parts
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnContext_nativeLoadGraphMultiPart(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    model_parts: JObjectArray,
    encoding_ordinal: jint,
    target_ordinal: jint,
) -> jlong {
    // Extract JNI data before the closure
    let parts_len = match env.get_array_length(&model_parts) {
        Ok(len) => len as usize,
        Err(_) => return 0,
    };

    // Concatenate all parts
    let mut combined_data = Vec::new();
    for i in 0..parts_len {
        let part = match env.get_object_array_element(&model_parts, i as i32) {
            Ok(p) => p,
            Err(_) => return 0,
        };
        let part_array = JByteArray::from(part);
        let part_data = match env.convert_byte_array(&part_array) {
            Ok(d) => d,
            Err(_) => return 0,
        };
        combined_data.extend_from_slice(&part_data);
    }

    jni_utils::jni_try_with_default(&mut env, 0, || {
        let context = unsafe { &*(context_handle as *const WasiNnContext) };

        let encoding = NnGraphEncoding::from_native_code(encoding_ordinal)
            .unwrap_or(NnGraphEncoding::Autodetect);
        let target = NnExecutionTarget::from_native_code(target_ordinal)
            .unwrap_or(NnExecutionTarget::Cpu);

        let graph = context.load_graph(&combined_data, encoding, target)?;
        Ok(Arc::into_raw(graph) as jlong)
    })
}

/// Load a graph by name
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnContext_nativeLoadGraphByName(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    name: JString,
    target_ordinal: jint,
) -> jlong {
    // Extract JNI data before the closure
    let name_str: String = match env.get_string(&name) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };

    jni_utils::jni_try_with_default(&mut env, 0, || {
        let context = unsafe { &*(context_handle as *const WasiNnContext) };

        let target = NnExecutionTarget::from_native_code(target_ordinal)
            .unwrap_or(NnExecutionTarget::Cpu);

        // Try to load from named models in config
        let data = context.config().get_named_model(&name_str)
            .ok_or_else(|| crate::error::WasmtimeError::Runtime {
                message: format!("Named model not found: {}", name_str),
                backtrace: None,
            })?
            .clone();

        let graph = context.load_graph(&data, NnGraphEncoding::Autodetect, target)?;
        Ok(Arc::into_raw(graph) as jlong)
    })
}

/// Get supported encodings
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnContext_nativeGetSupportedEncodings(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
) -> jintArray {
    // Get native data first
    let encoding_values: Vec<i32> = {
        let context = unsafe { &*(context_handle as *const WasiNnContext) };
        let encodings = context.supported_encodings();
        encodings.iter().map(|e| *e as i32).collect()
    };

    // Create JNI array with the data
    let result = match env.new_int_array(encoding_values.len() as i32) {
        Ok(arr) => arr,
        Err(_) => return std::ptr::null_mut(),
    };

    if env.set_int_array_region(&result, 0, &encoding_values).is_err() {
        return std::ptr::null_mut();
    }

    result.into_raw()
}

/// Get supported targets
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnContext_nativeGetSupportedTargets(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
) -> jintArray {
    // Get native data first
    let target_values: Vec<i32> = {
        let context = unsafe { &*(context_handle as *const WasiNnContext) };
        let targets = context.supported_targets();
        targets.iter().map(|t| *t as i32).collect()
    };

    // Create JNI array with the data
    let result = match env.new_int_array(target_values.len() as i32) {
        Ok(arr) => arr,
        Err(_) => return std::ptr::null_mut(),
    };

    if env.set_int_array_region(&result, 0, &target_values).is_err() {
        return std::ptr::null_mut();
    }

    result.into_raw()
}

/// Check if encoding is supported
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnContext_nativeIsEncodingSupported(
    _env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    encoding_ordinal: jint,
) -> jint {
    let context = unsafe { &*(context_handle as *const WasiNnContext) };

    let encoding = match NnGraphEncoding::from_native_code(encoding_ordinal) {
        Some(e) => e,
        None => return 0,
    };

    if context.is_encoding_supported(encoding) { 1 } else { 0 }
}

/// Check if target is supported
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnContext_nativeIsTargetSupported(
    _env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    target_ordinal: jint,
) -> jint {
    let context = unsafe { &*(context_handle as *const WasiNnContext) };

    let target = match NnExecutionTarget::from_native_code(target_ordinal) {
        Some(t) => t,
        None => return 0,
    };

    if context.is_target_supported(target) { 1 } else { 0 }
}

/// Close the context
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnContext_nativeClose(
    _env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
) {
    if context_handle != 0 {
        unsafe {
            let _ = Box::from_raw(context_handle as *mut WasiNnContext);
        }
    }
}

// ===== JniNnGraph native methods =====

/// Create an execution context
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnGraph_nativeCreateExecutionContext(
    mut env: JNIEnv,
    _class: JClass,
    graph_handle: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let graph = unsafe { &*(graph_handle as *const NnGraph) };

        let exec_ctx = graph.create_execution_context()?;
        Ok(Box::into_raw(Box::new(exec_ctx)) as jlong)
    })
}

/// Get model name
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnGraph_nativeGetModelName(
    mut env: JNIEnv,
    _class: JClass,
    graph_handle: jlong,
) -> jstring {
    // Get native data first
    let name: String = {
        let graph = unsafe { &*(graph_handle as *const NnGraph) };
        graph.name().unwrap_or("").to_string()
    };

    // Create JNI string
    match env.new_string(&name) {
        Ok(s) => s.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

/// Close the graph
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnGraph_nativeClose(
    _env: JNIEnv,
    _class: JClass,
    graph_handle: jlong,
) {
    if graph_handle != 0 {
        unsafe {
            let graph = Arc::from_raw(graph_handle as *const NnGraph);
            graph.close();
        }
    }
}

// ===== JniNnGraphExecutionContext native methods =====

/// Set input by index
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnGraphExecutionContext_nativeSetInputByIndex(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    index: jint,
    dimensions: JIntArray,
    type_ordinal: jint,
    data: JByteArray,
) -> jint {
    // Extract JNI data before the closure
    let dims_len = match env.get_array_length(&dimensions) {
        Ok(len) => len as usize,
        Err(_) => return -1,
    };

    let mut dims_vec = vec![0i32; dims_len];
    if env.get_int_array_region(&dimensions, 0, &mut dims_vec).is_err() {
        return -1;
    }

    let dims_u32: Vec<u32> = dims_vec.iter().map(|&d| d as u32).collect();

    let data_bytes = match env.convert_byte_array(&data) {
        Ok(d) => d,
        Err(_) => return -1,
    };

    jni_utils::jni_try_with_default(&mut env, -1, || {
        let exec_ctx = unsafe { &*(context_handle as *const NnExecutionContext) };

        // Get tensor type
        let tensor_type = NnTensorType::from_native_code(type_ordinal)
            .unwrap_or(NnTensorType::Fp32);

        // Create tensor and set input
        let tensor = NnTensor::new(dims_u32, tensor_type, data_bytes);
        exec_ctx.set_input(index as u32, tensor)?;

        Ok(0)
    })
}

/// Set input by name
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnGraphExecutionContext_nativeSetInputByName(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    name: JString,
    dimensions: JIntArray,
    type_ordinal: jint,
    data: JByteArray,
) -> jint {
    // Extract JNI data before the closure
    let name_str: String = match env.get_string(&name) {
        Ok(s) => s.into(),
        Err(_) => return -1,
    };

    let dims_len = match env.get_array_length(&dimensions) {
        Ok(len) => len as usize,
        Err(_) => return -1,
    };

    let mut dims_vec = vec![0i32; dims_len];
    if env.get_int_array_region(&dimensions, 0, &mut dims_vec).is_err() {
        return -1;
    }

    let dims_u32: Vec<u32> = dims_vec.iter().map(|&d| d as u32).collect();

    let data_bytes = match env.convert_byte_array(&data) {
        Ok(d) => d,
        Err(_) => return -1,
    };

    jni_utils::jni_try_with_default(&mut env, -1, || {
        let exec_ctx = unsafe { &*(context_handle as *const NnExecutionContext) };

        // Get tensor type
        let tensor_type = NnTensorType::from_native_code(type_ordinal)
            .unwrap_or(NnTensorType::Fp32);

        // Create tensor with name and set input
        // Use index 0 for named inputs (the Rust API uses indices)
        let tensor = NnTensor::new(dims_u32, tensor_type, data_bytes).with_name(name_str);
        exec_ctx.set_input(0, tensor)?;

        Ok(0)
    })
}

/// Compute inference
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnGraphExecutionContext_nativeCompute(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, -1, || {
        let exec_ctx = unsafe { &*(context_handle as *const NnExecutionContext) };

        exec_ctx.compute()?;
        Ok(0)
    })
}

/// Get output by index
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnGraphExecutionContext_nativeGetOutputByIndex(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    index: jint,
) -> jbyteArray {
    // Get native data first
    let data = {
        let exec_ctx = unsafe { &*(context_handle as *const NnExecutionContext) };
        match exec_ctx.get_output(index as u32) {
            Ok(Some(tensor)) => tensor.data.clone(),
            _ => return std::ptr::null_mut(),
        }
    };

    // Create JNI array
    match env.byte_array_from_slice(&data) {
        Ok(arr) => arr.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

/// Get output by name
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnGraphExecutionContext_nativeGetOutputByName(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    _name: JString,
) -> jbyteArray {
    // Get native data first (use index 0 since Rust API uses indices)
    let data = {
        let exec_ctx = unsafe { &*(context_handle as *const NnExecutionContext) };
        match exec_ctx.get_output(0) {
            Ok(Some(tensor)) => tensor.data.clone(),
            _ => return std::ptr::null_mut(),
        }
    };

    // Create JNI array
    match env.byte_array_from_slice(&data) {
        Ok(arr) => arr.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

/// Get output dimensions by index
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnGraphExecutionContext_nativeGetOutputDimensions(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    index: jint,
) -> jintArray {
    // Get native data first
    let dims: Vec<i32> = {
        let exec_ctx = unsafe { &*(context_handle as *const NnExecutionContext) };
        match exec_ctx.get_output(index as u32) {
            Ok(Some(tensor)) => tensor.dimensions.iter().map(|&d| d as i32).collect(),
            _ => return std::ptr::null_mut(),
        }
    };

    // Create JNI array
    let result = match env.new_int_array(dims.len() as i32) {
        Ok(arr) => arr,
        Err(_) => return std::ptr::null_mut(),
    };

    if env.set_int_array_region(&result, 0, &dims).is_err() {
        return std::ptr::null_mut();
    }

    result.into_raw()
}

/// Get output dimensions by name
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnGraphExecutionContext_nativeGetOutputDimensionsByName(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    _name: JString,
) -> jintArray {
    // Get native data first (use index 0 since Rust API uses indices)
    let dims: Vec<i32> = {
        let exec_ctx = unsafe { &*(context_handle as *const NnExecutionContext) };
        match exec_ctx.get_output(0) {
            Ok(Some(tensor)) => tensor.dimensions.iter().map(|&d| d as i32).collect(),
            _ => return std::ptr::null_mut(),
        }
    };

    // Create JNI array
    let result = match env.new_int_array(dims.len() as i32) {
        Ok(arr) => arr,
        Err(_) => return std::ptr::null_mut(),
    };

    if env.set_int_array_region(&result, 0, &dims).is_err() {
        return std::ptr::null_mut();
    }

    result.into_raw()
}

/// Get output type by index
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnGraphExecutionContext_nativeGetOutputType(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    index: jint,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, -1, || {
        let exec_ctx = unsafe { &*(context_handle as *const NnExecutionContext) };

        let tensor = exec_ctx.get_output(index as u32)?
            .ok_or_else(|| crate::error::WasmtimeError::Runtime {
                message: format!("Output tensor at index {} not found", index),
                backtrace: None,
            })?;

        Ok(tensor.tensor_type as jint)
    })
}

/// Get output type by name
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnGraphExecutionContext_nativeGetOutputTypeByName(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    _name: JString,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, -1, || {
        let exec_ctx = unsafe { &*(context_handle as *const NnExecutionContext) };

        let tensor = exec_ctx.get_output(0)?
            .ok_or_else(|| crate::error::WasmtimeError::Runtime {
                message: "Output tensor not found".to_string(),
                backtrace: None,
            })?;

        Ok(tensor.tensor_type as jint)
    })
}

/// Get input count
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnGraphExecutionContext_nativeGetInputCount(
    _env: JNIEnv,
    _class: JClass,
    _context_handle: jlong,
) -> jint {
    // The placeholder implementation doesn't track exact input count
    // Return a reasonable default
    1
}

/// Get output count
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnGraphExecutionContext_nativeGetOutputCount(
    _env: JNIEnv,
    _class: JClass,
    _context_handle: jlong,
) -> jint {
    // The placeholder implementation doesn't track exact output count
    // Return a reasonable default
    1
}

/// Get input names
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnGraphExecutionContext_nativeGetInputNames(
    mut env: JNIEnv,
    _class: JClass,
    _context_handle: jlong,
) -> jobjectArray {
    // Return empty array for now (no JNI operations inside closure)
    let string_class = match env.find_class("java/lang/String") {
        Ok(c) => c,
        Err(_) => return std::ptr::null_mut(),
    };

    let empty_string = match env.new_string("") {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };

    match env.new_object_array(0, string_class, empty_string) {
        Ok(arr) => arr.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

/// Get output names
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnGraphExecutionContext_nativeGetOutputNames(
    mut env: JNIEnv,
    _class: JClass,
    _context_handle: jlong,
) -> jobjectArray {
    // Return empty array for now (no JNI operations inside closure)
    let string_class = match env.find_class("java/lang/String") {
        Ok(c) => c,
        Err(_) => return std::ptr::null_mut(),
    };

    let empty_string = match env.new_string("") {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };

    match env.new_object_array(0, string_class, empty_string) {
        Ok(arr) => arr.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

/// Get input dimensions by index
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnGraphExecutionContext_nativeGetInputDimensionsByIndex(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    index: jint,
) -> jintArray {
    // Get native data first
    let dims: Vec<i32> = {
        let exec_ctx = unsafe { &*(context_handle as *const NnExecutionContext) };
        match exec_ctx.get_input(index as u32) {
            Ok(Some(tensor)) => tensor.dimensions.iter().map(|&d| d as i32).collect(),
            _ => return std::ptr::null_mut(),
        }
    };

    // Create JNI array
    let result = match env.new_int_array(dims.len() as i32) {
        Ok(arr) => arr,
        Err(_) => return std::ptr::null_mut(),
    };

    if env.set_int_array_region(&result, 0, &dims).is_err() {
        return std::ptr::null_mut();
    }

    result.into_raw()
}

/// Get input type by index
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnGraphExecutionContext_nativeGetInputTypeByIndex(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    index: jint,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, -1, || {
        let exec_ctx = unsafe { &*(context_handle as *const NnExecutionContext) };

        let tensor = exec_ctx.get_input(index as u32)?
            .ok_or_else(|| crate::error::WasmtimeError::Runtime {
                message: format!("Input tensor at index {} not found", index),
                backtrace: None,
            })?;

        Ok(tensor.tensor_type as jint)
    })
}

/// Close execution context
#[cfg(all(feature = "jni-bindings", feature = "wasi-nn"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnGraphExecutionContext_nativeClose(
    _env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
) {
    if context_handle != 0 {
        unsafe {
            let exec_ctx = Box::from_raw(context_handle as *mut NnExecutionContext);
            exec_ctx.close();
        }
    }
}

// ===== Stub implementations when wasi-nn feature is not enabled =====

#[cfg(not(all(feature = "jni-bindings", feature = "wasi-nn")))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnContextFactory_nativeCreateContext(
    _env: jni::JNIEnv,
    _class: jni::objects::JClass,
) -> jlong {
    0
}

#[cfg(not(all(feature = "jni-bindings", feature = "wasi-nn")))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnContextFactory_nativeIsNnAvailable(
    _env: jni::JNIEnv,
    _class: jni::objects::JClass,
) -> jint {
    0
}

#[cfg(not(all(feature = "jni-bindings", feature = "wasi-nn")))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_nn_JniNnContextFactory_nativeGetDefaultExecutionTarget(
    _env: jni::JNIEnv,
    _class: jni::objects::JClass,
) -> jint {
    0
}
