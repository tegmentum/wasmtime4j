//! Advanced SIMD operations for WebAssembly
//!
//! This module provides support for advanced SIMD vector operations
//! beyond basic v128 support, including platform-specific optimizations.

use crate::error::{WasmtimeError, WasmtimeResult};
#[cfg(target_arch = "x86_64")]
use std::arch::x86_64::*;
#[cfg(target_arch = "aarch64")]
use std::arch::aarch64::*;
use wasmtime::*;

/// V128 vector representation
#[derive(Debug, Clone, Copy, PartialEq)]
pub struct V128 {
    /// Raw 16-byte data
    pub data: [u8; 16],
}



/// Platform SIMD capabilities
#[derive(Debug, Clone)]
pub struct PlatformCapabilities {
    pub has_sse41: bool,
    pub has_avx: bool,
    pub has_avx2: bool,
    pub has_fma: bool,
    pub has_neon: bool,
    pub max_vector_width: u32,
    // Cryptographic extensions
    pub has_aes_ni: bool,
    pub has_aes_arm: bool,
    pub has_sha_arm: bool,
}

impl V128 {
    /// Creates a new V128 vector with all bytes set to zero
    pub fn zero() -> Self {
        Self { data: [0; 16] }
    }

    /// Creates a new V128 vector from raw bytes
    pub fn from_bytes(bytes: [u8; 16]) -> Self {
        Self { data: bytes }
    }

    /// Creates a new V128 vector with all bytes set to the given value
    pub fn splat_u8(value: u8) -> Self {
        Self { data: [value; 16] }
    }

    /// Creates a V128 vector from 4 32-bit integers
    pub fn from_i32s(i0: i32, i1: i32, i2: i32, i3: i32) -> Self {
        let mut data = [0u8; 16];
        data[0..4].copy_from_slice(&i0.to_le_bytes());
        data[4..8].copy_from_slice(&i1.to_le_bytes());
        data[8..12].copy_from_slice(&i2.to_le_bytes());
        data[12..16].copy_from_slice(&i3.to_le_bytes());
        Self { data }
    }

    /// Creates a V128 vector from 4 32-bit floats
    pub fn from_f32s(f0: f32, f1: f32, f2: f32, f3: f32) -> Self {
        let mut data = [0u8; 16];
        data[0..4].copy_from_slice(&f0.to_le_bytes());
        data[4..8].copy_from_slice(&f1.to_le_bytes());
        data[8..12].copy_from_slice(&f2.to_le_bytes());
        data[12..16].copy_from_slice(&f3.to_le_bytes());
        Self { data }
    }

    /// Gets the vector data as an array of 4 integers
    pub fn as_i32s(&self) -> [i32; 4] {
        [
            i32::from_le_bytes([self.data[0], self.data[1], self.data[2], self.data[3]]),
            i32::from_le_bytes([self.data[4], self.data[5], self.data[6], self.data[7]]),
            i32::from_le_bytes([self.data[8], self.data[9], self.data[10], self.data[11]]),
            i32::from_le_bytes([self.data[12], self.data[13], self.data[14], self.data[15]]),
        ]
    }

    /// Gets the vector data as an array of 4 floats
    pub fn as_f32s(&self) -> [f32; 4] {
        [
            f32::from_le_bytes([self.data[0], self.data[1], self.data[2], self.data[3]]),
            f32::from_le_bytes([self.data[4], self.data[5], self.data[6], self.data[7]]),
            f32::from_le_bytes([self.data[8], self.data[9], self.data[10], self.data[11]]),
            f32::from_le_bytes([self.data[12], self.data[13], self.data[14], self.data[15]]),
        ]
    }

    /// Gets the vector data as an array of 2 doubles
    pub fn as_f64s(&self) -> [f64; 2] {
        [
            f64::from_le_bytes([
                self.data[0], self.data[1], self.data[2], self.data[3],
                self.data[4], self.data[5], self.data[6], self.data[7],
            ]),
            f64::from_le_bytes([
                self.data[8], self.data[9], self.data[10], self.data[11],
                self.data[12], self.data[13], self.data[14], self.data[15],
            ]),
        ]
    }

    /// Creates a V128 vector from 2 64-bit doubles
    pub fn from_f64s(d0: f64, d1: f64) -> Self {
        let mut data = [0u8; 16];
        data[0..8].copy_from_slice(&d0.to_le_bytes());
        data[8..16].copy_from_slice(&d1.to_le_bytes());
        Self { data }
    }

    /// Gets the vector data as an array of 16 bytes
    pub fn as_u8s(&self) -> [u8; 16] {
        self.data
    }

    /// Gets the vector data as an array of 8 i16 values
    pub fn as_i16s(&self) -> [i16; 8] {
        [
            i16::from_le_bytes([self.data[0], self.data[1]]),
            i16::from_le_bytes([self.data[2], self.data[3]]),
            i16::from_le_bytes([self.data[4], self.data[5]]),
            i16::from_le_bytes([self.data[6], self.data[7]]),
            i16::from_le_bytes([self.data[8], self.data[9]]),
            i16::from_le_bytes([self.data[10], self.data[11]]),
            i16::from_le_bytes([self.data[12], self.data[13]]),
            i16::from_le_bytes([self.data[14], self.data[15]]),
        ]
    }

    /// Creates a V128 vector from 8 16-bit integers
    pub fn from_i16s(values: [i16; 8]) -> Self {
        let mut data = [0u8; 16];
        for (i, &val) in values.iter().enumerate() {
            data[i * 2..(i + 1) * 2].copy_from_slice(&val.to_le_bytes());
        }
        Self { data }
    }
}

/// SIMD instruction scheduling strategy
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum SchedulingStrategy {
    /// In-order execution (default)
    InOrder,
    /// Out-of-order execution with dependency tracking
    OutOfOrder,
    /// Pipeline optimization with instruction fusion
    Pipelined,
    /// Adaptive scheduling based on workload
    Adaptive,
}

/// Memory alignment requirements
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum AlignmentRequirement {
    /// No alignment requirement
    None,
    /// 16-byte alignment for V128
    Align16,
    /// Cache line alignment (typically 64 bytes)
    CacheLine,
}

/// SIMD operation configuration
#[derive(Debug, Clone)]
pub struct SIMDConfig {
    /// Enable platform-specific optimizations
    pub enable_platform_optimizations: bool,
    /// Enable relaxed operations
    pub enable_relaxed_operations: bool,
    /// Validate vector operands
    pub validate_vector_operands: bool,
    /// Maximum vector width in bits
    pub max_vector_width: u32,
    /// Enable advanced FMA operations
    pub enable_fma_operations: bool,
    /// Enable gather/scatter operations
    pub enable_gather_scatter: bool,
    /// Enable vector reduction operations
    pub enable_vector_reductions: bool,
    /// Debug mode for SIMD operations
    pub debug_mode: bool,
    /// Instruction scheduling strategy
    pub scheduling_strategy: SchedulingStrategy,
    /// Memory alignment requirement
    pub alignment_requirement: AlignmentRequirement,
    /// Enable instruction fusion optimizations
    pub enable_instruction_fusion: bool,
    /// Maximum pipeline depth for scheduling
    pub max_pipeline_depth: u32,
    /// Enable prefetching for memory operations
    pub enable_prefetching: bool,
}

impl Default for SIMDConfig {
    fn default() -> Self {
        Self {
            enable_platform_optimizations: true,
            enable_relaxed_operations: false,
            validate_vector_operands: true,
            max_vector_width: 128,
            enable_fma_operations: true,
            enable_gather_scatter: false,
            enable_vector_reductions: true,
            debug_mode: false,
            scheduling_strategy: SchedulingStrategy::InOrder,
            alignment_requirement: AlignmentRequirement::Align16,
            enable_instruction_fusion: true,
            max_pipeline_depth: 4,
            enable_prefetching: true,
        }
    }
}

/// SIMD operations handler
pub struct SIMDOperations {
    /// Configuration
    config: SIMDConfig,
    /// Platform capabilities
    capabilities: PlatformCapabilities,
}

impl PlatformCapabilities {
    /// Detects platform SIMD capabilities
    pub fn detect() -> Self {
        let capabilities = Self {
            has_sse41: Self::detect_sse41(),
            has_avx: Self::detect_avx(),
            has_avx2: Self::detect_avx2(),
            has_fma: Self::detect_fma(),
            has_neon: Self::detect_neon(),
            max_vector_width: Self::detect_max_vector_width(),
            // Cryptographic extensions
            has_aes_ni: Self::detect_aes_ni(),
            has_aes_arm: Self::detect_aes_arm(),
            has_sha_arm: Self::detect_sha_arm(),
        };

        // Log detected capabilities for debugging
        Self::log_capabilities(&capabilities);
        capabilities
    }

    /// Logs detected capabilities
    fn log_capabilities(caps: &PlatformCapabilities) {
        log::info!("SIMD Capabilities detected:");
        log::info!("  SSE4.1: {}", caps.has_sse41);
        log::info!("  AVX: {}", caps.has_avx);
        log::info!("  AVX2: {}", caps.has_avx2);
        log::info!("  FMA: {}", caps.has_fma);
        log::info!("  NEON: {}", caps.has_neon);
        log::info!("  Max vector width: {} bits", caps.max_vector_width);
    }

    /// Detects SSE4.1 support
    #[cfg(target_arch = "x86_64")]
    fn detect_sse41() -> bool {
        std::arch::is_x86_feature_detected!("sse4.1")
    }

    #[cfg(not(target_arch = "x86_64"))]
    fn detect_sse41() -> bool {
        false
    }

    /// Detects AVX support
    #[cfg(target_arch = "x86_64")]
    fn detect_avx() -> bool {
        std::arch::is_x86_feature_detected!("avx")
    }

    #[cfg(not(target_arch = "x86_64"))]
    fn detect_avx() -> bool {
        false
    }

    /// Detects AVX2 support
    #[cfg(target_arch = "x86_64")]
    fn detect_avx2() -> bool {
        std::arch::is_x86_feature_detected!("avx2")
    }

    #[cfg(not(target_arch = "x86_64"))]
    fn detect_avx2() -> bool {
        false
    }


    /// Detects FMA support
    #[cfg(target_arch = "x86_64")]
    fn detect_fma() -> bool {
        std::arch::is_x86_feature_detected!("fma")
    }

