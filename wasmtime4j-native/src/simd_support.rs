//! SIMD Support for WebAssembly
//!
//! This module provides native implementation for WebAssembly SIMD (Single Instruction,
//! Multiple Data) operations, enabling high-performance vector operations on 128-bit vectors.
//! All operations are implemented with defensive programming principles and platform
//! detection to ensure compatibility and prevent JVM crashes.

use std::collections::{HashMap, HashSet};
use std::sync::{Arc, Mutex, RwLock};
use std::ptr;
use std::os::raw::{c_void, c_int, c_char};
use std::ffi::{CStr, CString};

use wasmtime::{Config, Engine, Module};
use crate::error::{WasmtimeResult, WasmtimeError};

/// Maximum number of SIMD configurations to track
const MAX_SIMD_CONFIGURATIONS: usize = 100;

/// SIMD instruction enumeration (matches Java enum)
#[repr(C)]
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum SimdInstruction {
    // Load and Store Operations
    V128Load = 0,
    V128Store = 1,
    V128LoadSplat = 2,
    V128Const = 3,
    V128Load8x8S = 4,
    V128Load8x8U = 5,
    V128Load16x4S = 6,
    V128Load16x4U = 7,
    V128Load32x2S = 8,
    V128Load32x2U = 9,

    // Lane Access Operations
    I8x16ExtractLaneS = 9,
    I8x16ExtractLaneU = 10,
    I8x16ReplaceLane = 11,
    I16x8ExtractLaneS = 12,
    I16x8ExtractLaneU = 13,
    I16x8ReplaceLane = 14,
    I32x4ExtractLane = 15,
    I32x4ReplaceLane = 16,
    I64x2ExtractLane = 17,
    I64x2ReplaceLane = 18,
    F32x4ExtractLane = 19,
    F32x4ReplaceLane = 20,
    F64x2ExtractLane = 21,
    F64x2ReplaceLane = 22,

    // Arithmetic Operations - i8x16
    I8x16Add = 23,
    I8x16Sub = 24,
    I8x16Mul = 25,
    I8x16Neg = 26,
    I8x16AddSatS = 27,
    I8x16AddSatU = 28,
    I8x16SubSatS = 29,
    I8x16SubSatU = 30,

    // Arithmetic Operations - i16x8
    I16x8Add = 31,
    I16x8Sub = 32,
    I16x8Mul = 33,
    I16x8Neg = 34,
    I16x8AddSatS = 35,
    I16x8AddSatU = 36,
    I16x8SubSatS = 37,
    I16x8SubSatU = 38,

    // Arithmetic Operations - i32x4
    I32x4Add = 39,
    I32x4Sub = 40,
    I32x4Mul = 41,
    I32x4Neg = 42,

    // Arithmetic Operations - i64x2
    I64x2Add = 43,
    I64x2Sub = 44,
    I64x2Mul = 45,
    I64x2Neg = 46,

    // Arithmetic Operations - f32x4
    F32x4Add = 47,
    F32x4Sub = 48,
    F32x4Mul = 49,
    F32x4Div = 50,
    F32x4Neg = 51,
    F32x4Sqrt = 52,
    F32x4Abs = 53,
    F32x4Min = 54,
    F32x4Max = 55,

    // Arithmetic Operations - f64x2
    F64x2Add = 56,
    F64x2Sub = 57,
    F64x2Mul = 58,
    F64x2Div = 59,
    F64x2Neg = 60,
    F64x2Sqrt = 61,
    F64x2Abs = 62,
    F64x2Min = 63,
    F64x2Max = 64,

    // Comparison Operations - i8x16
    I8x16Eq = 65,
    I8x16Ne = 66,
    I8x16LtS = 67,
    I8x16LtU = 68,
    I8x16GtS = 69,
    I8x16GtU = 70,
    I8x16LeS = 71,
    I8x16LeU = 72,
    I8x16GeS = 73,
    I8x16GeU = 74,

    // Comparison Operations - i16x8
    I16x8Eq = 75,
    I16x8Ne = 76,
    I16x8LtS = 77,
    I16x8LtU = 78,
    I16x8GtS = 79,
    I16x8GtU = 80,
    I16x8LeS = 81,
    I16x8LeU = 82,
    I16x8GeS = 83,
    I16x8GeU = 84,

    // Comparison Operations - i32x4
    I32x4Eq = 85,
    I32x4Ne = 86,
    I32x4LtS = 87,
    I32x4LtU = 88,
    I32x4GtS = 89,
    I32x4GtU = 90,
    I32x4LeS = 91,
    I32x4LeU = 92,
    I32x4GeS = 93,
    I32x4GeU = 94,

    // Comparison Operations - f32x4
    F32x4Eq = 95,
    F32x4Ne = 96,
    F32x4Lt = 97,
    F32x4Gt = 98,
    F32x4Le = 99,
    F32x4Ge = 100,

    // Comparison Operations - f64x2
    F64x2Eq = 101,
    F64x2Ne = 102,
    F64x2Lt = 103,
    F64x2Gt = 104,
    F64x2Le = 105,
    F64x2Ge = 106,

    // Bitwise Operations
    V128And = 107,
    V128Or = 108,
    V128Xor = 109,
    V128Not = 110,
    V128Andnot = 111,

    // Shuffle and Select Operations
    I8x16Shuffle = 112,
    V128Bitselect = 113,

    // Conversion Operations
    F32x4ConvertI32x4S = 114,
    F32x4ConvertI32x4U = 115,
    I32x4TruncSatF32x4S = 116,
    I32x4TruncSatF32x4U = 117,

    // Splat Operations
    I8x16Splat = 118,
    I16x8Splat = 119,
    I32x4Splat = 120,
    I64x2Splat = 121,
    F32x4Splat = 122,
    F64x2Splat = 123,

    // Shift Operations
    I8x16Shl = 124,
    I8x16ShrS = 125,
    I8x16ShrU = 126,
    I16x8Shl = 127,
    I16x8ShrS = 128,
    I16x8ShrU = 129,
    I32x4Shl = 130,
    I32x4ShrS = 131,
    I32x4ShrU = 132,
    I64x2Shl = 133,
    I64x2ShrS = 134,
    I64x2ShrU = 135,
}

