//! Panama FFI bindings for WASI-NN (WebAssembly System Interface for Neural Networks)
//!
//! This module provides C-compatible FFI functions for use by the wasmtime4j-panama module
//! to access WASI-NN machine learning operations.
//!
//! All functions use C calling conventions and handle memory management appropriately.

#![allow(unused_variables)]

use std::os::raw::{c_char, c_int, c_longlong, c_uchar, c_void};
use std::slice;
use std::sync::Arc;

#[cfg(feature = "wasi-nn")]
use crate::wasi_nn::{
    NnExecutionContext, NnExecutionTarget, NnGraph, NnGraphEncoding, NnTensor, NnTensorType,
    WasiNnContext,
};

// =============================================================================
// Context Functions
// =============================================================================

/// Create a new WASI-NN context
///
/// # Returns
/// Pointer to the context, or null on error
#[cfg(feature = "wasi-nn")]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_context_create() -> *mut c_void {
    match WasiNnContext::with_defaults() {
        Ok(context) => Box::into_raw(Box::new(context)) as *mut c_void,
        Err(_) => std::ptr::null_mut(),
    }
}

#[cfg(not(feature = "wasi-nn"))]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_context_create() -> *mut c_void {
    std::ptr::null_mut()
}

/// Check if WASI-NN is available
///
/// # Returns
/// 1 if available, 0 if not
#[cfg(feature = "wasi-nn")]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_is_available() -> c_int {
    1
}

#[cfg(not(feature = "wasi-nn"))]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_is_available() -> c_int {
    0
}

/// Get default execution target
///
/// # Returns
/// The default target ordinal (0 = CPU)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_get_default_target() -> c_int {
    0 // CPU
}

/// Close a WASI-NN context
#[cfg(feature = "wasi-nn")]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_context_close(context_ptr: *mut c_void) {
    if !context_ptr.is_null() {
        unsafe {
            let _ = Box::from_raw(context_ptr as *mut WasiNnContext);
        }
    }
}

#[cfg(not(feature = "wasi-nn"))]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_context_close(_context_ptr: *mut c_void) {}

// =============================================================================
// Graph Loading Functions
// =============================================================================

/// Load a graph from model data
///
/// # Parameters
/// - `context_ptr`: Pointer to the WASI-NN context
/// - `data_ptr`: Pointer to the model data
/// - `data_len`: Length of the model data
/// - `encoding_ordinal`: The graph encoding format
/// - `target_ordinal`: The execution target
///
/// # Returns
/// Pointer to the graph, or null on error
#[cfg(feature = "wasi-nn")]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_load_graph(
    context_ptr: *mut c_void,
    data_ptr: *const c_uchar,
    data_len: c_longlong,
    encoding_ordinal: c_int,
    target_ordinal: c_int,
) -> *mut c_void {
    if context_ptr.is_null() || data_ptr.is_null() || data_len <= 0 {
        return std::ptr::null_mut();
    }

    let context = unsafe { &*(context_ptr as *const WasiNnContext) };
    let data = unsafe { slice::from_raw_parts(data_ptr, data_len as usize) };

    let encoding = NnGraphEncoding::from_native_code(encoding_ordinal)
        .unwrap_or(NnGraphEncoding::Autodetect);
    let target =
        NnExecutionTarget::from_native_code(target_ordinal).unwrap_or(NnExecutionTarget::Cpu);

    match context.load_graph(data, encoding, target) {
        Ok(graph) => Arc::into_raw(graph) as *mut c_void,
        Err(_) => std::ptr::null_mut(),
    }
}

#[cfg(not(feature = "wasi-nn"))]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_load_graph(
    _context_ptr: *mut c_void,
    _data_ptr: *const c_uchar,
    _data_len: c_longlong,
    _encoding_ordinal: c_int,
    _target_ordinal: c_int,
) -> *mut c_void {
    std::ptr::null_mut()
}