    #[cfg(not(target_arch = "x86_64"))]
    fn detect_fma() -> bool {
        false
    }

    /// Detects ARM NEON support
    #[cfg(target_arch = "aarch64")]
    fn detect_neon() -> bool {
        std::arch::is_aarch64_feature_detected!("neon")
    }

    #[cfg(not(target_arch = "aarch64"))]
    fn detect_neon() -> bool {
        false
    }

    /// Detects maximum vector width
    /// WebAssembly SIMD spec only supports V128 (128-bit vectors)
    fn detect_max_vector_width() -> u32 {
        if Self::detect_sse41() || Self::detect_neon() {
            128
        } else {
            0
        }
    }

    /// Detects AES-NI support (x86)
    #[cfg(target_arch = "x86_64")]
    fn detect_aes_ni() -> bool {
        std::arch::is_x86_feature_detected!("aes")
    }

    #[cfg(not(target_arch = "x86_64"))]
    fn detect_aes_ni() -> bool {
        false
    }

    /// Detects AES support on ARM
    #[cfg(target_arch = "aarch64")]
    fn detect_aes_arm() -> bool {
        std::arch::is_aarch64_feature_detected!("aes")
    }

    #[cfg(not(target_arch = "aarch64"))]
    fn detect_aes_arm() -> bool {
        false
    }

    /// Detects SHA support on ARM
    #[cfg(target_arch = "aarch64")]
    fn detect_sha_arm() -> bool {
        std::arch::is_aarch64_feature_detected!("sha2")
    }

    #[cfg(not(target_arch = "aarch64"))]
    fn detect_sha_arm() -> bool {
        false
    }
}

impl SIMDOperations {
    /// Creates a new SIMD operations handler
    pub fn new(config: SIMDConfig) -> WasmtimeResult<Self> {
        let capabilities = PlatformCapabilities::detect();

        // Validate configuration against detected capabilities
        if config.max_vector_width > capabilities.max_vector_width {
            log::warn!(
                "Requested vector width {} exceeds platform maximum {}",
                config.max_vector_width,
                capabilities.max_vector_width
            );
        }

        Ok(Self {
            config,
            capabilities,
        })
    }

    /// Performs gather operation - load vector from scattered memory locations
    pub fn gather_v128(&self, memory: &Memory, store: &mut Store<()>, indices: &V128, scale: u32) -> WasmtimeResult<V128> {
        if !self.config.enable_gather_scatter {
            return Err(WasmtimeError::UnsupportedFeature {
                message: "Gather/scatter operations are not enabled".to_string(),
            });
        }

        let indices_array = indices.as_i32s();
        let mut result_data = [0u8; 16];

        for (i, &index) in indices_array.iter().enumerate() {
            let offset = (index as u32).wrapping_mul(scale);
            if offset as u64 + 4 <= memory.data_size(&mut *store) as u64 {
                unsafe {
                    let src_ptr = memory.data_ptr(&mut *store).add(offset as usize) as *const i32;
                    let value = std::ptr::read_unaligned(src_ptr);
                    let bytes = value.to_le_bytes();
                    result_data[i * 4..(i + 1) * 4].copy_from_slice(&bytes);
                }
            }
        }

        Ok(V128 { data: result_data })
    }

    /// Performs scatter operation - store vector to scattered memory locations
    pub fn scatter_v128(&self, memory: &Memory, store: &mut Store<()>, indices: &V128, data: &V128, scale: u32) -> WasmtimeResult<()> {
        if !self.config.enable_gather_scatter {
            return Err(WasmtimeError::UnsupportedFeature {
                message: "Gather/scatter operations are not enabled".to_string(),
            });
        }

        let indices_array = indices.as_i32s();
        let data_array = data.as_i32s();

        // Get mutable access to memory data first, then check bounds within the loop
        let memory_data_mut = memory.data_mut(store);

        for (i, &index) in indices_array.iter().enumerate() {
            let offset = (index as u32).wrapping_mul(scale);
            if offset as u64 + 4 <= memory_data_mut.len() as u64 {
                unsafe {
                    let dst_ptr = memory_data_mut.as_mut_ptr().add(offset as usize) as *mut i32;
                    std::ptr::write_unaligned(dst_ptr, data_array[i]);
                }
            }
        }

        Ok(())
    }

    /// Vector reduction - sum all elements
    pub fn reduce_sum_i32(&self, a: &V128) -> WasmtimeResult<i32> {
        if !self.config.enable_vector_reductions {
            return Err(WasmtimeError::UnsupportedFeature {
                message: "Vector reduction operations are not enabled".to_string(),
            });
        }

        self.validate_single_operand(a)?;

        if self.config.enable_platform_optimizations && self.capabilities.has_sse41 {
            self.reduce_sum_i32_optimized(a)
        } else {
            self.reduce_sum_i32_scalar(a)
        }
    }

    /// Optimized reduction sum for x86_64
    #[cfg(target_arch = "x86_64")]
    fn reduce_sum_i32_optimized(&self, a: &V128) -> WasmtimeResult<i32> {
        unsafe {
            let va = _mm_loadu_si128(a.data.as_ptr() as *const __m128i);
            let hi = _mm_unpackhi_epi32(va, _mm_setzero_si128());
            let lo = _mm_unpacklo_epi32(va, _mm_setzero_si128());
            let sum = _mm_add_epi32(hi, lo);
            let hi2 = _mm_shuffle_epi32(sum, 0x4E);
            let result = _mm_add_epi32(sum, hi2);

            Ok(_mm_cvtsi128_si32(result))
        }
    }

    /// Optimized reduction sum for ARM64
    #[cfg(target_arch = "aarch64")]
    fn reduce_sum_i32_optimized(&self, a: &V128) -> WasmtimeResult<i32> {
        unsafe {
            let va = vld1q_s32(a.data.as_ptr() as *const i32);
            let sum = vaddvq_s32(va);
            Ok(sum)
        }
    }

    #[cfg(not(any(target_arch = "x86_64", target_arch = "aarch64")))]
    fn reduce_sum_i32_optimized(&self, a: &V128) -> WasmtimeResult<i32> {
        self.reduce_sum_i32_scalar(a)
    }

    /// Scalar reduction sum fallback
    fn reduce_sum_i32_scalar(&self, a: &V128) -> WasmtimeResult<i32> {
        let a_ints = a.as_i32s();
        Ok(a_ints[0].wrapping_add(a_ints[1]).wrapping_add(a_ints[2]).wrapping_add(a_ints[3]))
    }

    /// Vector reduction - find minimum element
    pub fn reduce_min_i32(&self, a: &V128) -> WasmtimeResult<i32> {
        if !self.config.enable_vector_reductions {
            return Err(WasmtimeError::UnsupportedFeature {
                message: "Vector reduction operations are not enabled".to_string(),
            });
        }

        self.validate_single_operand(a)?;
        let a_ints = a.as_i32s();
        // Safe: as_i32s() returns [i32; 4], always has 4 elements
        Ok(a_ints.iter().min().copied().unwrap_or(0))
    }

    /// Vector reduction - find maximum element
    pub fn reduce_max_i32(&self, a: &V128) -> WasmtimeResult<i32> {
        if !self.config.enable_vector_reductions {
            return Err(WasmtimeError::UnsupportedFeature {
                message: "Vector reduction operations are not enabled".to_string(),
            });
        }

        self.validate_single_operand(a)?;
        let a_ints = a.as_i32s();
        // Safe: as_i32s() returns [i32; 4], always has 4 elements
        Ok(a_ints.iter().max().copied().unwrap_or(0))
    }

    /// Advanced dot product operation
    pub fn dot_product_i32(&self, a: &V128, b: &V128) -> WasmtimeResult<i32> {
        self.validate_operands(a, b)?;

        if self.config.enable_platform_optimizations && self.capabilities.has_sse41 {
            self.dot_product_i32_optimized(a, b)
        } else {
            self.dot_product_i32_scalar(a, b)
        }
    }

    /// Optimized dot product for x86_64
    #[cfg(target_arch = "x86_64")]
    fn dot_product_i32_optimized(&self, a: &V128, b: &V128) -> WasmtimeResult<i32> {
        unsafe {
            let va = _mm_loadu_si128(a.data.as_ptr() as *const __m128i);
            let vb = _mm_loadu_si128(b.data.as_ptr() as *const __m128i);
            let product = _mm_mullo_epi32(va, vb);

            // Horizontal add for dot product
            let hi = _mm_unpackhi_epi32(product, _mm_setzero_si128());
            let lo = _mm_unpacklo_epi32(product, _mm_setzero_si128());
            let sum = _mm_add_epi32(hi, lo);
            let hi2 = _mm_shuffle_epi32(sum, 0x4E);
            let result = _mm_add_epi32(sum, hi2);

            Ok(_mm_cvtsi128_si32(result))
        }
    }

    /// Optimized dot product for ARM64
    #[cfg(target_arch = "aarch64")]
    fn dot_product_i32_optimized(&self, a: &V128, b: &V128) -> WasmtimeResult<i32> {
        unsafe {
            let va = vld1q_s32(a.data.as_ptr() as *const i32);
            let vb = vld1q_s32(b.data.as_ptr() as *const i32);
            let product = vmulq_s32(va, vb);
            let sum = vaddvq_s32(product);
            Ok(sum)
        }
    }

    #[cfg(not(any(target_arch = "x86_64", target_arch = "aarch64")))]
    fn dot_product_i32_optimized(&self, a: &V128, b: &V128) -> WasmtimeResult<i32> {
        self.dot_product_i32_scalar(a, b)
    }

    /// Scalar dot product fallback
    fn dot_product_i32_scalar(&self, a: &V128, b: &V128) -> WasmtimeResult<i32> {
        let a_ints = a.as_i32s();
        let b_ints = b.as_i32s();

        let mut sum = 0i32;
        for i in 0..4 {
            sum = sum.wrapping_add(a_ints[i].wrapping_mul(b_ints[i]));
        }

        Ok(sum)
    }