/// SIMD optimization levels
#[repr(C)]
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SimdOptimizationLevel {
    None = 0,
    Basic = 1,
    Aggressive = 2,
    Native = 3,
}

/// SIMD validation result
#[repr(C)]
#[derive(Debug)]
pub struct SimdValidationResult {
    /// Whether the module is valid
    pub is_valid: bool,
    /// Whether the module uses SIMD instructions
    pub uses_simd: bool,
    /// Number of detected SIMD instructions
    pub detected_instruction_count: u32,
    /// Array of detected SIMD instructions
    pub detected_instructions: *mut SimdInstruction,
    /// Number of validation errors
    pub error_count: u32,
    /// Array of error messages
    pub errors: *mut *mut c_char,
}

/// SIMD support manager for tracking SIMD capabilities and configurations
pub struct SimdSupportManager {
    /// Whether SIMD is supported on the current platform
    platform_support: Arc<Mutex<bool>>,
    /// Set of supported SIMD instructions
    supported_instructions: Arc<RwLock<HashSet<SimdInstruction>>>,
    /// Set of enabled SIMD instructions
    enabled_instructions: Arc<RwLock<HashSet<SimdInstruction>>>,
    /// Current optimization level
    optimization_level: Arc<Mutex<SimdOptimizationLevel>>,
    /// Native SIMD feature flags
    native_features: Arc<RwLock<HashSet<String>>>,
    /// SIMD configurations
    configurations: Arc<RwLock<HashMap<u64, SimdConfiguration>>>,
    /// Next configuration ID
    next_config_id: Arc<Mutex<u64>>,
}

/// SIMD configuration
#[derive(Debug, Clone)]
struct SimdConfiguration {
    id: u64,
    enabled_instructions: HashSet<SimdInstruction>,
    optimization_level: SimdOptimizationLevel,
    description: String,
}

impl Default for SimdSupportManager {
    fn default() -> Self {
        Self::new()
    }
}