/// Load a graph by name
///
/// # Parameters
/// - `context_ptr`: Pointer to the WASI-NN context
/// - `name_ptr`: Pointer to the null-terminated model name string
/// - `target_ordinal`: The execution target
///
/// # Returns
/// Pointer to the graph, or null on error
#[cfg(feature = "wasi-nn")]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_load_graph_by_name(
    context_ptr: *mut c_void,
    name_ptr: *const c_char,
    target_ordinal: c_int,
) -> *mut c_void {
    if context_ptr.is_null() || name_ptr.is_null() {
        return std::ptr::null_mut();
    }

    let context = unsafe { &*(context_ptr as *const WasiNnContext) };
    let name = unsafe {
        match std::ffi::CStr::from_ptr(name_ptr).to_str() {
            Ok(s) => s,
            Err(_) => return std::ptr::null_mut(),
        }
    };

    match context.load_graph_by_name(name) {
        Ok(graph) => Arc::into_raw(graph) as *mut c_void,
        Err(_) => std::ptr::null_mut(),
    }
}

#[cfg(not(feature = "wasi-nn"))]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_load_graph_by_name(
    _context_ptr: *mut c_void,
    _name_ptr: *const c_char,
    _target_ordinal: c_int,
) -> *mut c_void {
    std::ptr::null_mut()
}

/// Get supported encodings
///
/// # Parameters
/// - `context_ptr`: Pointer to the WASI-NN context
/// - `out_encodings`: Pointer to array to receive encoding ordinals
/// - `max_count`: Maximum number of encodings to return
///
/// # Returns
/// Number of encodings written, or -1 on error
#[cfg(feature = "wasi-nn")]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_get_supported_encodings(
    context_ptr: *mut c_void,
    out_encodings: *mut c_int,
    max_count: c_int,
) -> c_int {
    if context_ptr.is_null() || out_encodings.is_null() || max_count <= 0 {
        return -1;
    }

    let context = unsafe { &*(context_ptr as *const WasiNnContext) };
    let encodings = context.supported_encodings();

    let count = std::cmp::min(encodings.len(), max_count as usize);
    let mut i = 0;
    for encoding in encodings.iter().take(count) {
        unsafe {
            *out_encodings.add(i) = *encoding as c_int;
        }
        i += 1;
    }

    count as c_int
}

#[cfg(not(feature = "wasi-nn"))]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_get_supported_encodings(
    _context_ptr: *mut c_void,
    _out_encodings: *mut c_int,
    _max_count: c_int,
) -> c_int {
    0
}

/// Get supported targets
///
/// # Parameters
/// - `context_ptr`: Pointer to the WASI-NN context
/// - `out_targets`: Pointer to array to receive target ordinals
/// - `max_count`: Maximum number of targets to return
///
/// # Returns
/// Number of targets written, or -1 on error
#[cfg(feature = "wasi-nn")]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_get_supported_targets(
    context_ptr: *mut c_void,
    out_targets: *mut c_int,
    max_count: c_int,
) -> c_int {
    if context_ptr.is_null() || out_targets.is_null() || max_count <= 0 {
        return -1;
    }

    let context = unsafe { &*(context_ptr as *const WasiNnContext) };
    let targets = context.supported_targets();

    let count = std::cmp::min(targets.len(), max_count as usize);
    let mut i = 0;
    for target in targets.iter().take(count) {
        unsafe {
            *out_targets.add(i) = *target as c_int;
        }
        i += 1;
    }

    count as c_int
}

#[cfg(not(feature = "wasi-nn"))]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_get_supported_targets(
    _context_ptr: *mut c_void,
    _out_targets: *mut c_int,
    _max_count: c_int,
) -> c_int {
    0
}

/// Check if encoding is supported
#[cfg(feature = "wasi-nn")]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_is_encoding_supported(
    context_ptr: *mut c_void,
    encoding_ordinal: c_int,
) -> c_int {
    if context_ptr.is_null() {
        return 0;
    }

    let context = unsafe { &*(context_ptr as *const WasiNnContext) };
    let encoding = match NnGraphEncoding::from_native_code(encoding_ordinal) {
        Some(e) => e,
        None => return 0,
    };

    if context.is_encoding_supported(encoding) {
        1
    } else {
        0
    }
}

#[cfg(not(feature = "wasi-nn"))]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_is_encoding_supported(
    _context_ptr: *mut c_void,
    _encoding_ordinal: c_int,
) -> c_int {
    0
}