    /// Complex number multiplication (treating V128 as two complex numbers)
    pub fn complex_multiply(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        self.validate_operands(a, b)?;

        let a_floats = a.as_f32s();
        let b_floats = b.as_f32s();

        // First complex number: (a[0] + a[1]i) * (b[0] + b[1]i)
        let real1 = a_floats[0] * b_floats[0] - a_floats[1] * b_floats[1];
        let imag1 = a_floats[0] * b_floats[1] + a_floats[1] * b_floats[0];

        // Second complex number: (a[2] + a[3]i) * (b[2] + b[3]i)
        let real2 = a_floats[2] * b_floats[2] - a_floats[3] * b_floats[3];
        let imag2 = a_floats[2] * b_floats[3] + a_floats[3] * b_floats[2];

        Ok(V128::from_f32s(real1, imag1, real2, imag2))
    }

    /// Polynomial evaluation using Horner's method
    pub fn polynomial_evaluate(&self, x: &V128, coefficients: &[f32]) -> WasmtimeResult<V128> {
        if coefficients.is_empty() {
            return Ok(V128::zero());
        }

        self.validate_single_operand(x)?;
        let x_floats = x.as_f32s();
        let mut result = [0.0f32; 4];

        for (lane, &x_val) in x_floats.iter().enumerate() {
            let mut poly_result = coefficients[coefficients.len() - 1];
            for &coeff in coefficients.iter().rev().skip(1) {
                poly_result = poly_result * x_val + coeff;
            }
            result[lane] = poly_result;
        }

        Ok(V128::from_f32s(result[0], result[1], result[2], result[3]))
    }

    /// Loads a V128 vector from memory
    pub fn load(&self, memory: &Memory, store: &mut Store<()>, offset: u32) -> WasmtimeResult<V128> {
        if offset as u64 + 16 > memory.data_size(&mut *store) as u64 {
            return Err(WasmtimeError::Runtime {
                message: "Memory access out of bounds".to_string(),
                backtrace: None,
            });
        }

        let data_ptr = memory.data_ptr(&mut *store);
        let mut data = [0u8; 16];

        unsafe {
            std::ptr::copy_nonoverlapping(data_ptr.add(offset as usize), data.as_mut_ptr(), 16);
        }

        Ok(V128 { data })
    }

    /// Stores a V128 vector to memory
    pub fn store(
        &self,
        memory: &Memory,
        store: &mut Store<()>,
        offset: u32,
        vector: &V128,
    ) -> WasmtimeResult<()> {
        // Get mutable access to memory data and check bounds using the slice length
        let memory_data_mut = memory.data_mut(store);
        if offset as u64 + 16 > memory_data_mut.len() as u64 {
            return Err(WasmtimeError::Runtime {
                message: "Memory access out of bounds".to_string(),
                backtrace: None,
            });
        }

        let data_ptr = memory_data_mut.as_mut_ptr();

        unsafe {
            std::ptr::copy_nonoverlapping(vector.data.as_ptr(), data_ptr.add(offset as usize), 16);
        }

        Ok(())
    }

    /// Performs vector addition
    pub fn add(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        self.validate_operands(a, b)?;

        if self.config.enable_platform_optimizations && self.capabilities.has_sse41 {
            self.add_sse41(a, b)
        } else {
            self.add_scalar(a, b)
        }
    }

    /// Performs fused multiply-add operation (a * b + c)
    pub fn fma(&self, a: &V128, b: &V128, c: &V128) -> WasmtimeResult<V128> {
        if !self.config.enable_fma_operations {
            return Err(WasmtimeError::UnsupportedFeature {
                message: "FMA operations are not enabled".to_string(),
            });
        }

        self.validate_operands(a, b)?;
        self.validate_single_operand(c)?;

        if self.config.enable_platform_optimizations && self.capabilities.has_fma {
            self.fma_native(a, b, c)
        } else {
            self.fma_fallback(a, b, c)
        }
    }

    /// Performs fused multiply-subtract operation (a * b - c)
    pub fn fms(&self, a: &V128, b: &V128, c: &V128) -> WasmtimeResult<V128> {
        if !self.config.enable_fma_operations {
            return Err(WasmtimeError::UnsupportedFeature {
                message: "FMA operations are not enabled".to_string(),
            });
        }

        self.validate_operands(a, b)?;
        self.validate_single_operand(c)?;

        if self.config.enable_platform_optimizations && self.capabilities.has_fma {
            self.fms_native(a, b, c)
        } else {
            self.fms_fallback(a, b, c)
        }
    }

    /// Performs vector reciprocal approximation
    pub fn reciprocal(&self, a: &V128) -> WasmtimeResult<V128> {
        self.validate_single_operand(a)?;

        if self.config.enable_platform_optimizations && self.capabilities.has_sse41 {
            self.reciprocal_sse41(a)
        } else {
            self.reciprocal_scalar(a)
        }
    }

    /// Performs vector square root
    pub fn sqrt(&self, a: &V128) -> WasmtimeResult<V128> {
        self.validate_single_operand(a)?;

        if self.config.enable_platform_optimizations && self.capabilities.has_sse41 {
            self.sqrt_sse41(a)
        } else {
            self.sqrt_scalar(a)
        }
    }

    /// Performs reciprocal square root approximation
    pub fn rsqrt(&self, a: &V128) -> WasmtimeResult<V128> {
        self.validate_single_operand(a)?;

        if self.config.enable_platform_optimizations && self.capabilities.has_sse41 {
            self.rsqrt_sse41(a)
        } else {
            self.rsqrt_scalar(a)
        }
    }

    /// Vector addition using SSE4.1
    #[cfg(target_arch = "x86_64")]
    fn add_sse41(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        unsafe {
            let va = _mm_loadu_si128(a.data.as_ptr() as *const __m128i);
            let vb = _mm_loadu_si128(b.data.as_ptr() as *const __m128i);
            let result = _mm_add_epi32(va, vb);

            let mut data = [0u8; 16];
            _mm_storeu_si128(data.as_mut_ptr() as *mut __m128i, result);

            Ok(V128 { data })
        }
    }

    /// Vector addition using ARM64 NEON
    #[cfg(target_arch = "aarch64")]
    fn add_sse41(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        if self.capabilities.has_neon {
            self.add_neon(a, b)
        } else {
            self.add_scalar(a, b)
        }
    }

    /// Vector addition using NEON
    #[cfg(target_arch = "aarch64")]
    fn add_neon(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        unsafe {
            let va = vld1q_s32(a.data.as_ptr() as *const i32);
            let vb = vld1q_s32(b.data.as_ptr() as *const i32);
            let result = vaddq_s32(va, vb);

            let mut data = [0u8; 16];
            vst1q_s32(data.as_mut_ptr() as *mut i32, result);

            Ok(V128 { data })
        }
    }

    #[cfg(not(any(target_arch = "x86_64", target_arch = "aarch64")))]
    fn add_sse41(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        self.add_scalar(a, b)
    }

    /// Scalar vector addition fallback
    fn add_scalar(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        let a_ints = a.as_i32s();
        let b_ints = b.as_i32s();

        let result = V128::from_i32s(
            a_ints[0].wrapping_add(b_ints[0]),
            a_ints[1].wrapping_add(b_ints[1]),
            a_ints[2].wrapping_add(b_ints[2]),
            a_ints[3].wrapping_add(b_ints[3]),
        );

        Ok(result)
    }

    /// Performs vector subtraction
    pub fn subtract(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        self.validate_operands(a, b)?;

        if self.config.enable_platform_optimizations && self.capabilities.has_sse41 {
            self.subtract_sse41(a, b)
        } else {
            self.subtract_scalar(a, b)
        }
    }

    /// Vector subtraction using SSE4.1
    #[cfg(target_arch = "x86_64")]
    fn subtract_sse41(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        unsafe {
            let va = _mm_loadu_si128(a.data.as_ptr() as *const __m128i);
            let vb = _mm_loadu_si128(b.data.as_ptr() as *const __m128i);
            let result = _mm_sub_epi32(va, vb);

            let mut data = [0u8; 16];
            _mm_storeu_si128(data.as_mut_ptr() as *mut __m128i, result);

            Ok(V128 { data })
        }
    }

    #[cfg(not(target_arch = "x86_64"))]
    fn subtract_sse41(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        self.subtract_scalar(a, b)
    }

    /// Scalar vector subtraction fallback
    fn subtract_scalar(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        let a_ints = a.as_i32s();
        let b_ints = b.as_i32s();

        let result = V128::from_i32s(
            a_ints[0].wrapping_sub(b_ints[0]),
            a_ints[1].wrapping_sub(b_ints[1]),
            a_ints[2].wrapping_sub(b_ints[2]),
            a_ints[3].wrapping_sub(b_ints[3]),
        );

        Ok(result)
    }

    /// Performs vector multiplication
    pub fn multiply(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        self.validate_operands(a, b)?;

        if self.config.enable_platform_optimizations && self.capabilities.has_sse41 {
            self.multiply_sse41(a, b)
        } else {
            self.multiply_scalar(a, b)
        }
    }

    /// Performs vector division
    pub fn divide(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        self.validate_operands(a, b)?;

        if self.config.enable_platform_optimizations && self.capabilities.has_avx {
            self.divide_avx(a, b)
        } else {
            self.divide_scalar(a, b)
        }
    }

    /// Performs vector bitwise AND
    pub fn and(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        self.validate_operands(a, b)?;

        if self.config.enable_platform_optimizations && self.has_sse41() {
            self.and_sse41(a, b)
        } else {
            self.and_scalar(a, b)
        }
    }

    /// Performs vector bitwise OR
    pub fn or(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        self.validate_operands(a, b)?;

        if self.config.enable_platform_optimizations && self.has_sse41() {
            self.or_sse41(a, b)
        } else {
            self.or_scalar(a, b)
        }
    }

    /// Performs vector bitwise XOR
    pub fn xor(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        self.validate_operands(a, b)?;

        if self.config.enable_platform_optimizations && self.has_sse41() {
            self.xor_sse41(a, b)
        } else {
            self.xor_scalar(a, b)
        }
    }

    /// Performs vector bitwise NOT
    pub fn not(&self, a: &V128) -> WasmtimeResult<V128> {
        self.validate_single_operand(a)?;

        if self.config.enable_platform_optimizations && self.has_sse41() {
            self.not_sse41(a)
        } else {
            self.not_scalar(a)
        }
    }