impl SimdSupportManager {
    /// Create a new SIMD support manager
    pub fn new() -> Self {
        let platform_support = Self::detect_platform_simd_support();
        let supported_instructions = Self::detect_supported_instructions();
        let native_features = Self::detect_native_features();

        Self {
            platform_support: Arc::new(Mutex::new(platform_support)),
            supported_instructions: Arc::new(RwLock::new(supported_instructions.clone())),
            enabled_instructions: Arc::new(RwLock::new(supported_instructions)),
            optimization_level: Arc::new(Mutex::new(SimdOptimizationLevel::Basic)),
            native_features: Arc::new(RwLock::new(native_features)),
            configurations: Arc::new(RwLock::new(HashMap::new())),
            next_config_id: Arc::new(Mutex::new(1)),
        }
    }

    /// Detect platform SIMD support
    fn detect_platform_simd_support() -> bool {
        // Check for common SIMD instruction sets
        #[cfg(any(target_arch = "x86", target_arch = "x86_64"))]
        {
            // Check for SSE2 support (minimum for v128)
            #[cfg(target_feature = "sse2")]
            return true;

            // Runtime detection could be added here
            #[cfg(not(target_feature = "sse2"))]
            return false;
        }

        #[cfg(target_arch = "aarch64")]
        {
            // ARM64 has NEON support
            return true;
        }

        #[cfg(target_arch = "arm")]
        {
            // Check for NEON
            #[cfg(target_feature = "neon")]
            return true;

            #[cfg(not(target_feature = "neon"))]
            return false;
        }

        #[cfg(target_arch = "wasm32")]
        {
            // WebAssembly SIMD is optional
            #[cfg(target_feature = "simd128")]
            return true;

            #[cfg(not(target_feature = "simd128"))]
            return false;
        }

        // Default to false for unknown architectures
        false
    }

    /// Detect supported SIMD instructions
    fn detect_supported_instructions() -> HashSet<SimdInstruction> {
        let mut instructions = HashSet::new();

        if Self::detect_platform_simd_support() {
            // Add basic v128 operations
            instructions.insert(SimdInstruction::V128Load);
            instructions.insert(SimdInstruction::V128Store);
            instructions.insert(SimdInstruction::V128And);
            instructions.insert(SimdInstruction::V128Or);
            instructions.insert(SimdInstruction::V128Xor);
            instructions.insert(SimdInstruction::V128Not);

            // Add splat operations
            instructions.insert(SimdInstruction::I8x16Splat);
            instructions.insert(SimdInstruction::I16x8Splat);
            instructions.insert(SimdInstruction::I32x4Splat);
            instructions.insert(SimdInstruction::I64x2Splat);
            instructions.insert(SimdInstruction::F32x4Splat);
            instructions.insert(SimdInstruction::F64x2Splat);

            // Add arithmetic operations
            instructions.insert(SimdInstruction::I8x16Add);
            instructions.insert(SimdInstruction::I8x16Sub);
            instructions.insert(SimdInstruction::I16x8Add);
            instructions.insert(SimdInstruction::I16x8Sub);
            instructions.insert(SimdInstruction::I32x4Add);
            instructions.insert(SimdInstruction::I32x4Sub);
            instructions.insert(SimdInstruction::I32x4Mul);
            instructions.insert(SimdInstruction::I64x2Add);
            instructions.insert(SimdInstruction::I64x2Sub);

            instructions.insert(SimdInstruction::F32x4Add);
            instructions.insert(SimdInstruction::F32x4Sub);
            instructions.insert(SimdInstruction::F32x4Mul);
            instructions.insert(SimdInstruction::F32x4Div);
            instructions.insert(SimdInstruction::F64x2Add);
            instructions.insert(SimdInstruction::F64x2Sub);
            instructions.insert(SimdInstruction::F64x2Mul);
            instructions.insert(SimdInstruction::F64x2Div);

            // Add comparison operations
            instructions.insert(SimdInstruction::I8x16Eq);
            instructions.insert(SimdInstruction::I16x8Eq);
            instructions.insert(SimdInstruction::I32x4Eq);
            instructions.insert(SimdInstruction::F32x4Eq);
            instructions.insert(SimdInstruction::F64x2Eq);

            // Add shift operations
            instructions.insert(SimdInstruction::I8x16Shl);
            instructions.insert(SimdInstruction::I16x8Shl);
            instructions.insert(SimdInstruction::I32x4Shl);
            instructions.insert(SimdInstruction::I64x2Shl);
        }

        instructions
    }

