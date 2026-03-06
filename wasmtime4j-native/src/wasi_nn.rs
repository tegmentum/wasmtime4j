//! WASI-NN host-side inference module.
//!
//! Provides host-side access to ML backends independently of the Wasmtime Store.
//! This module wraps wasmtime-wasi-nn's Backend, Graph, and ExecutionContext types
//! for use through JNI and Panama FFI.

#[cfg(feature = "wasi-nn")]
use std::collections::HashMap;

#[cfg(feature = "wasi-nn")]
use wasmtime_wasi_nn::backend::Id;
#[cfg(feature = "wasi-nn")]
use wasmtime_wasi_nn::wit::{ExecutionTarget, GraphEncoding, TensorType};

// ============================================================================
// Java enum ordinal <-> Rust enum conversion
// ============================================================================

/// Converts a Java NnGraphEncoding ordinal to a Rust GraphEncoding.
///
/// Java enum order: OPENVINO(0), ONNX(1), TENSORFLOW(2), PYTORCH(3),
///                  TENSORFLOWLITE(4), GGML(5), AUTODETECT(6)
/// WASI spec order: Openvino, Onnx, Tensorflow, Pytorch, Tensorflowlite, Autodetect, Ggml
/// Note: Java swaps GGML(5) and AUTODETECT(6) vs spec's Autodetect(5), Ggml(6).
#[cfg(feature = "wasi-nn")]
pub fn graph_encoding_from_java(code: i32) -> Result<GraphEncoding, String> {
    match code {
        0 => Ok(GraphEncoding::Openvino),
        1 => Ok(GraphEncoding::Onnx),
        2 => Ok(GraphEncoding::Tensorflow),
        3 => Ok(GraphEncoding::Pytorch),
        4 => Ok(GraphEncoding::Tensorflowlite),
        5 => Ok(GraphEncoding::Ggml),
        6 => Ok(GraphEncoding::Autodetect),
        _ => Err(format!("Unknown graph encoding code: {}", code)),
    }
}

/// Converts a Rust GraphEncoding to a Java NnGraphEncoding ordinal.
#[cfg(feature = "wasi-nn")]
pub fn graph_encoding_to_java(encoding: &GraphEncoding) -> i32 {
    match encoding {
        GraphEncoding::Openvino => 0,
        GraphEncoding::Onnx => 1,
        GraphEncoding::Tensorflow => 2,
        GraphEncoding::Pytorch => 3,
        GraphEncoding::Tensorflowlite => 4,
        GraphEncoding::Ggml => 5,
        GraphEncoding::Autodetect => 6,
    }
}

/// Converts a Java NnExecutionTarget ordinal to a Rust ExecutionTarget.
/// Java and WASI spec have same order: CPU(0), GPU(1), TPU(2).
#[cfg(feature = "wasi-nn")]
pub fn execution_target_from_java(code: i32) -> Result<ExecutionTarget, String> {
    match code {
        0 => Ok(ExecutionTarget::Cpu),
        1 => Ok(ExecutionTarget::Gpu),
        2 => Ok(ExecutionTarget::Tpu),
        _ => Err(format!("Unknown execution target code: {}", code)),
    }
}

/// Converts a Java NnTensorType ordinal to a Rust TensorType.
/// Java and WASI spec have same order: FP16(0), FP32(1), FP64(2), BF16(3), U8(4), I32(5), I64(6).
#[cfg(feature = "wasi-nn")]
pub fn tensor_type_from_java(code: i32) -> Result<TensorType, String> {
    match code {
        0 => Ok(TensorType::Fp16),
        1 => Ok(TensorType::Fp32),
        2 => Ok(TensorType::Fp64),
        3 => Ok(TensorType::Bf16),
        4 => Ok(TensorType::U8),
        5 => Ok(TensorType::I32),
        6 => Ok(TensorType::I64),
        _ => Err(format!("Unknown tensor type code: {}", code)),
    }
}

/// Converts a Rust TensorType to a Java NnTensorType ordinal.
#[cfg(feature = "wasi-nn")]
pub fn tensor_type_to_java(ty: &TensorType) -> i32 {
    match ty {
        TensorType::Fp16 => 0,
        TensorType::Fp32 => 1,
        TensorType::Fp64 => 2,
        TensorType::Bf16 => 3,
        TensorType::U8 => 4,
        TensorType::I32 => 5,
        TensorType::I64 => 6,
    }
}

// ============================================================================
// Core types
// ============================================================================

/// Host-side WASI-NN inference context.
///
/// Manages ML backends independently of the Wasmtime Store (no WASM guest required).
/// Each NnContext is Box-leaked as a raw pointer for FFI, and freed via `nn_context_close`.
#[cfg(feature = "wasi-nn")]
pub struct NnContext {
    backends: HashMap<GraphEncoding, wasmtime_wasi_nn::Backend>,
}

