//! Panama FFI bindings for WASI-NN host-side inference.
//!
//! Provides C-compatible extern functions for NnContext, NnGraph, and
//! NnGraphExecutionContext, used by the wasmtime4j-panama Java module.

#[cfg(feature = "wasi-nn")]
use std::ffi::CStr;
use std::ffi::CString;
use std::os::raw::{c_char, c_int, c_void};
use std::ptr;

#[cfg(feature = "wasi-nn")]
use crate::wasi_nn::{NnContext, NnExecCtx, NnGraph};

/// Helper: writes an error message into the provided buffer.
fn write_error(error_buf: *mut c_char, error_buf_len: c_int, msg: &str) {
    if error_buf.is_null() || error_buf_len <= 0 {
        return;
    }
    let c_msg = CString::new(msg).unwrap_or_else(|_| CString::new("Unknown error").unwrap());
    let bytes = c_msg.as_bytes_with_nul();
    let copy_len = std::cmp::min(bytes.len(), error_buf_len as usize);
    unsafe {
        ptr::copy_nonoverlapping(bytes.as_ptr(), error_buf as *mut u8, copy_len);
        // Ensure null termination
        if copy_len < error_buf_len as usize {
            *error_buf.add(copy_len) = 0;
        } else {
            *error_buf.add(error_buf_len as usize - 1) = 0;
        }
    }
}

const FFI_SUCCESS: c_int = 0;
const FFI_ERROR: c_int = -1;

// ============================================================================
// NnContext Panama FFI
// ============================================================================

/// Creates a new NnContext. Returns a pointer (null if feature not enabled).
#[no_mangle]
pub extern "C" fn wasmtime4j_nn_context_create() -> *mut c_void {
    #[cfg(feature = "wasi-nn")]
    {
        let ctx = Box::new(NnContext::new());
        Box::into_raw(ctx) as *mut c_void
    }
    #[cfg(not(feature = "wasi-nn"))]
    {
        ptr::null_mut()
    }
}

/// Returns 1 if any backends are available, 0 otherwise.
#[no_mangle]
pub extern "C" fn wasmtime4j_nn_context_is_available(ctx_ptr: *const c_void) -> c_int {
    #[cfg(feature = "wasi-nn")]
    {
        if ctx_ptr.is_null() {
            return 0;
        }
        let ctx = unsafe { &*(ctx_ptr as *const NnContext) };
        if ctx.is_available() { 1 } else { 0 }
    }
    #[cfg(not(feature = "wasi-nn"))]
    {
        0
    }
}

/// Gets supported encoding codes. Writes codes to out_buf, sets out_count.
/// Returns FFI_SUCCESS or FFI_ERROR.
#[no_mangle]
pub extern "C" fn wasmtime4j_nn_context_supported_encodings(
    ctx_ptr: *const c_void,
    out_buf: *mut c_int,
    out_buf_capacity: c_int,
    out_count: *mut c_int,
) -> c_int {
    #[cfg(feature = "wasi-nn")]
    {
        if ctx_ptr.is_null() || out_count.is_null() {
            return FFI_ERROR;
        }
        let ctx = unsafe { &*(ctx_ptr as *const NnContext) };
        let codes = ctx.supported_encoding_codes();
        unsafe {
            *out_count = codes.len() as c_int;
        }
        if !out_buf.is_null() {
            let copy_len = std::cmp::min(codes.len(), out_buf_capacity as usize);
            for i in 0..copy_len {
                unsafe {
                    *out_buf.add(i) = codes[i] as c_int;
                }
            }
        }
        FFI_SUCCESS
    }
    #[cfg(not(feature = "wasi-nn"))]
    {
        if !out_count.is_null() {
            unsafe { *out_count = 0; }
        }
        FFI_SUCCESS
    }
}