    /// Detect native SIMD features
    fn detect_native_features() -> HashSet<String> {
        let mut features = HashSet::new();

        #[cfg(any(target_arch = "x86", target_arch = "x86_64"))]
        {
            #[cfg(target_feature = "sse")]
            features.insert("sse".to_string());
            #[cfg(target_feature = "sse2")]
            features.insert("sse2".to_string());
            #[cfg(target_feature = "sse3")]
            features.insert("sse3".to_string());
            #[cfg(target_feature = "ssse3")]
            features.insert("ssse3".to_string());
            #[cfg(target_feature = "sse4.1")]
            features.insert("sse4.1".to_string());
            #[cfg(target_feature = "sse4.2")]
            features.insert("sse4.2".to_string());
            #[cfg(target_feature = "avx")]
            features.insert("avx".to_string());
            #[cfg(target_feature = "avx2")]
            features.insert("avx2".to_string());
            #[cfg(target_feature = "avx512f")]
            features.insert("avx512f".to_string());
        }

        #[cfg(any(target_arch = "arm", target_arch = "aarch64"))]
        {
            #[cfg(target_feature = "neon")]
            features.insert("neon".to_string());
            #[cfg(target_arch = "aarch64")]
            features.insert("asimd".to_string());
        }

        #[cfg(target_arch = "wasm32")]
        {
            #[cfg(target_feature = "simd128")]
            features.insert("simd128".to_string());
        }

        features
    }