/// Check if target is supported
#[cfg(feature = "wasi-nn")]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_is_target_supported(
    context_ptr: *mut c_void,
    target_ordinal: c_int,
) -> c_int {
    if context_ptr.is_null() {
        return 0;
    }

    let context = unsafe { &*(context_ptr as *const WasiNnContext) };
    let target = match NnExecutionTarget::from_native_code(target_ordinal) {
        Some(t) => t,
        None => return 0,
    };

    if context.is_target_supported(target) {
        1
    } else {
        0
    }
}

#[cfg(not(feature = "wasi-nn"))]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_is_target_supported(
    _context_ptr: *mut c_void,
    _target_ordinal: c_int,
) -> c_int {
    0
}

// =============================================================================
// Graph Functions
// =============================================================================

/// Create an execution context from a graph
#[cfg(feature = "wasi-nn")]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_graph_create_exec_context(
    graph_ptr: *mut c_void,
) -> *mut c_void {
    if graph_ptr.is_null() {
        return std::ptr::null_mut();
    }

    let graph = unsafe { &*(graph_ptr as *const NnGraph) };

    match graph.create_execution_context() {
        Ok(ctx) => Box::into_raw(Box::new(ctx)) as *mut c_void,
        Err(_) => std::ptr::null_mut(),
    }
}

#[cfg(not(feature = "wasi-nn"))]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_graph_create_exec_context(
    _graph_ptr: *mut c_void,
) -> *mut c_void {
    std::ptr::null_mut()
}

/// Close a graph
#[cfg(feature = "wasi-nn")]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_graph_close(graph_ptr: *mut c_void) {
    if !graph_ptr.is_null() {
        unsafe {
            let graph = Arc::from_raw(graph_ptr as *const NnGraph);
            graph.close();
        }
    }
}

#[cfg(not(feature = "wasi-nn"))]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_graph_close(_graph_ptr: *mut c_void) {}

// =============================================================================
// Execution Context Functions
// =============================================================================

/// Set input tensor by index
#[cfg(feature = "wasi-nn")]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_exec_set_input(
    ctx_ptr: *mut c_void,
    index: c_int,
    dims_ptr: *const c_int,
    dims_len: c_int,
    type_ordinal: c_int,
    data_ptr: *const c_uchar,
    data_len: c_longlong,
) -> c_int {
    if ctx_ptr.is_null()
        || dims_ptr.is_null()
        || data_ptr.is_null()
        || dims_len <= 0
        || data_len <= 0
    {
        return -1;
    }

    let exec_ctx = unsafe { &*(ctx_ptr as *const NnExecutionContext) };

    // Read dimensions
    let dims: Vec<u32> = unsafe {
        slice::from_raw_parts(dims_ptr, dims_len as usize)
            .iter()
            .map(|&d| d as u32)
            .collect()
    };

    // Read data
    let data = unsafe { slice::from_raw_parts(data_ptr, data_len as usize).to_vec() };

    // Get tensor type
    let tensor_type =
        NnTensorType::from_native_code(type_ordinal).unwrap_or(NnTensorType::Fp32);

    // Create tensor
    let tensor = NnTensor::new(dims, tensor_type, data);

    match exec_ctx.set_input(index as u32, tensor) {
        Ok(()) => 0,
        Err(_) => -1,
    }
}

#[cfg(not(feature = "wasi-nn"))]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_exec_set_input(
    _ctx_ptr: *mut c_void,
    _index: c_int,
    _dims_ptr: *const c_int,
    _dims_len: c_int,
    _type_ordinal: c_int,
    _data_ptr: *const c_uchar,
    _data_len: c_longlong,
) -> c_int {
    -1
}

/// Compute inference
#[cfg(feature = "wasi-nn")]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_exec_compute(ctx_ptr: *mut c_void) -> c_int {
    if ctx_ptr.is_null() {
        return -1;
    }

    let exec_ctx = unsafe { &*(ctx_ptr as *const NnExecutionContext) };

    match exec_ctx.compute() {
        Ok(()) => 0,
        Err(_) => -1,
    }
}

#[cfg(not(feature = "wasi-nn"))]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_exec_compute(_ctx_ptr: *mut c_void) -> c_int {
    -1
}