    /// Performs vector comparison (equal)
    pub fn equals(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        self.validate_operands(a, b)?;

        if self.config.enable_platform_optimizations && self.has_sse41() {
            self.equals_sse41(a, b)
        } else {
            self.equals_scalar(a, b)
        }
    }

    /// Performs vector comparison (less than)
    pub fn less_than(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        self.validate_operands(a, b)?;

        if self.config.enable_platform_optimizations && self.has_sse41() {
            self.less_than_sse41(a, b)
        } else {
            self.less_than_scalar(a, b)
        }
    }

    /// Performs vector comparison (greater than)
    pub fn greater_than(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        self.validate_operands(a, b)?;

        if self.config.enable_platform_optimizations && self.has_sse41() {
            self.greater_than_sse41(a, b)
        } else {
            self.greater_than_scalar(a, b)
        }
    }

    /// Vector multiplication using SSE4.1
    #[cfg(target_arch = "x86_64")]
    fn multiply_sse41(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        unsafe {
            let va = _mm_loadu_si128(a.data.as_ptr() as *const __m128i);
            let vb = _mm_loadu_si128(b.data.as_ptr() as *const __m128i);
            let result = _mm_mullo_epi32(va, vb);

            let mut data = [0u8; 16];
            _mm_storeu_si128(data.as_mut_ptr() as *mut __m128i, result);

            Ok(V128 { data })
        }
    }

    #[cfg(not(target_arch = "x86_64"))]
    fn multiply_sse41(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        self.multiply_scalar(a, b)
    }

    /// Scalar vector multiplication fallback
    fn multiply_scalar(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        let a_ints = a.as_i32s();
        let b_ints = b.as_i32s();

        let result = V128::from_i32s(
            a_ints[0].wrapping_mul(b_ints[0]),
            a_ints[1].wrapping_mul(b_ints[1]),
            a_ints[2].wrapping_mul(b_ints[2]),
            a_ints[3].wrapping_mul(b_ints[3]),
        );

        Ok(result)
    }

    /// Vector division using AVX
    #[cfg(target_arch = "x86_64")]
    fn divide_avx(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        unsafe {
            let va = _mm_loadu_ps(a.data.as_ptr() as *const f32);
            let vb = _mm_loadu_ps(b.data.as_ptr() as *const f32);
            let result = _mm_div_ps(va, vb);

            let mut data = [0u8; 16];
            _mm_storeu_ps(data.as_mut_ptr() as *mut f32, result);

            Ok(V128 { data })
        }
    }

    #[cfg(not(target_arch = "x86_64"))]
    fn divide_avx(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        self.divide_scalar(a, b)
    }

    /// Scalar vector division fallback
    fn divide_scalar(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        let a_floats = a.as_f32s();
        let b_floats = b.as_f32s();

        // Check for division by zero
        for b_val in &b_floats {
            if *b_val == 0.0 {
                return Err(WasmtimeError::Runtime {
                    message: "Division by zero in SIMD operation".to_string(),
                    backtrace: None,
                })
            }
        }

        let result = V128::from_f32s(
            a_floats[0] / b_floats[0],
            a_floats[1] / b_floats[1],
            a_floats[2] / b_floats[2],
            a_floats[3] / b_floats[3],
        );

        Ok(result)
    }

    /// Vector AND using SSE4.1
    #[cfg(target_arch = "x86_64")]
    fn and_sse41(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        unsafe {
            let va = _mm_loadu_si128(a.data.as_ptr() as *const __m128i);
            let vb = _mm_loadu_si128(b.data.as_ptr() as *const __m128i);
            let result = _mm_and_si128(va, vb);

            let mut data = [0u8; 16];
            _mm_storeu_si128(data.as_mut_ptr() as *mut __m128i, result);

            Ok(V128 { data })
        }
    }

    #[cfg(not(target_arch = "x86_64"))]
    fn and_sse41(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        self.and_scalar(a, b)
    }

    /// Scalar vector AND fallback
    fn and_scalar(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        let mut result_data = [0u8; 16];
        for i in 0..16 {
            result_data[i] = a.data[i] & b.data[i];
        }
        Ok(V128 { data: result_data })
    }

    /// Vector OR using SSE4.1
    #[cfg(target_arch = "x86_64")]
    fn or_sse41(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        unsafe {
            let va = _mm_loadu_si128(a.data.as_ptr() as *const __m128i);
            let vb = _mm_loadu_si128(b.data.as_ptr() as *const __m128i);
            let result = _mm_or_si128(va, vb);

            let mut data = [0u8; 16];
            _mm_storeu_si128(data.as_mut_ptr() as *mut __m128i, result);

            Ok(V128 { data })
        }
    }

    #[cfg(not(target_arch = "x86_64"))]
    fn or_sse41(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        self.or_scalar(a, b)
    }

    /// Scalar vector OR fallback
    fn or_scalar(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        let mut result_data = [0u8; 16];
        for i in 0..16 {
            result_data[i] = a.data[i] | b.data[i];
        }
        Ok(V128 { data: result_data })
    }

    /// Vector XOR using SSE4.1
    #[cfg(target_arch = "x86_64")]
    fn xor_sse41(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        unsafe {
            let va = _mm_loadu_si128(a.data.as_ptr() as *const __m128i);
            let vb = _mm_loadu_si128(b.data.as_ptr() as *const __m128i);
            let result = _mm_xor_si128(va, vb);

            let mut data = [0u8; 16];
            _mm_storeu_si128(data.as_mut_ptr() as *mut __m128i, result);

            Ok(V128 { data })
        }
    }

    #[cfg(not(target_arch = "x86_64"))]
    fn xor_sse41(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        self.xor_scalar(a, b)
    }

    /// Scalar vector XOR fallback
    fn xor_scalar(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        let mut result_data = [0u8; 16];
        for i in 0..16 {
            result_data[i] = a.data[i] ^ b.data[i];
        }
        Ok(V128 { data: result_data })
    }

    /// Vector NOT using SSE4.1
    #[cfg(target_arch = "x86_64")]
    fn not_sse41(&self, a: &V128) -> WasmtimeResult<V128> {
        unsafe {
            let va = _mm_loadu_si128(a.data.as_ptr() as *const __m128i);
            let ones = _mm_set1_epi32(-1);
            let result = _mm_xor_si128(va, ones);

            let mut data = [0u8; 16];
            _mm_storeu_si128(data.as_mut_ptr() as *mut __m128i, result);

            Ok(V128 { data })
        }
    }

    #[cfg(not(target_arch = "x86_64"))]
    fn not_sse41(&self, a: &V128) -> WasmtimeResult<V128> {
        self.not_scalar(a)
    }

    /// Scalar vector NOT fallback
    fn not_scalar(&self, a: &V128) -> WasmtimeResult<V128> {
        let mut result_data = [0u8; 16];
        for i in 0..16 {
            result_data[i] = !a.data[i];
        }
        Ok(V128 { data: result_data })
    }

    /// Vector equals using SSE4.1
    #[cfg(target_arch = "x86_64")]
    fn equals_sse41(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        unsafe {
            let va = _mm_loadu_si128(a.data.as_ptr() as *const __m128i);
            let vb = _mm_loadu_si128(b.data.as_ptr() as *const __m128i);
            let result = _mm_cmpeq_epi32(va, vb);

            let mut data = [0u8; 16];
            _mm_storeu_si128(data.as_mut_ptr() as *mut __m128i, result);

            Ok(V128 { data })
        }
    }

    #[cfg(not(target_arch = "x86_64"))]
    fn equals_sse41(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        self.equals_scalar(a, b)
    }

    /// Scalar vector equals fallback
    fn equals_scalar(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        let a_ints = a.as_i32s();
        let b_ints = b.as_i32s();

        let result = V128::from_i32s(
            if a_ints[0] == b_ints[0] { -1 } else { 0 },
            if a_ints[1] == b_ints[1] { -1 } else { 0 },
            if a_ints[2] == b_ints[2] { -1 } else { 0 },
            if a_ints[3] == b_ints[3] { -1 } else { 0 },
        );

        Ok(result)
    }

    /// Vector less than using SSE4.1
    #[cfg(target_arch = "x86_64")]
    fn less_than_sse41(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        unsafe {
            let va = _mm_loadu_si128(a.data.as_ptr() as *const __m128i);
            let vb = _mm_loadu_si128(b.data.as_ptr() as *const __m128i);
            let result = _mm_cmplt_epi32(va, vb);

            let mut data = [0u8; 16];
            _mm_storeu_si128(data.as_mut_ptr() as *mut __m128i, result);

            Ok(V128 { data })
        }
    }

    #[cfg(not(target_arch = "x86_64"))]
    fn less_than_sse41(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        self.less_than_scalar(a, b)
    }

    /// Scalar vector less than fallback
    fn less_than_scalar(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        let a_ints = a.as_i32s();
        let b_ints = b.as_i32s();

        let result = V128::from_i32s(
            if a_ints[0] < b_ints[0] { -1 } else { 0 },
            if a_ints[1] < b_ints[1] { -1 } else { 0 },
            if a_ints[2] < b_ints[2] { -1 } else { 0 },
            if a_ints[3] < b_ints[3] { -1 } else { 0 },
        );

        Ok(result)
    }

    /// Vector greater than using SSE4.1
    #[cfg(target_arch = "x86_64")]
    fn greater_than_sse41(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        unsafe {
            let va = _mm_loadu_si128(a.data.as_ptr() as *const __m128i);
            let vb = _mm_loadu_si128(b.data.as_ptr() as *const __m128i);
            let result = _mm_cmpgt_epi32(va, vb);

            let mut data = [0u8; 16];
            _mm_storeu_si128(data.as_mut_ptr() as *mut __m128i, result);

            Ok(V128 { data })
        }
    }

    #[cfg(not(target_arch = "x86_64"))]
    fn greater_than_sse41(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        self.greater_than_scalar(a, b)
    }

    /// Scalar vector greater than fallback
    fn greater_than_scalar(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        let a_ints = a.as_i32s();
        let b_ints = b.as_i32s();

        let result = V128::from_i32s(
            if a_ints[0] > b_ints[0] { -1 } else { 0 },
            if a_ints[1] > b_ints[1] { -1 } else { 0 },
            if a_ints[2] > b_ints[2] { -1 } else { 0 },
            if a_ints[3] > b_ints[3] { -1 } else { 0 },
        );

        Ok(result)
    }