    /// Check if SIMD is supported
    pub fn is_simd_supported(&self) -> WasmtimeResult<bool> {
        let platform_support = self.platform_support.lock().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire platform support lock".to_string(),
            }
        })?;
        Ok(*platform_support)
    }

    /// Get supported SIMD instructions
    pub fn get_supported_simd_instructions(&self) -> WasmtimeResult<HashSet<SimdInstruction>> {
        let supported = self.supported_instructions.read().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire supported instructions lock".to_string(),
            }
        })?;
        Ok(supported.clone())
    }

    /// Check if specific vector type is supported
    pub fn supports_v128(&self) -> WasmtimeResult<bool> {
        self.is_simd_supported()
    }

    pub fn supports_i8x16(&self) -> WasmtimeResult<bool> {
        let instructions = self.get_supported_simd_instructions()?;
        Ok(instructions.contains(&SimdInstruction::I8x16Add))
    }

    pub fn supports_i16x8(&self) -> WasmtimeResult<bool> {
        let instructions = self.get_supported_simd_instructions()?;
        Ok(instructions.contains(&SimdInstruction::I16x8Add))
    }

    pub fn supports_i32x4(&self) -> WasmtimeResult<bool> {
        let instructions = self.get_supported_simd_instructions()?;
        Ok(instructions.contains(&SimdInstruction::I32x4Add))
    }

    pub fn supports_i64x2(&self) -> WasmtimeResult<bool> {
        let instructions = self.get_supported_simd_instructions()?;
        Ok(instructions.contains(&SimdInstruction::I64x2Add))
    }

    pub fn supports_f32x4(&self) -> WasmtimeResult<bool> {
        let instructions = self.get_supported_simd_instructions()?;
        Ok(instructions.contains(&SimdInstruction::F32x4Add))
    }

    pub fn supports_f64x2(&self) -> WasmtimeResult<bool> {
        let instructions = self.get_supported_simd_instructions()?;
        Ok(instructions.contains(&SimdInstruction::F64x2Add))
    }

    /// Enable specific SIMD instructions
    pub fn enable_simd_instructions(&self, instructions: HashSet<SimdInstruction>) -> WasmtimeResult<()> {
        let supported = self.supported_instructions.read().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire supported instructions lock".to_string(),
            }
        })?;

        // Check if all requested instructions are supported
        for instruction in &instructions {
            if !supported.contains(instruction) {
                return Err(WasmtimeError::FeatureNotSupported {
                    message: format!("SIMD instruction {:?} is not supported on this platform", instruction),
                });
            }
        }
        drop(supported);

        let mut enabled = self.enabled_instructions.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire enabled instructions lock".to_string(),
            }
        })?;

        enabled.extend(instructions);
        Ok(())
    }

    /// Disable specific SIMD instructions
    pub fn disable_simd_instructions(&self, instructions: HashSet<SimdInstruction>) -> WasmtimeResult<()> {
        let mut enabled = self.enabled_instructions.write().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire enabled instructions lock".to_string(),
            }
        })?;

        for instruction in &instructions {
            enabled.remove(instruction);
        }

        Ok(())
    }

    /// Validate a WebAssembly module for SIMD usage
    pub fn validate_simd_module(&self, wasm_bytes: &[u8]) -> WasmtimeResult<SimdValidationResult> {
        if wasm_bytes.is_empty() {
            return Err(WasmtimeError::InvalidParameter {
                message: "WebAssembly module bytes cannot be empty".to_string(),
            });
        }

        // Create engine with SIMD enabled
        let mut config = Config::new();
        config.wasm_simd(true);
        let engine = Engine::new(&config).map_err(|e| {
            WasmtimeError::EngineCreation {
                message: format!("Failed to create engine: {}", e),
            }
        })?;

        // Validate the module
        let validation_result = Module::validate(&engine, wasm_bytes);
        let is_valid = validation_result.is_ok();

        // Detect SIMD usage (simplified detection)
        let (uses_simd, detected_instructions) = self.detect_simd_usage(wasm_bytes)?;

        // Collect errors if validation failed
        let errors = if let Err(e) = validation_result {
            vec![e.to_string()]
        } else {
            vec![]
        };

        // Convert detected instructions to C-compatible array
        let detected_instruction_count = detected_instructions.len() as u32;
        let detected_instructions_ptr = if detected_instruction_count > 0 {
            let mut instructions_vec = detected_instructions;
            let instructions_ptr = instructions_vec.as_mut_ptr();
            std::mem::forget(instructions_vec);
            instructions_ptr
        } else {
            ptr::null_mut()
        };

        // Convert errors to C-compatible array
        let error_count = errors.len() as u32;
        let errors_ptr = if error_count > 0 {
            let errors_cstr: Result<Vec<_>, _> = errors
                .into_iter()
                .map(|s| CString::new(s))
                .collect();
            let errors_cstr = errors_cstr.map_err(|_| {
                WasmtimeError::InvalidParameter {
                    message: "Invalid error string".to_string(),
                }
            })?;
            let mut errors_ptrs: Vec<*mut c_char> = errors_cstr
                .into_iter()
                .map(|s| s.into_raw())
                .collect();
            let errors_ptr = errors_ptrs.as_mut_ptr();
            std::mem::forget(errors_ptrs);
            errors_ptr
        } else {
            ptr::null_mut()
        };

        Ok(SimdValidationResult {
            is_valid,
            uses_simd,
            detected_instruction_count,
            detected_instructions: detected_instructions_ptr,
            error_count,
            errors: errors_ptr,
        })
    }

    /// Detect SIMD usage in WebAssembly bytecode (simplified)
    fn detect_simd_usage(&self, wasm_bytes: &[u8]) -> WasmtimeResult<(bool, Vec<SimdInstruction>)> {
        let mut uses_simd = false;
        let mut detected_instructions = Vec::new();

        // Look for SIMD instruction prefix (0xFD)
        for (i, &byte) in wasm_bytes.iter().enumerate() {
            if byte == 0xFD && i + 1 < wasm_bytes.len() {
                uses_simd = true;

                // Get the SIMD instruction opcode
                let opcode = wasm_bytes[i + 1];

                // Map opcodes to SIMD instructions (simplified mapping)
                let instruction = match opcode {
                    0x00 => Some(SimdInstruction::V128Load),
                    0x01 => Some(SimdInstruction::V128Store),
                    0x02 => Some(SimdInstruction::V128Const),
                    0x03 => Some(SimdInstruction::I8x16Shuffle),
                    0x04 => Some(SimdInstruction::I8x16Splat),
                    0x05 => Some(SimdInstruction::I16x8Splat),
                    0x06 => Some(SimdInstruction::I32x4Splat),
                    0x07 => Some(SimdInstruction::I64x2Splat),
                    0x08 => Some(SimdInstruction::F32x4Splat),
                    0x09 => Some(SimdInstruction::F64x2Splat),
                    0x0A => Some(SimdInstruction::I8x16ExtractLaneS),
                    0x0B => Some(SimdInstruction::I8x16ExtractLaneU),
                    0x0C => Some(SimdInstruction::I8x16ReplaceLane),
                    // Add more mappings as needed
                    _ => None,
                };

                if let Some(instr) = instruction {
                    if !detected_instructions.contains(&instr) {
                        detected_instructions.push(instr);
                    }
                }
            }
        }

        Ok((uses_simd, detected_instructions))
    }

    /// Get SIMD optimization level
    pub fn get_simd_optimization_level(&self) -> WasmtimeResult<SimdOptimizationLevel> {
        let level = self.optimization_level.lock().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire optimization level lock".to_string(),
            }
        })?;
        Ok(*level)
    }

    /// Set SIMD optimization level
    pub fn set_simd_optimization_level(&self, level: SimdOptimizationLevel) -> WasmtimeResult<()> {
        let mut opt_level = self.optimization_level.lock().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire optimization level lock".to_string(),
            }
        })?;
        *opt_level = level;
        Ok(())
    }

    /// Check if a specific SIMD instruction is supported
    pub fn is_instruction_supported(&self, instruction: SimdInstruction) -> WasmtimeResult<bool> {
        let supported = self.supported_instructions.read().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire supported instructions lock".to_string(),
            }
        })?;
        Ok(supported.contains(&instruction))
    }

    /// Get native SIMD feature flags
    pub fn get_native_simd_features(&self) -> WasmtimeResult<HashSet<String>> {
        let features = self.native_features.read().map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire native features lock".to_string(),
            }
        })?;
        Ok(features.clone())
    }
}