/// Loads a graph from byte data parts.
/// parts_ptrs: array of pointers to byte data
/// parts_lens: array of lengths for each part
/// num_parts: number of parts
/// Returns graph pointer or null on error.
#[no_mangle]
pub extern "C" fn wasmtime4j_nn_context_load_graph(
    ctx_ptr: *mut c_void,
    parts_ptrs: *const *const u8,
    parts_lens: *const c_int,
    num_parts: c_int,
    encoding_code: c_int,
    target_code: c_int,
    error_buf: *mut c_char,
    error_buf_len: c_int,
) -> *mut c_void {
    #[cfg(feature = "wasi-nn")]
    {
        if ctx_ptr.is_null() || parts_ptrs.is_null() || parts_lens.is_null() || num_parts <= 0 {
            write_error(error_buf, error_buf_len, "Invalid arguments");
            return ptr::null_mut();
        }
        let ctx = unsafe { &mut *(ctx_ptr as *mut NnContext) };

        let encoding = match crate::wasi_nn::graph_encoding_from_java(encoding_code) {
            Ok(e) => e,
            Err(msg) => {
                write_error(error_buf, error_buf_len, &msg);
                return ptr::null_mut();
            }
        };
        let target = match crate::wasi_nn::execution_target_from_java(target_code) {
            Ok(t) => t,
            Err(msg) => {
                write_error(error_buf, error_buf_len, &msg);
                return ptr::null_mut();
            }
        };

        // Build slices from raw pointers
        let mut part_slices: Vec<&[u8]> = Vec::with_capacity(num_parts as usize);
        for i in 0..num_parts as usize {
            unsafe {
                let ptr = *parts_ptrs.add(i);
                let len = *parts_lens.add(i) as usize;
                if ptr.is_null() {
                    write_error(error_buf, error_buf_len, &format!("Part {} pointer is null", i));
                    return ptr::null_mut();
                }
                part_slices.push(std::slice::from_raw_parts(ptr, len));
            }
        }

        match ctx.load_graph(&part_slices, encoding, target) {
            Ok(graph) => {
                let boxed = Box::new(graph);
                Box::into_raw(boxed) as *mut c_void
            }
            Err(msg) => {
                write_error(error_buf, error_buf_len, &msg);
                ptr::null_mut()
            }
        }
    }
    #[cfg(not(feature = "wasi-nn"))]
    {
        write_error(error_buf, error_buf_len, "wasi-nn feature not enabled");
        ptr::null_mut()
    }
}

/// Gets backend info as a null-terminated JSON string.
/// Returns a pointer to a heap-allocated CString. Caller must free with wasmtime4j_nn_free_string.
#[no_mangle]
pub extern "C" fn wasmtime4j_nn_context_get_backend_info(
    ctx_ptr: *const c_void,
) -> *mut c_char {
    #[cfg(feature = "wasi-nn")]
    {
        if ctx_ptr.is_null() {
            return ptr::null_mut();
        }
        let ctx = unsafe { &*(ctx_ptr as *const NnContext) };
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
        match CString::new(json) {
            Ok(c) => c.into_raw(),
            Err(_) => ptr::null_mut(),
        }
    }
    #[cfg(not(feature = "wasi-nn"))]
    {
        ptr::null_mut()
    }
}

/// Frees a string allocated by wasmtime4j_nn_context_get_backend_info.
#[no_mangle]
pub extern "C" fn wasmtime4j_nn_free_string(ptr: *mut c_char) {
    if !ptr.is_null() {
        unsafe {
            let _ = CString::from_raw(ptr);
        }
    }
}

/// Closes and frees an NnContext.
#[no_mangle]
pub extern "C" fn wasmtime4j_nn_context_close(ctx_ptr: *mut c_void) {
    #[cfg(feature = "wasi-nn")]
    {
        if !ctx_ptr.is_null() {
            let _ = unsafe { Box::from_raw(ctx_ptr as *mut NnContext) };
        }
    }
}

// ============================================================================
// NnGraph Panama FFI
// ============================================================================

/// Gets the graph encoding code.
#[no_mangle]
pub extern "C" fn wasmtime4j_nn_graph_get_encoding(graph_ptr: *const c_void) -> c_int {
    #[cfg(feature = "wasi-nn")]
    {
        if graph_ptr.is_null() {
            return -1;
        }
        let graph = unsafe { &*(graph_ptr as *const NnGraph) };
        graph.encoding_code()
    }
    #[cfg(not(feature = "wasi-nn"))]
    {
        -1
    }
}

/// Gets the execution target code.
#[no_mangle]
pub extern "C" fn wasmtime4j_nn_graph_get_target(graph_ptr: *const c_void) -> c_int {
    #[cfg(feature = "wasi-nn")]
    {
        if graph_ptr.is_null() {
            return -1;
        }
        let graph = unsafe { &*(graph_ptr as *const NnGraph) };
        graph.target_code()
    }
    #[cfg(not(feature = "wasi-nn"))]
    {
        -1
    }
}

/// Creates an execution context from the graph.
/// Returns exec context pointer or null on error.
#[no_mangle]
pub extern "C" fn wasmtime4j_nn_graph_create_exec_ctx(
    graph_ptr: *const c_void,
    error_buf: *mut c_char,
    error_buf_len: c_int,
) -> *mut c_void {
    #[cfg(feature = "wasi-nn")]
    {
        if graph_ptr.is_null() {
            write_error(error_buf, error_buf_len, "Graph pointer is null");
            return ptr::null_mut();
        }
        let graph = unsafe { &*(graph_ptr as *const NnGraph) };
        match graph.create_execution_context() {
            Ok(exec) => {
                let boxed = Box::new(exec);
                Box::into_raw(boxed) as *mut c_void
            }
            Err(msg) => {
                write_error(error_buf, error_buf_len, &msg);
                ptr::null_mut()
            }
        }
    }
    #[cfg(not(feature = "wasi-nn"))]
    {
        write_error(error_buf, error_buf_len, "wasi-nn feature not enabled");
        ptr::null_mut()
    }
}