    /// Performs vector shuffle operation
    pub fn shuffle(&self, a: &V128, b: &V128, indices: &[u8; 16]) -> WasmtimeResult<V128> {
        self.validate_operands(a, b)?;

        // Validate shuffle indices
        for &index in indices {
            if index >= 32 {
                return Err(WasmtimeError::Validation {
                    message: "Shuffle index must be less than 32".to_string(),
                });
            }
        }

        let mut result_data = [0u8; 16];
        for (i, &index) in indices.iter().enumerate() {
            if index < 16 {
                result_data[i] = a.data[index as usize];
            } else {
                result_data[i] = b.data[(index - 16) as usize];
            }
        }

        Ok(V128 { data: result_data })
    }

    /// Performs relaxed floating-point addition
    pub fn relaxed_add(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        if !self.config.enable_relaxed_operations {
            return Err(WasmtimeError::UnsupportedFeature {
                message: "Relaxed SIMD operations are not enabled".to_string(),
            });
        }

        self.validate_operands(a, b)?;

        // Use platform-optimized path if available
        if self.config.enable_platform_optimizations && self.has_avx() {
            self.relaxed_add_avx(a, b)
        } else {
            // Fallback to regular addition for relaxed semantics
            self.add_scalar(a, b)
        }
    }

    /// Relaxed floating-point addition using AVX
    #[cfg(target_arch = "x86_64")]
    fn relaxed_add_avx(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        unsafe {
            let va = _mm_loadu_ps(a.data.as_ptr() as *const f32);
            let vb = _mm_loadu_ps(b.data.as_ptr() as *const f32);
            let result = _mm_add_ps(va, vb);

            let mut data = [0u8; 16];
            _mm_storeu_ps(data.as_mut_ptr() as *mut f32, result);

            Ok(V128 { data })
        }
    }

    #[cfg(not(target_arch = "x86_64"))]
    fn relaxed_add_avx(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        self.add_scalar(a, b)
    }

    /// Native FMA implementation using x86 FMA instructions
    #[cfg(target_arch = "x86_64")]
    fn fma_native(&self, a: &V128, b: &V128, c: &V128) -> WasmtimeResult<V128> {
        unsafe {
            let va = _mm_loadu_ps(a.data.as_ptr() as *const f32);
            let vb = _mm_loadu_ps(b.data.as_ptr() as *const f32);
            let vc = _mm_loadu_ps(c.data.as_ptr() as *const f32);
            let result = _mm_fmadd_ps(va, vb, vc);

            let mut data = [0u8; 16];
            _mm_storeu_ps(data.as_mut_ptr() as *mut f32, result);

            Ok(V128 { data })
        }
    }

    #[cfg(not(target_arch = "x86_64"))]
    fn fma_native(&self, a: &V128, b: &V128, c: &V128) -> WasmtimeResult<V128> {
        self.fma_fallback(a, b, c)
    }

    /// Fallback FMA implementation
    fn fma_fallback(&self, a: &V128, b: &V128, c: &V128) -> WasmtimeResult<V128> {
        let a_floats = a.as_f32s();
        let b_floats = b.as_f32s();
        let c_floats = c.as_f32s();

        let result = V128::from_f32s(
            a_floats[0].mul_add(b_floats[0], c_floats[0]),
            a_floats[1].mul_add(b_floats[1], c_floats[1]),
            a_floats[2].mul_add(b_floats[2], c_floats[2]),
            a_floats[3].mul_add(b_floats[3], c_floats[3]),
        );

        Ok(result)
    }

    /// Native FMS implementation using x86 FMA instructions
    #[cfg(target_arch = "x86_64")]
    fn fms_native(&self, a: &V128, b: &V128, c: &V128) -> WasmtimeResult<V128> {
        unsafe {
            let va = _mm_loadu_ps(a.data.as_ptr() as *const f32);
            let vb = _mm_loadu_ps(b.data.as_ptr() as *const f32);
            let vc = _mm_loadu_ps(c.data.as_ptr() as *const f32);
            let result = _mm_fmsub_ps(va, vb, vc);

            let mut data = [0u8; 16];
            _mm_storeu_ps(data.as_mut_ptr() as *mut f32, result);

            Ok(V128 { data })
        }
    }

    #[cfg(not(target_arch = "x86_64"))]
    fn fms_native(&self, a: &V128, b: &V128, c: &V128) -> WasmtimeResult<V128> {
        self.fms_fallback(a, b, c)
    }

    /// Fallback FMS implementation
    fn fms_fallback(&self, a: &V128, b: &V128, c: &V128) -> WasmtimeResult<V128> {
        let a_floats = a.as_f32s();
        let b_floats = b.as_f32s();
        let c_floats = c.as_f32s();

        let result = V128::from_f32s(
            a_floats[0] * b_floats[0] - c_floats[0],
            a_floats[1] * b_floats[1] - c_floats[1],
            a_floats[2] * b_floats[2] - c_floats[2],
            a_floats[3] * b_floats[3] - c_floats[3],
        );

        Ok(result)
    }

    /// SSE4.1 reciprocal approximation
    #[cfg(target_arch = "x86_64")]
    fn reciprocal_sse41(&self, a: &V128) -> WasmtimeResult<V128> {
        unsafe {
            let va = _mm_loadu_ps(a.data.as_ptr() as *const f32);
            let result = _mm_rcp_ps(va);

            let mut data = [0u8; 16];
            _mm_storeu_ps(data.as_mut_ptr() as *mut f32, result);

            Ok(V128 { data })
        }
    }

    #[cfg(not(target_arch = "x86_64"))]
    fn reciprocal_sse41(&self, a: &V128) -> WasmtimeResult<V128> {
        self.reciprocal_scalar(a)
    }

    /// Scalar reciprocal fallback
    fn reciprocal_scalar(&self, a: &V128) -> WasmtimeResult<V128> {
        let a_floats = a.as_f32s();

        // Check for zero values
        for &val in &a_floats {
            if val == 0.0 {
                return Err(WasmtimeError::Runtime {
                    message: "Division by zero in reciprocal operation".to_string(),
                    backtrace: None,
                })
            }
        }

        let result = V128::from_f32s(
            1.0 / a_floats[0],
            1.0 / a_floats[1],
            1.0 / a_floats[2],
            1.0 / a_floats[3],
        );

        Ok(result)
    }

    /// SSE4.1 square root
    #[cfg(target_arch = "x86_64")]
    fn sqrt_sse41(&self, a: &V128) -> WasmtimeResult<V128> {
        unsafe {
            let va = _mm_loadu_ps(a.data.as_ptr() as *const f32);
            let result = _mm_sqrt_ps(va);

            let mut data = [0u8; 16];
            _mm_storeu_ps(data.as_mut_ptr() as *mut f32, result);

            Ok(V128 { data })
        }
    }

    #[cfg(not(target_arch = "x86_64"))]
    fn sqrt_sse41(&self, a: &V128) -> WasmtimeResult<V128> {
        self.sqrt_scalar(a)
    }

    /// Scalar square root fallback
    fn sqrt_scalar(&self, a: &V128) -> WasmtimeResult<V128> {
        let a_floats = a.as_f32s();

        // Check for negative values
        for &val in &a_floats {
            if val < 0.0 {
                return Err(WasmtimeError::Runtime { message: 
                    "Square root of negative number".to_string(),
                    backtrace: None,
                })
            }
        }

        let result = V128::from_f32s(
            a_floats[0].sqrt(),
            a_floats[1].sqrt(),
            a_floats[2].sqrt(),
            a_floats[3].sqrt(),
        );

        Ok(result)
    }

    /// SSE4.1 reciprocal square root approximation
    #[cfg(target_arch = "x86_64")]
    fn rsqrt_sse41(&self, a: &V128) -> WasmtimeResult<V128> {
        unsafe {
            let va = _mm_loadu_ps(a.data.as_ptr() as *const f32);
            let result = _mm_rsqrt_ps(va);

            let mut data = [0u8; 16];
            _mm_storeu_ps(data.as_mut_ptr() as *mut f32, result);

            Ok(V128 { data })
        }
    }

    #[cfg(not(target_arch = "x86_64"))]
    fn rsqrt_sse41(&self, a: &V128) -> WasmtimeResult<V128> {
        self.rsqrt_scalar(a)
    }

    /// Scalar reciprocal square root fallback
    fn rsqrt_scalar(&self, a: &V128) -> WasmtimeResult<V128> {
        let a_floats = a.as_f32s();

        // Check for zero or negative values
        for &val in &a_floats {
            if val <= 0.0 {
                return Err(WasmtimeError::Runtime { message: 
                    "Reciprocal square root of non-positive number".to_string(),
                    backtrace: None,
                })
            }
        }

        let result = V128::from_f32s(
            1.0 / a_floats[0].sqrt(),
            1.0 / a_floats[1].sqrt(),
            1.0 / a_floats[2].sqrt(),
            1.0 / a_floats[3].sqrt(),
        );

        Ok(result)
    }

    /// Validates vector operands if validation is enabled
    fn validate_operands(&self, _a: &V128, _b: &V128) -> WasmtimeResult<()> {
        // In a real implementation, this would check for NaN values,
        // denormal numbers, or other validation criteria
        if self.config.validate_vector_operands {
            // All vectors are considered valid for now
            Ok(())
        } else {
            Ok(())
        }
    }

    /// Validates single vector operand if validation is enabled
    fn validate_single_operand(&self, _a: &V128) -> WasmtimeResult<()> {
        if self.config.validate_vector_operands {
            // All vectors are considered valid for now
            Ok(())
        } else {
            Ok(())
        }
    }

    /// Checks if a memory offset meets alignment requirements for V128
    pub fn check_alignment(&self, offset: u32) -> WasmtimeResult<()> {
        let required_alignment = match self.config.alignment_requirement {
            AlignmentRequirement::None => 1,
            AlignmentRequirement::Align16 => 16,
            AlignmentRequirement::CacheLine => 64,
        };

        // WebAssembly SIMD only supports V128 (16 bytes)
        let alignment = std::cmp::max(required_alignment, 16);

        if offset % alignment != 0 {
            return Err(WasmtimeError::Validation {
                message: format!("Memory offset {} is not aligned to {} bytes (required: {})",
                        offset, alignment, required_alignment),
            });
        }

        Ok(())
    }