/// Global instance of SIMD support manager
static SIMD_SUPPORT_MANAGER: std::sync::LazyLock<SimdSupportManager> =
    std::sync::LazyLock::new(SimdSupportManager::new);

// C API exports for SIMD support

/// Check if SIMD is supported
///
/// # Safety
///
/// The result parameter must be a valid pointer.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_simd_is_supported(result: *mut bool) -> c_int {
    if result.is_null() {
        return -1;
    }

    let manager = &*SIMD_SUPPORT_MANAGER;
    match manager.is_simd_supported() {
        Ok(supported) => {
            ptr::write(result, supported);
            0
        }
        Err(_) => -1,
    }
}

/// Check if v128 vector type is supported
///
/// # Safety
///
/// The result parameter must be a valid pointer.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_simd_supports_v128(result: *mut bool) -> c_int {
    if result.is_null() {
        return -1;
    }

    let manager = &*SIMD_SUPPORT_MANAGER;
    match manager.supports_v128() {
        Ok(supported) => {
            ptr::write(result, supported);
            0
        }
        Err(_) => -1,
    }
}

/// Check if a specific SIMD instruction is supported
///
/// # Safety
///
/// The result parameter must be a valid pointer.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_simd_is_instruction_supported(
    instruction: SimdInstruction,
    result: *mut bool,
) -> c_int {
    if result.is_null() {
        return -1;
    }

    let manager = &*SIMD_SUPPORT_MANAGER;
    match manager.is_instruction_supported(instruction) {
        Ok(supported) => {
            ptr::write(result, supported);
            0
        }
        Err(_) => -1,
    }
}

/// Validate a WebAssembly module for SIMD usage
///
/// # Safety
///
/// All parameters must be valid pointers and wasm_bytes must point to valid memory.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_simd_validate_module(
    wasm_bytes: *const u8,
    wasm_length: usize,
    result: *mut SimdValidationResult,
) -> c_int {
    if wasm_bytes.is_null() || result.is_null() || wasm_length == 0 {
        return -1;
    }

    let manager = &*SIMD_SUPPORT_MANAGER;
    let bytes_slice = std::slice::from_raw_parts(wasm_bytes, wasm_length);

    match manager.validate_simd_module(bytes_slice) {
        Ok(validation_result) => {
            ptr::write(result, validation_result);
            0
        }
        Err(_) => -1,
    }
}