/// Get output tensor data by index
///
/// # Parameters
/// - `ctx_ptr`: Execution context pointer
/// - `index`: Output index
/// - `out_data`: Pointer to buffer to receive data
/// - `max_len`: Maximum buffer length
///
/// # Returns
/// Actual data length, or -1 on error
#[cfg(feature = "wasi-nn")]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_exec_get_output(
    ctx_ptr: *mut c_void,
    index: c_int,
    out_data: *mut c_uchar,
    max_len: c_longlong,
) -> c_longlong {
    if ctx_ptr.is_null() || out_data.is_null() || max_len <= 0 {
        return -1;
    }

    let exec_ctx = unsafe { &*(ctx_ptr as *const NnExecutionContext) };

    match exec_ctx.get_output(index as u32) {
        Ok(Some(tensor)) => {
            let len = std::cmp::min(tensor.data.len(), max_len as usize);
            unsafe {
                std::ptr::copy_nonoverlapping(tensor.data.as_ptr(), out_data, len);
            }
            len as c_longlong
        }
        _ => -1,
    }
}

#[cfg(not(feature = "wasi-nn"))]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_exec_get_output(
    _ctx_ptr: *mut c_void,
    _index: c_int,
    _out_data: *mut c_uchar,
    _max_len: c_longlong,
) -> c_longlong {
    -1
}

/// Get output tensor size by index
#[cfg(feature = "wasi-nn")]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_exec_get_output_size(
    ctx_ptr: *mut c_void,
    index: c_int,
) -> c_longlong {
    if ctx_ptr.is_null() {
        return -1;
    }

    let exec_ctx = unsafe { &*(ctx_ptr as *const NnExecutionContext) };

    match exec_ctx.get_output(index as u32) {
        Ok(Some(tensor)) => tensor.data.len() as c_longlong,
        _ => -1,
    }
}

#[cfg(not(feature = "wasi-nn"))]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_exec_get_output_size(
    _ctx_ptr: *mut c_void,
    _index: c_int,
) -> c_longlong {
    -1
}

/// Get output tensor dimensions by index
#[cfg(feature = "wasi-nn")]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_exec_get_output_dims(
    ctx_ptr: *mut c_void,
    index: c_int,
    out_dims: *mut c_int,
    max_dims: c_int,
) -> c_int {
    if ctx_ptr.is_null() || out_dims.is_null() || max_dims <= 0 {
        return -1;
    }

    let exec_ctx = unsafe { &*(ctx_ptr as *const NnExecutionContext) };

    match exec_ctx.get_output(index as u32) {
        Ok(Some(tensor)) => {
            let count = std::cmp::min(tensor.dimensions.len(), max_dims as usize);
            for i in 0..count {
                unsafe {
                    *out_dims.add(i) = tensor.dimensions[i] as c_int;
                }
            }
            count as c_int
        }
        _ => -1,
    }
}

#[cfg(not(feature = "wasi-nn"))]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_exec_get_output_dims(
    _ctx_ptr: *mut c_void,
    _index: c_int,
    _out_dims: *mut c_int,
    _max_dims: c_int,
) -> c_int {
    -1
}

/// Get output tensor type by index
#[cfg(feature = "wasi-nn")]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_exec_get_output_type(
    ctx_ptr: *mut c_void,
    index: c_int,
) -> c_int {
    if ctx_ptr.is_null() {
        return -1;
    }

    let exec_ctx = unsafe { &*(ctx_ptr as *const NnExecutionContext) };

    match exec_ctx.get_output(index as u32) {
        Ok(Some(tensor)) => tensor.tensor_type as c_int,
        _ => -1,
    }
}

#[cfg(not(feature = "wasi-nn"))]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_exec_get_output_type(
    _ctx_ptr: *mut c_void,
    _index: c_int,
) -> c_int {
    -1
}

/// Close an execution context
#[cfg(feature = "wasi-nn")]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_exec_close(ctx_ptr: *mut c_void) {
    if !ctx_ptr.is_null() {
        unsafe {
            let ctx = Box::from_raw(ctx_ptr as *mut NnExecutionContext);
            ctx.close();
        }
    }
}

#[cfg(not(feature = "wasi-nn"))]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_nn_exec_close(_ctx_ptr: *mut c_void) {}