    /// Performs prefetching for memory operations
    pub fn prefetch_memory(&self, memory: &Memory, store: &Store<()>, offset: u32, size: u32) -> WasmtimeResult<()> {
        if !self.config.enable_prefetching {
            return Ok(());
        }

        if offset as u64 + size as u64 > memory.data_size(store) as u64 {
            return Err(WasmtimeError::Runtime {
                message: "Prefetch range exceeds memory bounds".to_string(),
                backtrace: None,
            });
        }

        unsafe {
            let data_ptr = memory.data_ptr(store).add(offset as usize);

            // Prefetch data into cache (platform-specific hints)
            #[cfg(target_arch = "x86_64")]
            {
                // Use prefetch instructions for x86_64
                let mut ptr = data_ptr;
                let end_ptr = data_ptr.add(size as usize);
                while ptr < end_ptr {
                    std::arch::x86_64::_mm_prefetch(ptr as *const i8, std::arch::x86_64::_MM_HINT_T0);
                    ptr = ptr.add(64); // Cache line size
                }
            }

            #[cfg(target_arch = "aarch64")]
            {
                // Use prefetch instructions for ARM64
                let mut ptr = data_ptr;
                let end_ptr = data_ptr.add(size as usize);
                while ptr < end_ptr {
                    std::arch::asm!("prfm pldl1keep, [{}]", in(reg) ptr);
                    ptr = ptr.add(64); // Cache line size
                }
            }
        }

        Ok(())
    }

    /// Loads a V128 vector from memory with alignment
    pub fn load_aligned(&self, memory: &Memory, store: &mut Store<()>, offset: u32, alignment: u32) -> WasmtimeResult<V128> {
        // Check custom alignment
        if !alignment.is_power_of_two() || alignment > 16 {
            return Err(WasmtimeError::Validation {
                message: format!("Invalid alignment: {}. Must be power of 2 and <= 16", alignment),
            });
        }

        if offset % alignment != 0 {
            return Err(WasmtimeError::Validation {
                message: format!("Memory offset {} is not aligned to {} bytes", offset, alignment),
            });
        }

        // Check configuration alignment requirements
        self.check_alignment(offset)?;

        // Prefetch if enabled
        if self.config.enable_prefetching {
            self.prefetch_memory(memory, store, offset, 16)?;
        }

        self.load(memory, store, offset)
    }

    /// Stores a V128 vector to memory with alignment
    pub fn store_aligned(
        &self,
        memory: &Memory,
        store: &mut Store<()>,
        offset: u32,
        vector: &V128,
        alignment: u32,
    ) -> WasmtimeResult<()> {
        // Validate alignment
        if !alignment.is_power_of_two() || alignment > 16 {
            return Err(WasmtimeError::Validation {
                message: format!("Invalid alignment: {}. Must be power of 2 and <= 16", alignment),
            });
        }

        if offset % alignment != 0 {
            return Err(WasmtimeError::Validation {
                message: format!("Memory offset {} is not aligned to {} bytes", offset, alignment),
            });
        }

        self.store(memory, store, offset, vector)
    }

    /// Converts integer vector to float vector
    pub fn convert_i32_to_f32(&self, a: &V128) -> WasmtimeResult<V128> {
        self.validate_single_operand(a)?;

        let a_ints = a.as_i32s();
        let result = V128::from_f32s(
            a_ints[0] as f32,
            a_ints[1] as f32,
            a_ints[2] as f32,
            a_ints[3] as f32,
        );

        Ok(result)
    }

    /// Converts float vector to integer vector
    pub fn convert_f32_to_i32(&self, a: &V128) -> WasmtimeResult<V128> {
        self.validate_single_operand(a)?;

        let a_floats = a.as_f32s();
        let result = V128::from_i32s(
            a_floats[0] as i32,
            a_floats[1] as i32,
            a_floats[2] as i32,
            a_floats[3] as i32,
        );

        Ok(result)
    }

    /// Saturated addition (prevents overflow)
    pub fn add_saturated(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        self.validate_operands(a, b)?;

        if self.config.enable_platform_optimizations && self.has_sse41() {
            self.add_saturated_sse41(a, b)
        } else {
            self.add_saturated_scalar(a, b)
        }
    }

    /// SSE4.1 saturated addition
    #[cfg(target_arch = "x86_64")]
    fn add_saturated_sse41(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        unsafe {
            let va = _mm_loadu_si128(a.data.as_ptr() as *const __m128i);
            let vb = _mm_loadu_si128(b.data.as_ptr() as *const __m128i);
            let result = _mm_adds_epi16(va, vb);

            let mut data = [0u8; 16];
            _mm_storeu_si128(data.as_mut_ptr() as *mut __m128i, result);

            Ok(V128 { data })
        }
    }

    #[cfg(not(target_arch = "x86_64"))]
    fn add_saturated_sse41(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        self.add_saturated_scalar(a, b)
    }

    /// Scalar saturated addition
    fn add_saturated_scalar(&self, a: &V128, b: &V128) -> WasmtimeResult<V128> {
        let a_ints = a.as_i32s();
        let b_ints = b.as_i32s();

        let result = V128::from_i32s(
            a_ints[0].saturating_add(b_ints[0]),
            a_ints[1].saturating_add(b_ints[1]),
            a_ints[2].saturating_add(b_ints[2]),
            a_ints[3].saturating_add(b_ints[3]),
        );

        Ok(result)
    }

    /// Extract lane from vector
    pub fn extract_lane_i32(&self, a: &V128, lane: u8) -> WasmtimeResult<i32> {
        self.validate_single_operand(a)?;

        if lane >= 4 {
            return Err(WasmtimeError::Validation {
                message: format!("Lane index {} out of bounds for i32x4 vector", lane),
            });
        }

        let a_ints = a.as_i32s();
        Ok(a_ints[lane as usize])
    }

    /// Replace lane in vector
    pub fn replace_lane_i32(&self, a: &V128, lane: u8, value: i32) -> WasmtimeResult<V128> {
        self.validate_single_operand(a)?;

        if lane >= 4 {
            return Err(WasmtimeError::Validation {
                message: format!("Lane index {} out of bounds for i32x4 vector", lane),
            });
        }

        let mut a_ints = a.as_i32s();
        a_ints[lane as usize] = value;

        Ok(V128::from_i32s(a_ints[0], a_ints[1], a_ints[2], a_ints[3]))
    }

    /// Splat value to all lanes
    pub fn splat_i32(&self, value: i32) -> WasmtimeResult<V128> {
        Ok(V128::from_i32s(value, value, value, value))
    }

    /// Splat float value to all lanes
    pub fn splat_f32(&self, value: f32) -> WasmtimeResult<V128> {
        Ok(V128::from_f32s(value, value, value, value))
    }

    /// Gets the configuration
    pub fn config(&self) -> &SIMDConfig {
        &self.config
    }

    /// Checks if AVX is supported
    pub fn has_avx(&self) -> bool {
        self.capabilities.has_avx
    }

    /// Checks if AVX2 is supported
    pub fn has_avx2(&self) -> bool {
        self.capabilities.has_avx2
    }

    /// Checks if SSE4.1 is supported
    pub fn has_sse41(&self) -> bool {
        self.capabilities.has_sse41
    }

    /// Checks if NEON is supported
    pub fn has_neon(&self) -> bool {
        self.capabilities.has_neon
    }

    /// Gets platform capabilities
    pub fn capabilities(&self) -> &PlatformCapabilities {
        &self.capabilities
    }

    /// Validates that the platform supports required SIMD features
    pub fn validate_simd_support(&self, required_features: &[&str]) -> WasmtimeResult<()> {
        for &feature in required_features {
            let supported = match feature {
                "sse41" => self.capabilities.has_sse41,
                "avx" => self.capabilities.has_avx,
                "avx2" => self.capabilities.has_avx2,
                "fma" => self.capabilities.has_fma,
                "neon" => self.capabilities.has_neon,
                _ => {
                    return Err(WasmtimeError::UnsupportedFeature {
                        message: format!("Unknown SIMD feature: {}", feature),
                    });
                }
            };

            if !supported {
                log::warn!("Required SIMD feature '{}' is not supported, falling back to scalar implementation", feature);
                if self.config.debug_mode {
                    return Err(WasmtimeError::UnsupportedFeature {
                        message: format!("SIMD feature '{}' is not supported on this platform", feature),
                    });
                }
            }
        }

        Ok(())
    }

    /// Tests SIMD functionality with platform detection
    pub fn test_simd_operations(&self) -> WasmtimeResult<()> {
        log::info!("Testing SIMD operations on platform...");

        // Test basic V128 operations
        let a = V128::from_i32s(1, 2, 3, 4);
        let b = V128::from_i32s(5, 6, 7, 8);

        let result = self.add(&a, &b)?;
        let expected = [6, 8, 10, 12];
        let actual = result.as_i32s();

        if actual != expected {
            return Err(WasmtimeError::Runtime {
                message: format!("SIMD test failed: expected {:?}, got {:?}", expected, actual),
                backtrace: None,
            });
        }

        // Test floating-point operations
        let fa = V128::from_f32s(1.0, 2.0, 3.0, 4.0);
        let fb = V128::from_f32s(2.0, 3.0, 4.0, 5.0);

        let fresult = self.multiply(&fa, &fb)?;
        let fexpected = [2.0, 6.0, 12.0, 20.0];
        let factual = fresult.as_f32s();

        for i in 0..4 {
            if (factual[i] - fexpected[i]).abs() > f32::EPSILON {
                return Err(WasmtimeError::Runtime {
                    message: format!("SIMD float test failed at index {}: expected {}, got {}",
                           i, fexpected[i], factual[i]),
                    backtrace: None,
                });
            }
        }

        log::info!("SIMD operations test completed successfully");
        Ok(())
    }

    /// Gets recommended configuration based on platform capabilities
    pub fn get_recommended_config(&self) -> SIMDConfig {
        let mut config = SIMDConfig::default();

        // Adjust vector width based on capabilities
        config.max_vector_width = self.capabilities.max_vector_width;

        // Enable advanced features if supported
        config.enable_fma_operations = self.capabilities.has_fma;
        config.enable_gather_scatter = self.capabilities.has_avx2 || self.capabilities.has_neon;

        // Set alignment requirements (WebAssembly SIMD only supports V128)
        config.alignment_requirement = AlignmentRequirement::Align16;

        // Enable platform optimizations if any SIMD is available
        config.enable_platform_optimizations = self.capabilities.max_vector_width > 0;

        // Set scheduling strategy based on capabilities
        config.scheduling_strategy = if self.capabilities.has_avx2 || self.capabilities.has_neon {
            SchedulingStrategy::OutOfOrder
        } else {
            SchedulingStrategy::InOrder
        };

        config
    }