/// Get SIMD optimization level
///
/// # Safety
///
/// The level parameter must be a valid pointer.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_simd_get_optimization_level(
    level: *mut SimdOptimizationLevel,
) -> c_int {
    if level.is_null() {
        return -1;
    }

    let manager = &*SIMD_SUPPORT_MANAGER;
    match manager.get_simd_optimization_level() {
        Ok(opt_level) => {
            ptr::write(level, opt_level);
            0
        }
        Err(_) => -1,
    }
}

/// Set SIMD optimization level
///
/// # Safety
///
/// This function is safe to call with any valid optimization level.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_simd_set_optimization_level(
    level: SimdOptimizationLevel,
) -> c_int {
    let manager = &*SIMD_SUPPORT_MANAGER;
    match manager.set_simd_optimization_level(level) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_simd_support_manager_creation() {
        let manager = SimdSupportManager::new();
        // SIMD support depends on platform capabilities
        let _ = manager.is_simd_supported().unwrap();
    }

    #[test]
    fn test_simd_instruction_detection() {
        let manager = SimdSupportManager::new();
        let supported = manager.get_supported_simd_instructions().unwrap();

        if manager.is_simd_supported().unwrap() {
            assert!(!supported.is_empty());
            assert!(supported.contains(&SimdInstruction::V128Load));
        }
    }

    #[test]
    fn test_vector_type_support() {
        let manager = SimdSupportManager::new();

        if manager.is_simd_supported().unwrap() {
            assert!(manager.supports_v128().unwrap());
            // Other vector types depend on specific instruction support
        }
    }

    #[test]
    fn test_optimization_level() {
        let manager = SimdSupportManager::new();

        let initial_level = manager.get_simd_optimization_level().unwrap();
        assert_eq!(initial_level, SimdOptimizationLevel::Basic);

        manager.set_simd_optimization_level(SimdOptimizationLevel::Aggressive).unwrap();
        let new_level = manager.get_simd_optimization_level().unwrap();
        assert_eq!(new_level, SimdOptimizationLevel::Aggressive);
    }

    #[test]
    fn test_native_features() {
        let manager = SimdSupportManager::new();
        let features = manager.get_native_simd_features().unwrap();

        // Features depend on the compilation target
        #[cfg(target_feature = "sse2")]
        assert!(features.contains("sse2"));

        #[cfg(target_feature = "neon")]
        assert!(features.contains("neon"));
    }

    #[test]
    fn test_simd_bytecode_detection() {
        let manager = SimdSupportManager::new();

        // Test with SIMD instruction prefix
        let simd_bytes = vec![0xFD, 0x00, 0x41, 0x00]; // v128.load instruction
        let (uses_simd, instructions) = manager.detect_simd_usage(&simd_bytes).unwrap();

        assert!(uses_simd);
        assert!(!instructions.is_empty());
        assert!(instructions.contains(&SimdInstruction::V128Load));
    }

    #[test]
    fn test_instruction_enable_disable() {
        let manager = SimdSupportManager::new();

        if manager.is_simd_supported().unwrap() {
            let mut instructions_to_disable = HashSet::new();
            instructions_to_disable.insert(SimdInstruction::V128Load);

            manager.disable_simd_instructions(instructions_to_disable.clone()).unwrap();

            let mut instructions_to_enable = HashSet::new();
            instructions_to_enable.insert(SimdInstruction::V128Load);

            manager.enable_simd_instructions(instructions_to_enable).unwrap();
        }
    }

    #[test]
    fn test_c_api_safety() {
        unsafe {
            // Test null pointer handling
            let mut result = false;
            assert_eq!(wasmtime4j_simd_is_supported(&mut result), 0);

            assert_eq!(wasmtime4j_simd_is_supported(ptr::null_mut()), -1);

            let mut supports_v128 = false;
            assert_eq!(wasmtime4j_simd_supports_v128(&mut supports_v128), 0);

            let mut instruction_supported = false;
            assert_eq!(
                wasmtime4j_simd_is_instruction_supported(
                    SimdInstruction::V128Load,
                    &mut instruction_supported
                ),
                0
            );

            let mut level = SimdOptimizationLevel::None;
            assert_eq!(wasmtime4j_simd_get_optimization_level(&mut level), 0);

            assert_eq!(
                wasmtime4j_simd_set_optimization_level(SimdOptimizationLevel::Native),
                0
            );
        }
    }
}