#[cfg(feature = "wasi-nn")]
impl NnContext {
    /// Creates a new NnContext with all available backends.
    pub fn new() -> Self {
        let backend_list = wasmtime_wasi_nn::backend::list();
        let mut backends = HashMap::new();
        for b in backend_list {
            backends.insert(b.encoding(), b);
        }
        log::debug!("NnContext created with {} backends", backends.len());
        Self { backends }
    }

    /// Checks if any backends are available.
    pub fn is_available(&self) -> bool {
        !self.backends.is_empty()
    }

    /// Gets the supported graph encoding codes (Java ordinals).
    pub fn supported_encoding_codes(&self) -> Vec<i32> {
        self.backends
            .keys()
            .map(|e| graph_encoding_to_java(e))
            .collect()
    }

    /// Checks if a specific encoding (Java ordinal) is supported.
    pub fn is_encoding_supported(&self, java_code: i32) -> bool {
        if let Ok(encoding) = graph_encoding_from_java(java_code) {
            self.backends.contains_key(&encoding)
        } else {
            false
        }
    }

    /// Gets backend names as strings.
    pub fn backend_names(&self) -> Vec<String> {
        self.backends
            .keys()
            .map(|e| format!("{e}").to_lowercase())
            .collect()
    }

    /// Gets the default backend name.
    pub fn default_backend_name(&self) -> Option<String> {
        self.backends
            .keys()
            .next()
            .map(|e| format!("{e}").to_lowercase())
    }

    /// Loads a graph from byte data parts.
    pub fn load_graph(
        &mut self,
        parts: &[&[u8]],
        encoding: GraphEncoding,
        target: ExecutionTarget,
    ) -> Result<NnGraph, String> {
        let backend = self
            .backends
            .get_mut(&encoding)
            .ok_or_else(|| format!("No backend available for encoding {encoding}"))?;
        let graph = backend
            .load(parts, target)
            .map_err(|e| format!("Failed to load graph: {e}"))?;
        Ok(NnGraph {
            graph,
            encoding,
            target,
        })
    }
}

/// Wrapper around a loaded WASI-NN graph (ML model).
/// Box-leaked as a raw pointer for FFI, freed via `nn_graph_close`.
#[cfg(feature = "wasi-nn")]
pub struct NnGraph {
    graph: wasmtime_wasi_nn::Graph,
    encoding: GraphEncoding,
    target: ExecutionTarget,
}

#[cfg(feature = "wasi-nn")]
impl NnGraph {
    /// Creates an execution context for running inference.
    pub fn create_execution_context(&self) -> Result<NnExecCtx, String> {
        let exec = self
            .graph
            .init_execution_context()
            .map_err(|e| format!("Failed to create execution context: {e}"))?;
        Ok(NnExecCtx { exec })
    }

    /// Gets the graph encoding as a Java ordinal.
    pub fn encoding_code(&self) -> i32 {
        graph_encoding_to_java(&self.encoding)
    }

    /// Gets the execution target as a Java ordinal.
    pub fn target_code(&self) -> i32 {
        match self.target {
            ExecutionTarget::Cpu => 0,
            ExecutionTarget::Gpu => 1,
            ExecutionTarget::Tpu => 2,
        }
    }
}

/// Wrapper around a WASI-NN execution context for running inference.
/// Box-leaked as a raw pointer for FFI, freed via `nn_exec_close`.
#[cfg(feature = "wasi-nn")]
pub struct NnExecCtx {
    exec: wasmtime_wasi_nn::ExecutionContext,
}

#[cfg(feature = "wasi-nn")]
impl NnExecCtx {
    /// Sets an input tensor by index.
    pub fn set_input_by_index(
        &mut self,
        index: u32,
        dimensions: &[u32],
        tensor_type: TensorType,
        data: &[u8],
    ) -> Result<(), String> {
        let tensor = wasmtime_wasi_nn::Tensor::new(dimensions.to_vec(), tensor_type, data.to_vec());
        self.exec
            .set_input(Id::Index(index), &tensor)
            .map_err(|e| format!("Failed to set input at index {index}: {e}"))
    }

    /// Sets an input tensor by name.
    pub fn set_input_by_name(
        &mut self,
        name: &str,
        dimensions: &[u32],
        tensor_type: TensorType,
        data: &[u8],
    ) -> Result<(), String> {
        let tensor = wasmtime_wasi_nn::Tensor::new(dimensions.to_vec(), tensor_type, data.to_vec());
        self.exec
            .set_input(Id::Name(name.to_string()), &tensor)
            .map_err(|e| format!("Failed to set input '{name}': {e}"))
    }