/// Closes and frees an NnGraph.
#[no_mangle]
pub extern "C" fn wasmtime4j_nn_graph_close(graph_ptr: *mut c_void) {
    #[cfg(feature = "wasi-nn")]
    {
        if !graph_ptr.is_null() {
            let _ = unsafe { Box::from_raw(graph_ptr as *mut NnGraph) };
        }
    }
}

// ============================================================================
// NnGraphExecutionContext Panama FFI
// ============================================================================

/// Sets an input tensor by index.
#[no_mangle]
pub extern "C" fn wasmtime4j_nn_exec_set_input_by_index(
    exec_ptr: *mut c_void,
    index: c_int,
    dims: *const c_int,
    num_dims: c_int,
    tensor_type: c_int,
    data: *const u8,
    data_len: c_int,
    error_buf: *mut c_char,
    error_buf_len: c_int,
) -> c_int {
    #[cfg(feature = "wasi-nn")]
    {
        if exec_ptr.is_null() || dims.is_null() || data.is_null() {
            write_error(error_buf, error_buf_len, "Invalid arguments");
            return FFI_ERROR;
        }
        let exec = unsafe { &mut *(exec_ptr as *mut NnExecCtx) };

        let ty = match crate::wasi_nn::tensor_type_from_java(tensor_type) {
            Ok(t) => t,
            Err(msg) => {
                write_error(error_buf, error_buf_len, &msg);
                return FFI_ERROR;
            }
        };

        let dims_slice = unsafe { std::slice::from_raw_parts(dims, num_dims as usize) };
        let dims_u32: Vec<u32> = dims_slice.iter().map(|&d| d as u32).collect();
        let data_slice = unsafe { std::slice::from_raw_parts(data, data_len as usize) };

        match exec.set_input_by_index(index as u32, &dims_u32, ty, data_slice) {
            Ok(()) => FFI_SUCCESS,
            Err(msg) => {
                write_error(error_buf, error_buf_len, &msg);
                FFI_ERROR
            }
        }
    }
    #[cfg(not(feature = "wasi-nn"))]
    {
        write_error(error_buf, error_buf_len, "wasi-nn feature not enabled");
        FFI_ERROR
    }
}

/// Sets an input tensor by name.
#[no_mangle]
pub extern "C" fn wasmtime4j_nn_exec_set_input_by_name(
    exec_ptr: *mut c_void,
    name: *const c_char,
    dims: *const c_int,
    num_dims: c_int,
    tensor_type: c_int,
    data: *const u8,
    data_len: c_int,
    error_buf: *mut c_char,
    error_buf_len: c_int,
) -> c_int {
    #[cfg(feature = "wasi-nn")]
    {
        if exec_ptr.is_null() || name.is_null() || dims.is_null() || data.is_null() {
            write_error(error_buf, error_buf_len, "Invalid arguments");
            return FFI_ERROR;
        }
        let exec = unsafe { &mut *(exec_ptr as *mut NnExecCtx) };

        let name_str = unsafe {
            match CStr::from_ptr(name).to_str() {
                Ok(s) => s,
                Err(e) => {
                    write_error(error_buf, error_buf_len, &format!("Invalid UTF-8 name: {}", e));
                    return FFI_ERROR;
                }
            }
        };

        let ty = match crate::wasi_nn::tensor_type_from_java(tensor_type) {
            Ok(t) => t,
            Err(msg) => {
                write_error(error_buf, error_buf_len, &msg);
                return FFI_ERROR;
            }
        };

        let dims_slice = unsafe { std::slice::from_raw_parts(dims, num_dims as usize) };
        let dims_u32: Vec<u32> = dims_slice.iter().map(|&d| d as u32).collect();
        let data_slice = unsafe { std::slice::from_raw_parts(data, data_len as usize) };

        match exec.set_input_by_name(name_str, &dims_u32, ty, data_slice) {
            Ok(()) => FFI_SUCCESS,
            Err(msg) => {
                write_error(error_buf, error_buf_len, &msg);
                FFI_ERROR
            }
        }
    }
    #[cfg(not(feature = "wasi-nn"))]
    {
        write_error(error_buf, error_buf_len, "wasi-nn feature not enabled");
        FFI_ERROR
    }
}