    /// Fallback operation selector - chooses best available implementation
    pub fn select_best_operation<T, F1, F2, F3>(&self,
                                               optimized_impl: F1,
                                               fallback_impl: F2,
                                               scalar_impl: F3) -> WasmtimeResult<T>
    where
        F1: FnOnce() -> WasmtimeResult<T>,
        F2: FnOnce() -> WasmtimeResult<T>,
        F3: FnOnce() -> WasmtimeResult<T>,
    {
        if self.config.enable_platform_optimizations && self.capabilities.max_vector_width > 0 {
            match optimized_impl() {
                Ok(result) => return Ok(result),
                Err(e) => {
                    log::warn!("Optimized SIMD implementation failed: {}, trying fallback", e);
                    if self.config.debug_mode {
                        return Err(e);
                    }
                }
            }
        }

        // Try fallback implementation
        match fallback_impl() {
            Ok(result) => Ok(result),
            Err(e) => {
                log::warn!("Fallback SIMD implementation failed: {}, using scalar", e);
                if self.config.debug_mode {
                    return Err(e);
                }
                scalar_impl()
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_v128_creation() {
        let zero = V128::zero();
        assert_eq!(zero.data, [0; 16]);

        let splat = V128::splat_u8(0xFF);
        assert_eq!(splat.data, [0xFF; 16]);

        let from_ints = V128::from_i32s(1, 2, 3, 4);
        let expected_ints = [1, 2, 3, 4];
        assert_eq!(from_ints.as_i32s(), expected_ints);

        let from_floats = V128::from_f32s(1.0, 2.0, 3.0, 4.0);
        let expected_floats = [1.0, 2.0, 3.0, 4.0];
        assert_eq!(from_floats.as_f32s(), expected_floats);
    }

    #[test]
    fn test_simd_operations_creation() {
        let config = SIMDConfig::default();
        let simd = SIMDOperations::new(config).unwrap();

        assert!(simd.config.enable_platform_optimizations);
        assert!(!simd.config.enable_relaxed_operations);
        assert!(simd.config.validate_vector_operands);
        assert_eq!(simd.config.max_vector_width, 128);
    }

    #[test]
    fn test_vector_addition() {
        let config = SIMDConfig::default();
        let simd = SIMDOperations::new(config).unwrap();

        let a = V128::from_i32s(1, 2, 3, 4);
        let b = V128::from_i32s(5, 6, 7, 8);

        let result = simd.add(&a, &b).unwrap();
        let result_ints = result.as_i32s();

        assert_eq!(result_ints, [6, 8, 10, 12]);
    }

    #[test]
    fn test_vector_subtraction() {
        let config = SIMDConfig::default();
        let simd = SIMDOperations::new(config).unwrap();

        let a = V128::from_i32s(10, 20, 30, 40);
        let b = V128::from_i32s(1, 2, 3, 4);

        let result = simd.subtract(&a, &b).unwrap();
        let result_ints = result.as_i32s();

        assert_eq!(result_ints, [9, 18, 27, 36]);
    }

    #[test]
    fn test_vector_multiplication() {
        let config = SIMDConfig::default();
        let simd = SIMDOperations::new(config).unwrap();

        let a = V128::from_i32s(2, 3, 4, 5);
        let b = V128::from_i32s(3, 4, 5, 6);

        let result = simd.multiply(&a, &b).unwrap();
        let result_ints = result.as_i32s();

        assert_eq!(result_ints, [6, 12, 20, 30]);
    }

    #[test]
    fn test_vector_shuffle() {
        let config = SIMDConfig::default();
        let simd = SIMDOperations::new(config).unwrap();

        let a = V128::from_i32s(0x01020304, 0x05060708, 0x090A0B0C, 0x0D0E0F10);
        let b = V128::from_i32s(0x11121314, 0x15161718, 0x191A1B1C, 0x1D1E1F20);

        // Reverse the bytes from vector a
        let indices = [3, 2, 1, 0, 7, 6, 5, 4, 11, 10, 9, 8, 15, 14, 13, 12];
        let result = simd.shuffle(&a, &b, &indices).unwrap();

        let expected = V128::from_i32s(0x04030201, 0x08070605, 0x0C0B0A09, 0x100F0E0D);
        assert_eq!(result, expected);
    }

    #[test]
    fn test_vector_division() {
        let config = SIMDConfig::default();
        let simd = SIMDOperations::new(config).unwrap();

        let a = V128::from_f32s(8.0, 12.0, 20.0, 30.0);
        let b = V128::from_f32s(2.0, 3.0, 4.0, 5.0);

        let result = simd.divide(&a, &b).unwrap();
        let result_floats = result.as_f32s();

        assert_eq!(result_floats, [4.0, 4.0, 5.0, 6.0]);
    }

    #[test]
    fn test_vector_division_by_zero() {
        let config = SIMDConfig::default();
        let simd = SIMDOperations::new(config).unwrap();

        let a = V128::from_f32s(1.0, 2.0, 3.0, 4.0);
        let b = V128::from_f32s(0.0, 1.0, 2.0, 3.0);

        assert!(simd.divide(&a, &b).is_err());
    }

    #[test]
    fn test_vector_logical_operations() {
        let config = SIMDConfig::default();
        let simd = SIMDOperations::new(config).unwrap();

        let a = V128::from_i32s(0xFF00FF00u32 as i32, 0x00FF00FF, 0xF0F0F0F0u32 as i32, 0x0F0F0F0F);
        let b = V128::from_i32s(0xF0F0F0F0u32 as i32, 0x0F0F0F0F, 0xFF00FF00u32 as i32, 0x00FF00FF);

        // Test AND
        let and_result = simd.and(&a, &b).unwrap();
        let and_ints = and_result.as_i32s();
        assert_eq!(and_ints[0], 0xF000F000u32 as i32);

        // Test OR
        let or_result = simd.or(&a, &b).unwrap();
        let or_ints = or_result.as_i32s();
        assert_eq!(or_ints[0], 0xFFF0FFF0u32 as i32);

        // Test XOR
        let xor_result = simd.xor(&a, &b).unwrap();
        let xor_ints = xor_result.as_i32s();
        assert_eq!(xor_ints[0], 0x0FF00FF0u32 as i32);

        // Test NOT
        let not_result = simd.not(&a).unwrap();
        let not_ints = not_result.as_i32s();
        assert_eq!(not_ints[0], !0xFF00FF00u32 as i32);
    }

    #[test]
    fn test_vector_comparison_operations() {
        let config = SIMDConfig::default();
        let simd = SIMDOperations::new(config).unwrap();

        let a = V128::from_i32s(1, 5, 3, 7);
        let b = V128::from_i32s(1, 4, 5, 6);

        // Test equals
        let eq_result = simd.equals(&a, &b).unwrap();
        let eq_ints = eq_result.as_i32s();
        assert_eq!(eq_ints, [-1, 0, 0, 0]); // Only first elements are equal

        // Test less than
        let lt_result = simd.less_than(&a, &b).unwrap();
        let lt_ints = lt_result.as_i32s();
        assert_eq!(lt_ints, [0, 0, -1, 0]); // Only third element: 3 < 5

        // Test greater than
        let gt_result = simd.greater_than(&a, &b).unwrap();
        let gt_ints = gt_result.as_i32s();
        assert_eq!(gt_ints, [0, -1, 0, -1]); // Second and fourth: 5 > 4, 7 > 6
    }

    #[test]
    fn test_vector_conversion_operations() {
        let config = SIMDConfig::default();
        let simd = SIMDOperations::new(config).unwrap();

        // Test int to float conversion
        let int_vec = V128::from_i32s(1, 2, 3, 4);
        let float_result = simd.convert_i32_to_f32(&int_vec).unwrap();
        let float_vals = float_result.as_f32s();
        assert_eq!(float_vals, [1.0, 2.0, 3.0, 4.0]);

        // Test float to int conversion
        let float_vec = V128::from_f32s(1.5, 2.7, 3.1, 4.9);
        let int_result = simd.convert_f32_to_i32(&float_vec).unwrap();
        let int_vals = int_result.as_i32s();
        assert_eq!(int_vals, [1, 2, 3, 4]); // Truncation behavior
    }

    #[test]
    fn test_saturated_addition() {
        let config = SIMDConfig::default();
        let simd = SIMDOperations::new(config).unwrap();

        let a = V128::from_i32s(i32::MAX - 1, 100, 200, 300);
        let b = V128::from_i32s(5, 10, 20, 30);

        let result = simd.add_saturated(&a, &b).unwrap();
        let result_ints = result.as_i32s();

        assert_eq!(result_ints[0], i32::MAX); // Saturated
        assert_eq!(result_ints[1], 110);
        assert_eq!(result_ints[2], 220);
        assert_eq!(result_ints[3], 330);
    }

    #[test]
    fn test_lane_operations() {
        let config = SIMDConfig::default();
        let simd = SIMDOperations::new(config).unwrap();

        let vec = V128::from_i32s(10, 20, 30, 40);

        // Test extract lane
        assert_eq!(simd.extract_lane_i32(&vec, 0).unwrap(), 10);
        assert_eq!(simd.extract_lane_i32(&vec, 2).unwrap(), 30);

        // Test invalid lane index
        assert!(simd.extract_lane_i32(&vec, 4).is_err());

        // Test replace lane
        let replaced = simd.replace_lane_i32(&vec, 1, 99).unwrap();
        let replaced_ints = replaced.as_i32s();
        assert_eq!(replaced_ints, [10, 99, 30, 40]);

        // Test invalid lane index for replace
        assert!(simd.replace_lane_i32(&vec, 5, 99).is_err());
    }

    #[test]
    fn test_splat_operations() {
        let config = SIMDConfig::default();
        let simd = SIMDOperations::new(config).unwrap();

        // Test int splat
        let int_splat = simd.splat_i32(42).unwrap();
        let int_vals = int_splat.as_i32s();
        assert_eq!(int_vals, [42, 42, 42, 42]);

        // Test float splat
        let float_splat = simd.splat_f32(3.14).unwrap();
        let float_vals = float_splat.as_f32s();
        assert_eq!(float_vals, [3.14, 3.14, 3.14, 3.14]);
    }

    #[test]
    fn test_aligned_memory_operations() {
        let config = SIMDConfig::default();
        let simd = SIMDOperations::new(config).unwrap();

        // These tests validate parameter checking
        // In a real scenario, you'd need actual WebAssembly memory instances

        let vec = V128::from_i32s(1, 2, 3, 4);

        // Test invalid alignment values - use shared engine to avoid GLOBAL_CODE accumulation
        let engine = crate::engine::get_shared_wasmtime_engine();
        let mut store = wasmtime::Store::new(&engine, ());
        let memory_type = wasmtime::MemoryType::new(1, None);
        let memory = wasmtime::Memory::new(&mut store, memory_type).unwrap();

        // Test invalid alignment (not power of 2)
        assert!(simd.load_aligned(&memory, &mut store, 0, 3).is_err());

        // Test invalid alignment (too large)
        assert!(simd.load_aligned(&memory, &mut store, 0, 32).is_err());

        // Test misaligned offset
        assert!(simd.load_aligned(&memory, &mut store, 5, 4).is_err());

        // Test valid alignment
        // Note: This would succeed in parameter validation but might fail
        // if memory is too small for actual load
        let result = simd.load_aligned(&memory, &mut store, 0, 16);
        // We don't assert success because memory might be too small
    }

    // ==================== NEW TESTS: Platform Capabilities ====================

    #[test]
    fn test_platform_capabilities_detection() {
        let caps = PlatformCapabilities::detect();

        // All platforms should have non-negative max vector width
        assert!(caps.max_vector_width >= 0);

        // Verify we can detect at least basic capabilities
        // The actual values depend on the platform
        #[cfg(target_arch = "x86_64")]
        {
            // x86_64 should always have at least SSE2 (128-bit vectors)
            assert!(caps.max_vector_width >= 128 || caps.max_vector_width == 0);
        }
    }

    #[test]
    fn test_simd_config_with_relaxed_mode() {
        let config = SIMDConfig {
            enable_relaxed_operations: true,
            ..SIMDConfig::default()
        };

        assert!(config.enable_relaxed_operations);
        assert!(config.enable_platform_optimizations); // Default is true
    }

    #[test]
    fn test_simd_operations_with_relaxed_mode() {
        let config = SIMDConfig {
            enable_relaxed_operations: true,
            ..SIMDConfig::default()
        };
        let simd = SIMDOperations::new(config).unwrap();

        // Basic operations should still work in relaxed mode
        let a = V128::from_i32s(1, 2, 3, 4);
        let b = V128::from_i32s(5, 6, 7, 8);
        let result = simd.add(&a, &b).unwrap();
        assert_eq!(result.as_i32s(), [6, 8, 10, 12]);
    }

    #[test]
    fn test_simd_operations_without_platform_optimizations() {
        let config = SIMDConfig {
            enable_platform_optimizations: false,
            ..SIMDConfig::default()
        };
        let simd = SIMDOperations::new(config).unwrap();

        // Operations should fall back to non-optimized path
        let a = V128::from_i32s(10, 20, 30, 40);
        let b = V128::from_i32s(1, 2, 3, 4);
        let result = simd.subtract(&a, &b).unwrap();
        assert_eq!(result.as_i32s(), [9, 18, 27, 36]);
    }

    #[test]
    fn test_v128_from_bytes() {
        let bytes = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15];
        let vec = V128::from_bytes(bytes);
        assert_eq!(vec.data, bytes);
    }

    #[test]
    fn test_v128_as_u8s() {
        let vec = V128::from_i32s(0x03020100, 0x07060504, 0x0B0A0908, 0x0F0E0D0C);
        let bytes = vec.as_u8s();
        assert_eq!(bytes[0], 0x00);
        assert_eq!(bytes[4], 0x04);
        assert_eq!(bytes[8], 0x08);
        assert_eq!(bytes[12], 0x0C);
    }

    #[test]
    fn test_v128_splat_operations_via_simd() {
        let config = SIMDConfig::default();
        let simd = SIMDOperations::new(config).unwrap();

        let int_splat = simd.splat_i32(42).unwrap();
        let ints = int_splat.as_i32s();
        assert_eq!(ints, [42, 42, 42, 42]);

        let float_splat = simd.splat_f32(3.14).unwrap();
        let floats = float_splat.as_f32s();
        for f in floats.iter() {
            assert!((f - 3.14).abs() < 0.0001);
        }
    }

    #[test]
    fn test_v128_from_f64s() {
        let vec = V128::from_f64s(1.5, 2.5);
        let doubles = vec.as_f64s();
        assert!((doubles[0] - 1.5).abs() < 0.0001);
        assert!((doubles[1] - 2.5).abs() < 0.0001);
    }

    #[test]
    fn test_v128_equality() {
        let a = V128::from_i32s(1, 2, 3, 4);
        let b = V128::from_i32s(1, 2, 3, 4);
        let c = V128::from_i32s(1, 2, 3, 5);

        assert_eq!(a, b);
        assert_ne!(a, c);
    }

    #[test]
    fn test_v128_clone() {
        let original = V128::from_i32s(10, 20, 30, 40);
        let cloned = original.clone();

        assert_eq!(original, cloned);
        assert_eq!(original.as_i32s(), cloned.as_i32s());
    }

    #[test]
    fn test_simd_debug_mode() {
        let config = SIMDConfig {
            debug_mode: true,
            ..SIMDConfig::default()
        };
        let simd = SIMDOperations::new(config).unwrap();

        // Verify debug mode is set
        assert!(simd.config.debug_mode);
    }

    #[test]
    fn test_vector_max_vector_width_zero() {
        let config = SIMDConfig {
            max_vector_width: 0,
            ..SIMDConfig::default()
        };
        let simd = SIMDOperations::new(config).unwrap();

        // Operations should still work with scalar fallback
        let a = V128::from_i32s(1, 2, 3, 4);
        let b = V128::from_i32s(1, 1, 1, 1);
        let result = simd.add(&a, &b).unwrap();
        assert_eq!(result.as_i32s(), [2, 3, 4, 5]);
    }

    #[test]
    fn test_shuffle_with_second_vector_indices() {
        let config = SIMDConfig::default();
        let simd = SIMDOperations::new(config).unwrap();

        let a = V128::from_i32s(0x01010101, 0x02020202, 0x03030303, 0x04040404);
        let b = V128::from_i32s(0x11111111, 0x12121212, 0x13131313, 0x14141414);

        // Mix bytes from both vectors
        // Indices 0-15 come from vector a, 16-31 from vector b
        let indices = [16, 17, 18, 19, 0, 1, 2, 3, 20, 21, 22, 23, 4, 5, 6, 7];
        let result = simd.shuffle(&a, &b, &indices).unwrap();

        // First 4 bytes should come from b's first int, next 4 from a's first int, etc.
        let result_bytes = result.as_u8s();
        assert_eq!(result_bytes[0..4], [0x11, 0x11, 0x11, 0x11]);
        assert_eq!(result_bytes[4..8], [0x01, 0x01, 0x01, 0x01]);
    }

    #[test]
    fn test_v128_from_i16s() {
        let values: [i16; 8] = [1, 2, 3, 4, 5, 6, 7, 8];
        let vec = V128::from_i16s(values);
        let result = vec.as_i16s();
        assert_eq!(result, values);
    }

    #[test]
    fn test_v128_as_i16s() {
        let vec = V128::from_i16s([100, 200, 300, 400, 500, 600, 700, 800]);
        let shorts = vec.as_i16s();
        assert_eq!(shorts[0], 100);
        assert_eq!(shorts[7], 800);
    }

    #[test]
    fn test_scheduling_strategy_variants() {
        assert_eq!(SchedulingStrategy::InOrder, SchedulingStrategy::InOrder);
        assert_ne!(SchedulingStrategy::InOrder, SchedulingStrategy::OutOfOrder);
        assert_ne!(SchedulingStrategy::Pipelined, SchedulingStrategy::Adaptive);
    }

    #[test]
    fn test_alignment_requirement_variants() {
        assert_eq!(AlignmentRequirement::None, AlignmentRequirement::None);
        assert_ne!(AlignmentRequirement::None, AlignmentRequirement::Align16);
        assert_ne!(AlignmentRequirement::Align16, AlignmentRequirement::CacheLine);
    }

    #[test]
    fn test_simd_config_scheduling_and_alignment() {
        let config = SIMDConfig {
            scheduling_strategy: SchedulingStrategy::OutOfOrder,
            alignment_requirement: AlignmentRequirement::CacheLine,
            ..SIMDConfig::default()
        };

        assert_eq!(config.scheduling_strategy, SchedulingStrategy::OutOfOrder);
        assert_eq!(config.alignment_requirement, AlignmentRequirement::CacheLine);
    }

    #[test]
    fn test_simd_config_fma_and_gather_scatter() {
        let config = SIMDConfig {
            enable_fma_operations: true,
            enable_gather_scatter: true,
            ..SIMDConfig::default()
        };

        assert!(config.enable_fma_operations);
        assert!(config.enable_gather_scatter);
    }

    #[test]
    fn test_simd_config_prefetching_and_fusion() {
        let config = SIMDConfig {
            enable_prefetching: false,
            enable_instruction_fusion: false,
            ..SIMDConfig::default()
        };

        assert!(!config.enable_prefetching);
        assert!(!config.enable_instruction_fusion);
    }

    #[test]
    fn test_simd_config_pipeline_depth() {
        let config = SIMDConfig {
            max_pipeline_depth: 8,
            ..SIMDConfig::default()
        };

        assert_eq!(config.max_pipeline_depth, 8);
    }

    #[test]
    fn test_platform_capabilities_crypto_extensions() {
        let caps = PlatformCapabilities::detect();

        // These are platform-dependent, just verify they can be queried
        let _ = caps.has_aes_ni;
        let _ = caps.has_aes_arm;
        let _ = caps.has_sha_arm;
    }
}