    /// Runs inference (no-arg compute).
    pub fn compute(&mut self) -> Result<(), String> {
        self.exec
            .compute()
            .map_err(|e| format!("Inference failed: {e}"))
    }

    /// Gets an output tensor by index. Returns serialized tensor bytes.
    ///
    /// Serialization format:
    /// `[num_dims: i32][dim0: i32]...[dimN: i32][tensor_type: i32][data: remaining bytes]`
    pub fn get_output_by_index(&mut self, index: u32) -> Result<Vec<u8>, String> {
        let tensor = self
            .exec
            .get_output(Id::Index(index))
            .map_err(|e| format!("Failed to get output at index {index}: {e}"))?;
        Ok(serialize_tensor(&tensor))
    }

    /// Gets an output tensor by name. Returns serialized tensor bytes.
    pub fn get_output_by_name(&mut self, name: &str) -> Result<Vec<u8>, String> {
        let tensor = self
            .exec
            .get_output(Id::Name(name.to_string()))
            .map_err(|e| format!("Failed to get output '{name}': {e}"))?;
        Ok(serialize_tensor(&tensor))
    }
}

/// Serializes a Tensor to bytes for FFI transfer.
///
/// Format: `[num_dims: i32][dim0: i32]...[dimN: i32][tensor_type: i32][data bytes]`
#[cfg(feature = "wasi-nn")]
pub fn serialize_tensor(tensor: &wasmtime_wasi_nn::Tensor) -> Vec<u8> {
    let num_dims = tensor.dimensions.len() as i32;
    let tensor_type_code = tensor_type_to_java(&tensor.ty);
    let header_size = (2 + tensor.dimensions.len()) * 4;
    let total_size = header_size + tensor.data.len();

    let mut buf = Vec::with_capacity(total_size);
    buf.extend_from_slice(&num_dims.to_le_bytes());
    for &dim in &tensor.dimensions {
        buf.extend_from_slice(&(dim as i32).to_le_bytes());
    }
    buf.extend_from_slice(&tensor_type_code.to_le_bytes());
    buf.extend_from_slice(&tensor.data);
    buf
}

// ============================================================================
// Feature detection (always compiled)
// ============================================================================

/// Returns 1 if the wasi-nn feature was compiled in, 0 otherwise.
/// This is always exported regardless of feature flags.
#[no_mangle]
pub extern "C" fn wasmtime4j_nn_is_feature_enabled() -> i32 {
    #[cfg(feature = "wasi-nn")]
    {
        1
    }
    #[cfg(not(feature = "wasi-nn"))]
    {
        0
    }
}

// ============================================================================
// Tests
// ============================================================================

#[cfg(all(test, feature = "wasi-nn"))]
mod tests {
    use super::*;

    #[test]
    fn test_nn_context_creation() {
        let ctx = NnContext::new();
        let _ = ctx.is_available();
        let encodings = ctx.supported_encoding_codes();
        assert!(encodings.len() <= 7);
    }

    #[test]
    fn test_graph_encoding_roundtrip() {
        for code in 0..7 {
            let encoding = graph_encoding_from_java(code).unwrap();
            let back = graph_encoding_to_java(&encoding);
            assert_eq!(code, back, "GraphEncoding roundtrip failed for code {code}");
        }
    }

    #[test]
    fn test_execution_target_roundtrip() {
        for code in 0..3 {
            let _target = execution_target_from_java(code).unwrap();
        }
    }

    #[test]
    fn test_tensor_type_roundtrip() {
        for code in 0..7 {
            let ty = tensor_type_from_java(code).unwrap();
            let back = tensor_type_to_java(&ty);
            assert_eq!(code, back, "TensorType roundtrip failed for code {code}");
        }
    }

    #[test]
    fn test_invalid_codes() {
        assert!(graph_encoding_from_java(-1).is_err());
        assert!(graph_encoding_from_java(99).is_err());
        assert!(execution_target_from_java(-1).is_err());
        assert!(execution_target_from_java(99).is_err());
        assert!(tensor_type_from_java(-1).is_err());
        assert!(tensor_type_from_java(99).is_err());
    }

    #[test]
    fn test_feature_enabled() {
        assert_eq!(wasmtime4j_nn_is_feature_enabled(), 1);
    }

    #[test]
    fn test_backend_info() {
        let ctx = NnContext::new();
        let names = ctx.backend_names();
        let default = ctx.default_backend_name();
        if !names.is_empty() {
            assert!(default.is_some());
        }
    }
}

#[cfg(all(test, not(feature = "wasi-nn")))]
mod tests_no_feature {
    use super::*;

    #[test]
    fn test_feature_disabled() {
        assert_eq!(wasmtime4j_nn_is_feature_enabled(), 0);
    }
}