/// Runs inference.
#[no_mangle]
pub extern "C" fn wasmtime4j_nn_exec_compute(
    exec_ptr: *mut c_void,
    error_buf: *mut c_char,
    error_buf_len: c_int,
) -> c_int {
    #[cfg(feature = "wasi-nn")]
    {
        if exec_ptr.is_null() {
            write_error(error_buf, error_buf_len, "Execution context pointer is null");
            return FFI_ERROR;
        }
        let exec = unsafe { &mut *(exec_ptr as *mut NnExecCtx) };
        match exec.compute() {
            Ok(()) => FFI_SUCCESS,
            Err(msg) => {
                write_error(error_buf, error_buf_len, &msg);
                FFI_ERROR
            }
        }
    }
    #[cfg(not(feature = "wasi-nn"))]
    {
        write_error(error_buf, error_buf_len, "wasi-nn feature not enabled");
        FFI_ERROR
    }
}

/// Gets an output tensor by index as serialized bytes.
/// Allocates output buffer and writes pointer/length to out_data/out_data_len.
/// Caller must free the allocated buffer with wasmtime4j_nn_free_buffer.
#[no_mangle]
pub extern "C" fn wasmtime4j_nn_exec_get_output_by_index(
    exec_ptr: *mut c_void,
    index: c_int,
    out_data: *mut *mut u8,
    out_data_len: *mut c_int,
    error_buf: *mut c_char,
    error_buf_len: c_int,
) -> c_int {
    #[cfg(feature = "wasi-nn")]
    {
        if exec_ptr.is_null() || out_data.is_null() || out_data_len.is_null() {
            write_error(error_buf, error_buf_len, "Invalid arguments");
            return FFI_ERROR;
        }
        let exec = unsafe { &mut *(exec_ptr as *mut NnExecCtx) };
        match exec.get_output_by_index(index as u32) {
            Ok(serialized) => {
                let len = serialized.len();
                let boxed = serialized.into_boxed_slice();
                let ptr = Box::into_raw(boxed) as *mut u8;
                unsafe {
                    *out_data = ptr;
                    *out_data_len = len as c_int;
                }
                FFI_SUCCESS
            }
            Err(msg) => {
                write_error(error_buf, error_buf_len, &msg);
                FFI_ERROR
            }
        }
    }
    #[cfg(not(feature = "wasi-nn"))]
    {
        write_error(error_buf, error_buf_len, "wasi-nn feature not enabled");
        FFI_ERROR
    }
}

/// Gets an output tensor by name as serialized bytes.
#[no_mangle]
pub extern "C" fn wasmtime4j_nn_exec_get_output_by_name(
    exec_ptr: *mut c_void,
    name: *const c_char,
    out_data: *mut *mut u8,
    out_data_len: *mut c_int,
    error_buf: *mut c_char,
    error_buf_len: c_int,
) -> c_int {
    #[cfg(feature = "wasi-nn")]
    {
        if exec_ptr.is_null() || name.is_null() || out_data.is_null() || out_data_len.is_null() {
            write_error(error_buf, error_buf_len, "Invalid arguments");
            return FFI_ERROR;
        }
        let exec = unsafe { &mut *(exec_ptr as *mut NnExecCtx) };
        let name_str = unsafe {
            match CStr::from_ptr(name).to_str() {
                Ok(s) => s,
                Err(e) => {
                    write_error(
                        error_buf,
                        error_buf_len,
                        &format!("Invalid UTF-8 name: {}", e),
                    );
                    return FFI_ERROR;
                }
            }
        };
        match exec.get_output_by_name(name_str) {
            Ok(serialized) => {
                let len = serialized.len();
                let boxed = serialized.into_boxed_slice();
                let ptr = Box::into_raw(boxed) as *mut u8;
                unsafe {
                    *out_data = ptr;
                    *out_data_len = len as c_int;
                }
                FFI_SUCCESS
            }
            Err(msg) => {
                write_error(error_buf, error_buf_len, &msg);
                FFI_ERROR
            }
        }
    }
    #[cfg(not(feature = "wasi-nn"))]
    {
        write_error(error_buf, error_buf_len, "wasi-nn feature not enabled");
        FFI_ERROR
    }
}

/// Frees a buffer allocated by get_output functions.
#[no_mangle]
pub extern "C" fn wasmtime4j_nn_free_buffer(ptr: *mut u8, len: c_int) {
    if !ptr.is_null() && len > 0 {
        unsafe {
            let slice = std::slice::from_raw_parts_mut(ptr, len as usize);
            let _ = Box::from_raw(slice as *mut [u8]);
        }
    }
}

/// Closes and frees an NnExecCtx.
#[no_mangle]
pub extern "C" fn wasmtime4j_nn_exec_close(exec_ptr: *mut c_void) {
    #[cfg(feature = "wasi-nn")]
    {
        if !exec_ptr.is_null() {
            let _ = unsafe { Box::from_raw(exec_ptr as *mut NnExecCtx) };
        }
    }
}
