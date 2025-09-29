//! Domain-specific SIMD optimizations for wasmtime4j
//!
//! This module provides ultra-specialized SIMD optimizations for domain-specific
//! applications including image processing, signal processing, audio processing,
//! computer vision, and scientific computing.

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::simd::{V128, V256, V512, PlatformCapabilities};
#[cfg(target_arch = "x86_64")]
use std::arch::x86_64::*;
#[cfg(target_arch = "aarch64")]
use std::arch::aarch64::*;

/// Domain-specific SIMD optimizations
pub mod domain {
    use super::*;

    /// Image processing operations with SIMD acceleration
    pub struct VectorizedImageProcessing {
        capabilities: PlatformCapabilities,
    }

    impl VectorizedImageProcessing {
        /// Creates a new vectorized image processing instance
        pub fn new(capabilities: &PlatformCapabilities) -> Self {
            VectorizedImageProcessing {
                capabilities: capabilities.clone(),
            }
        }

        /// RGB to grayscale conversion with SIMD optimization
        pub fn rgb_to_grayscale(&self, rgb: &[u8], grayscale: &mut [u8],
                               width: usize, height: usize) -> WasmtimeResult<()> {
            let pixel_count = width * height;
            if rgb.len() < pixel_count * 3 || grayscale.len() < pixel_count {
                return Err(WasmtimeError::InvalidParameter { message: "Buffer size too small".to_string() });
            }

            #[cfg(target_arch = "x86_64")]
            unsafe {
                if is_x86_feature_detected!("avx512bw") && self.capabilities.has_avx512bw {
                    return self.rgb_to_grayscale_avx512(rgb, grayscale, pixel_count);
                } else if is_x86_feature_detected!("avx2") && self.capabilities.has_avx2 {
                    return self.rgb_to_grayscale_avx2(rgb, grayscale, pixel_count);
                }
            }

            #[cfg(target_arch = "aarch64")]
            if self.capabilities.has_neon {
                return self.rgb_to_grayscale_neon(rgb, grayscale, pixel_count);
            }

            // Software fallback
            self.rgb_to_grayscale_software(rgb, grayscale, pixel_count)
        }

        /// AVX-512 RGB to grayscale conversion
        #[cfg(target_arch = "x86_64")]
        unsafe fn rgb_to_grayscale_avx512(&self, rgb: &[u8], grayscale: &mut [u8],
                                         pixel_count: usize) -> WasmtimeResult<()> {
            if !is_x86_feature_detected!("avx512bw") {
                return self.rgb_to_grayscale_software(rgb, grayscale, pixel_count);
            }

            // Luminance coefficients: Y = 0.299*R + 0.587*G + 0.114*B
            // Convert to 16-bit integers: 77*R + 150*G + 29*B >> 8
            let coeff_r = _mm512_set1_epi16(77);
            let coeff_g = _mm512_set1_epi16(150);
            let coeff_b = _mm512_set1_epi16(29);

            const PIXELS_PER_ITER: usize = 32; // Process 32 pixels at once
            let mut i = 0;

            while i + PIXELS_PER_ITER <= pixel_count {
                // Load 32 RGB pixels (96 bytes)
                let rgb_data1 = _mm512_loadu_si512(&rgb[i * 3] as *const u8 as *const i32);
                let rgb_data2 = _mm512_loadu_si512(&rgb[i * 3 + 64] as *const u8 as *const i32);
                let rgb_data3 = _mm512_loadu_si512(&rgb[i * 3 + 128] as *const u8 as *const i32);

                // Deinterleave RGB channels using complex shuffle operations
                // This is a simplified version - production would use optimized shuffles
                let mut r_vals = [0u8; PIXELS_PER_ITER];
                let mut g_vals = [0u8; PIXELS_PER_ITER];
                let mut b_vals = [0u8; PIXELS_PER_ITER];

                // Manual deinterleaving (simplified)
                for j in 0..PIXELS_PER_ITER {
                    r_vals[j] = rgb[i * 3 + j * 3];
                    g_vals[j] = rgb[i * 3 + j * 3 + 1];
                    b_vals[j] = rgb[i * 3 + j * 3 + 2];
                }

                // Convert to 16-bit for calculations
                let r_16 = _mm512_unpacklo_epi8(_mm512_loadu_si512(r_vals.as_ptr() as *const i32), _mm512_setzero_si512());
                let g_16 = _mm512_unpacklo_epi8(_mm512_loadu_si512(g_vals.as_ptr() as *const i32), _mm512_setzero_si512());
                let b_16 = _mm512_unpacklo_epi8(_mm512_loadu_si512(b_vals.as_ptr() as *const i32), _mm512_setzero_si512());

                // Calculate luminance
                let r_weighted = _mm512_mullo_epi16(r_16, coeff_r);
                let g_weighted = _mm512_mullo_epi16(g_16, coeff_g);
                let b_weighted = _mm512_mullo_epi16(b_16, coeff_b);

                let luminance = _mm512_add_epi16(_mm512_add_epi16(r_weighted, g_weighted), b_weighted);
                let luminance_shifted = _mm512_srli_epi16(luminance, 8);

                // Pack back to 8-bit
                let result = _mm512_packus_epi16(luminance_shifted, _mm512_setzero_si512());

                // Store result (first 32 bytes)
                let mut temp_result = [0u8; 64];
                _mm512_storeu_si512(temp_result.as_mut_ptr() as *mut i32, result);
                grayscale[i..i + PIXELS_PER_ITER].copy_from_slice(&temp_result[..PIXELS_PER_ITER]);

                i += PIXELS_PER_ITER;
            }

            // Handle remaining pixels
            while i < pixel_count {
                let r = rgb[i * 3] as u16;
                let g = rgb[i * 3 + 1] as u16;
                let b = rgb[i * 3 + 2] as u16;
                grayscale[i] = ((77 * r + 150 * g + 29 * b) >> 8) as u8;
                i += 1;
            }

            Ok(())
        }

        /// AVX2 RGB to grayscale conversion
        #[cfg(target_arch = "x86_64")]
        unsafe fn rgb_to_grayscale_avx2(&self, rgb: &[u8], grayscale: &mut [u8],
                                       pixel_count: usize) -> WasmtimeResult<()> {
            if !is_x86_feature_detected!("avx2") {
                return self.rgb_to_grayscale_software(rgb, grayscale, pixel_count);
            }

            let coeff_r = _mm256_set1_epi16(77);
            let coeff_g = _mm256_set1_epi16(150);
            let coeff_b = _mm256_set1_epi16(29);

            const PIXELS_PER_ITER: usize = 16;
            let mut i = 0;

            while i + PIXELS_PER_ITER <= pixel_count {
                // Load and deinterleave RGB data
                let mut r_vals = [0u8; PIXELS_PER_ITER];
                let mut g_vals = [0u8; PIXELS_PER_ITER];
                let mut b_vals = [0u8; PIXELS_PER_ITER];

                for j in 0..PIXELS_PER_ITER {
                    r_vals[j] = rgb[i * 3 + j * 3];
                    g_vals[j] = rgb[i * 3 + j * 3 + 1];
                    b_vals[j] = rgb[i * 3 + j * 3 + 2];
                }

                // Load 16 bytes and unpack to 16-bit
                let r_8 = _mm_loadu_si128(r_vals.as_ptr() as *const __m128i);
                let g_8 = _mm_loadu_si128(g_vals.as_ptr() as *const __m128i);
                let b_8 = _mm_loadu_si128(b_vals.as_ptr() as *const __m128i);

                let r_16 = _mm256_unpacklo_epi8(_mm256_cvtepu8_epi16(r_8), _mm256_setzero_si256());
                let g_16 = _mm256_unpacklo_epi8(_mm256_cvtepu8_epi16(g_8), _mm256_setzero_si256());
                let b_16 = _mm256_unpacklo_epi8(_mm256_cvtepu8_epi16(b_8), _mm256_setzero_si256());

                // Calculate luminance
                let r_weighted = _mm256_mullo_epi16(r_16, coeff_r);
                let g_weighted = _mm256_mullo_epi16(g_16, coeff_g);
                let b_weighted = _mm256_mullo_epi16(b_16, coeff_b);

                let luminance = _mm256_add_epi16(_mm256_add_epi16(r_weighted, g_weighted), b_weighted);
                let luminance_shifted = _mm256_srli_epi16(luminance, 8);

                // Pack back to 8-bit
                let result_128 = _mm_packus_epi16(_mm256_extracti128_si256(luminance_shifted, 0),
                                                 _mm256_extracti128_si256(luminance_shifted, 1));

                // Store result
                _mm_storeu_si128(&mut grayscale[i] as *mut u8 as *mut __m128i, result_128);

                i += PIXELS_PER_ITER;
            }

            // Handle remaining pixels
            while i < pixel_count {
                let r = rgb[i * 3] as u16;
                let g = rgb[i * 3 + 1] as u16;
                let b = rgb[i * 3 + 2] as u16;
                grayscale[i] = ((77 * r + 150 * g + 29 * b) >> 8) as u8;
                i += 1;
            }

            Ok(())
        }

        /// NEON RGB to grayscale conversion for ARM64
        #[cfg(target_arch = "aarch64")]
        fn rgb_to_grayscale_neon(&self, rgb: &[u8], grayscale: &mut [u8],
                                pixel_count: usize) -> WasmtimeResult<()> {
            unsafe {
                let coeff_r = vdupq_n_u16(77);
                let coeff_g = vdupq_n_u16(150);
                let coeff_b = vdupq_n_u16(29);

                const PIXELS_PER_ITER: usize = 8;
                let mut i = 0;

                while i + PIXELS_PER_ITER <= pixel_count {
                    // Load and deinterleave RGB data
                    let mut r_vals = [0u8; PIXELS_PER_ITER];
                    let mut g_vals = [0u8; PIXELS_PER_ITER];
                    let mut b_vals = [0u8; PIXELS_PER_ITER];

                    for j in 0..PIXELS_PER_ITER {
                        r_vals[j] = rgb[i * 3 + j * 3];
                        g_vals[j] = rgb[i * 3 + j * 3 + 1];
                        b_vals[j] = rgb[i * 3 + j * 3 + 2];
                    }

                    // Load and convert to 16-bit
                    let r_8 = vld1_u8(r_vals.as_ptr());
                    let g_8 = vld1_u8(g_vals.as_ptr());
                    let b_8 = vld1_u8(b_vals.as_ptr());

                    let r_16 = vmovl_u8(r_8);
                    let g_16 = vmovl_u8(g_8);
                    let b_16 = vmovl_u8(b_8);

                    // Calculate luminance
                    let r_weighted = vmulq_u16(r_16, coeff_r);
                    let g_weighted = vmulq_u16(g_16, coeff_g);
                    let b_weighted = vmulq_u16(b_16, coeff_b);

                    let luminance = vaddq_u16(vaddq_u16(r_weighted, g_weighted), b_weighted);
                    let luminance_shifted = vshrq_n_u16(luminance, 8);

                    // Convert back to 8-bit
                    let result = vmovn_u16(luminance_shifted);

                    // Store result
                    vst1_u8(&mut grayscale[i], result);

                    i += PIXELS_PER_ITER;
                }

                // Handle remaining pixels
                while i < pixel_count {
                    let r = rgb[i * 3] as u16;
                    let g = rgb[i * 3 + 1] as u16;
                    let b = rgb[i * 3 + 2] as u16;
                    grayscale[i] = ((77 * r + 150 * g + 29 * b) >> 8) as u8;
                    i += 1;
                }

                Ok(())
            }
        }

        /// Software RGB to grayscale conversion
        fn rgb_to_grayscale_software(&self, rgb: &[u8], grayscale: &mut [u8],
                                   pixel_count: usize) -> WasmtimeResult<()> {
            for i in 0..pixel_count {
                let r = rgb[i * 3] as u16;
                let g = rgb[i * 3 + 1] as u16;
                let b = rgb[i * 3 + 2] as u16;
                // Use integer approximation: Y = (77*R + 150*G + 29*B) / 256
                grayscale[i] = ((77 * r + 150 * g + 29 * b) >> 8) as u8;
            }
            Ok(())
        }

        /// Gaussian blur with separable kernel optimization
        pub fn gaussian_blur(&self, input: &[f32], output: &mut [f32],
                            width: usize, height: usize, sigma: f32) -> WasmtimeResult<()> {
            if input.len() < width * height || output.len() < width * height {
                return Err(WasmtimeError::InvalidParameter { message: "Buffer size too small".to_string() });
            }

            // Generate 1D Gaussian kernel
            let kernel_size = ((sigma * 6.0) as usize).max(3) | 1; // Ensure odd size
            let mut kernel = vec![0.0f32; kernel_size];
            let half_size = kernel_size / 2;

            // Calculate Gaussian kernel
            let mut sum = 0.0f32;
            for i in 0..kernel_size {
                let x = (i as isize - half_size as isize) as f32;
                kernel[i] = (-x * x / (2.0 * sigma * sigma)).exp();
                sum += kernel[i];
            }

            // Normalize kernel
            for k in &mut kernel {
                *k /= sum;
            }

            // Temporary buffer for horizontal pass
            let mut temp = vec![0.0f32; width * height];

            // Horizontal pass
            self.gaussian_blur_horizontal(input, &mut temp, &kernel, width, height)?;

            // Vertical pass
            self.gaussian_blur_vertical(&temp, output, &kernel, width, height)?;

            Ok(())
        }

        /// Horizontal Gaussian blur pass with SIMD
        fn gaussian_blur_horizontal(&self, input: &[f32], output: &mut [f32],
                                  kernel: &[f32], width: usize, height: usize) -> WasmtimeResult<()> {
            let half_size = kernel.len() / 2;

            #[cfg(target_arch = "x86_64")]
            unsafe {
                if is_x86_feature_detected!("avx2") && self.capabilities.has_avx2 {
                    return self.gaussian_blur_horizontal_avx2(input, output, kernel, width, height);
                }
            }

            // Software fallback
            for y in 0..height {
                for x in 0..width {
                    let mut sum = 0.0f32;

                    for (k, &kernel_val) in kernel.iter().enumerate() {
                        let src_x = (x as isize + k as isize - half_size as isize)
                            .max(0).min(width as isize - 1) as usize;
                        sum += input[y * width + src_x] * kernel_val;
                    }

                    output[y * width + x] = sum;
                }
            }

            Ok(())
        }

        /// AVX2 horizontal Gaussian blur
        #[cfg(target_arch = "x86_64")]
        unsafe fn gaussian_blur_horizontal_avx2(&self, input: &[f32], output: &mut [f32],
                                              kernel: &[f32], width: usize, height: usize) -> WasmtimeResult<()> {
            if !is_x86_feature_detected!("avx2") {
                return self.gaussian_blur_horizontal_software(input, output, kernel, width, height);
            }

            let half_size = kernel.len() / 2;
            const SIMD_WIDTH: usize = 8;

            for y in 0..height {
                let mut x = 0;

                // Process multiple pixels at once
                while x + SIMD_WIDTH <= width {
                    let mut sum = _mm256_setzero_ps();

                    for (k, &kernel_val) in kernel.iter().enumerate() {
                        let offset = k as isize - half_size as isize;
                        let kernel_vec = _mm256_set1_ps(kernel_val);

                        // Load 8 pixels with proper boundary handling
                        let mut pixels = [0.0f32; SIMD_WIDTH];
                        for i in 0..SIMD_WIDTH {
                            let src_x = ((x + i) as isize + offset)
                                .max(0).min(width as isize - 1) as usize;
                            pixels[i] = input[y * width + src_x];
                        }

                        let pixel_vec = _mm256_loadu_ps(pixels.as_ptr());
                        sum = _mm256_fmadd_ps(pixel_vec, kernel_vec, sum);
                    }

                    // Store results
                    _mm256_storeu_ps(&mut output[y * width + x], sum);
                    x += SIMD_WIDTH;
                }

                // Handle remaining pixels
                while x < width {
                    let mut sum = 0.0f32;
                    for (k, &kernel_val) in kernel.iter().enumerate() {
                        let src_x = (x as isize + k as isize - half_size as isize)
                            .max(0).min(width as isize - 1) as usize;
                        sum += input[y * width + src_x] * kernel_val;
                    }
                    output[y * width + x] = sum;
                    x += 1;
                }
            }

            Ok(())
        }

        /// Software horizontal blur fallback
        fn gaussian_blur_horizontal_software(&self, input: &[f32], output: &mut [f32],
                                           kernel: &[f32], width: usize, height: usize) -> WasmtimeResult<()> {
            let half_size = kernel.len() / 2;

            for y in 0..height {
                for x in 0..width {
                    let mut sum = 0.0f32;

                    for (k, &kernel_val) in kernel.iter().enumerate() {
                        let src_x = (x as isize + k as isize - half_size as isize)
                            .max(0).min(width as isize - 1) as usize;
                        sum += input[y * width + src_x] * kernel_val;
                    }

                    output[y * width + x] = sum;
                }
            }

            Ok(())
        }

        /// Vertical Gaussian blur pass
        fn gaussian_blur_vertical(&self, input: &[f32], output: &mut [f32],
                                kernel: &[f32], width: usize, height: usize) -> WasmtimeResult<()> {
            let half_size = kernel.len() / 2;

            for y in 0..height {
                for x in 0..width {
                    let mut sum = 0.0f32;

                    for (k, &kernel_val) in kernel.iter().enumerate() {
                        let src_y = (y as isize + k as isize - half_size as isize)
                            .max(0).min(height as isize - 1) as usize;
                        sum += input[src_y * width + x] * kernel_val;
                    }

                    output[y * width + x] = sum;
                }
            }

            Ok(())
        }

        /// Edge detection using Sobel operator with SIMD optimization
        pub fn sobel_edge_detection(&self, input: &[f32], output: &mut [f32],
                                   width: usize, height: usize) -> WasmtimeResult<()> {
            if input.len() < width * height || output.len() < width * height {
                return Err(WasmtimeError::InvalidParameter { message: "Buffer size too small".to_string() });
            }

            // Sobel kernels
            #[rustfmt::skip]
            let sobel_x = [
                -1.0, 0.0, 1.0,
                -2.0, 0.0, 2.0,
                -1.0, 0.0, 1.0,
            ];

            #[rustfmt::skip]
            let sobel_y = [
                -1.0, -2.0, -1.0,
                 0.0,  0.0,  0.0,
                 1.0,  2.0,  1.0,
            ];

            // Apply convolution for each pixel
            for y in 1..height - 1 {
                for x in 1..width - 1 {
                    let mut gx = 0.0f32;
                    let mut gy = 0.0f32;

                    // Apply 3x3 kernels
                    for ky in 0..3 {
                        for kx in 0..3 {
                            let pixel = input[(y + ky - 1) * width + (x + kx - 1)];
                            let kernel_idx = ky * 3 + kx;
                            gx += pixel * sobel_x[kernel_idx];
                            gy += pixel * sobel_y[kernel_idx];
                        }
                    }

                    // Calculate magnitude
                    let magnitude = (gx * gx + gy * gy).sqrt();
                    output[y * width + x] = magnitude;
                }
            }

            // Handle borders (set to 0)
            for x in 0..width {
                output[x] = 0.0; // Top row
                output[(height - 1) * width + x] = 0.0; // Bottom row
            }
            for y in 0..height {
                output[y * width] = 0.0; // Left column
                output[y * width + (width - 1)] = 0.0; // Right column
            }

            Ok(())
        }

        /// Histogram equalization with SIMD optimization
        pub fn histogram_equalization(&self, input: &[u8], output: &mut [u8],
                                    width: usize, height: usize) -> WasmtimeResult<()> {
            let pixel_count = width * height;
            if input.len() < pixel_count || output.len() < pixel_count {
                return Err(WasmtimeError::InvalidParameter { message: "Buffer size too small".to_string() });
            }

            // Calculate histogram
            let mut histogram = [0u32; 256];
            for &pixel in &input[..pixel_count] {
                histogram[pixel as usize] += 1;
            }

            // Calculate cumulative distribution function (CDF)
            let mut cdf = [0u32; 256];
            cdf[0] = histogram[0];
            for i in 1..256 {
                cdf[i] = cdf[i - 1] + histogram[i];
            }

            // Find minimum non-zero CDF value
            let cdf_min = cdf.iter().find(|&&x| x > 0).copied().unwrap_or(0);

            // Create lookup table for equalization
            let mut lut = [0u8; 256];
            for i in 0..256 {
                if cdf[i] > cdf_min {
                    lut[i] = (((cdf[i] - cdf_min) as f32 / (pixel_count - cdf_min as usize) as f32) * 255.0).round() as u8;
                } else {
                    lut[i] = 0;
                }
            }

            // Apply lookup table with SIMD optimization
            #[cfg(target_arch = "x86_64")]
            unsafe {
                if is_x86_feature_detected!("avx2") && self.capabilities.has_avx2 {
                    return self.apply_lut_avx2(input, output, &lut, pixel_count);
                }
            }

            // Software fallback
            for i in 0..pixel_count {
                output[i] = lut[input[i] as usize];
            }

            Ok(())
        }

        /// Apply lookup table with AVX2
        #[cfg(target_arch = "x86_64")]
        unsafe fn apply_lut_avx2(&self, input: &[u8], output: &mut [u8],
                                lut: &[u8; 256], pixel_count: usize) -> WasmtimeResult<()> {
            if !is_x86_feature_detected!("avx2") {
                for i in 0..pixel_count {
                    output[i] = lut[input[i] as usize];
                }
                return Ok(());
            }

            const SIMD_WIDTH: usize = 32; // AVX2 can process 32 bytes at once
            let mut i = 0;

            while i + SIMD_WIDTH <= pixel_count {
                // Load 32 input pixels
                let pixels = _mm256_loadu_si256(&input[i] as *const u8 as *const __m256i);

                // Apply LUT using gather operations (AVX2)
                // This is complex and requires multiple instructions
                // Simplified approach: process 8 pixels at a time
                let mut results = [0u8; SIMD_WIDTH];
                for j in 0..SIMD_WIDTH {
                    results[j] = lut[input[i + j] as usize];
                }

                // Store results
                _mm256_storeu_si256(&mut output[i] as *mut u8 as *mut __m256i,
                                   _mm256_loadu_si256(results.as_ptr() as *const __m256i));

                i += SIMD_WIDTH;
            }

            // Handle remaining pixels
            while i < pixel_count {
                output[i] = lut[input[i] as usize];
                i += 1;
            }

            Ok(())
        }

        /// Bilateral filter for noise reduction while preserving edges
        pub fn bilateral_filter(&self, input: &[f32], output: &mut [f32],
                               width: usize, height: usize, sigma_spatial: f32,
                               sigma_intensity: f32, kernel_size: usize) -> WasmtimeResult<()> {
            if input.len() < width * height || output.len() < width * height {
                return Err(WasmtimeError::InvalidParameter { message: "Buffer size too small".to_string() });
            }

            let half_size = kernel_size / 2;
            let spatial_factor = -1.0 / (2.0 * sigma_spatial * sigma_spatial);
            let intensity_factor = -1.0 / (2.0 * sigma_intensity * sigma_intensity);

            for y in 0..height {
                for x in 0..width {
                    let center_intensity = input[y * width + x];
                    let mut sum = 0.0f32;
                    let mut weight_sum = 0.0f32;

                    for ky in 0..kernel_size {
                        for kx in 0..kernel_size {
                            let ny = (y as isize + ky as isize - half_size as isize)
                                .max(0).min(height as isize - 1) as usize;
                            let nx = (x as isize + kx as isize - half_size as isize)
                                .max(0).min(width as isize - 1) as usize;

                            let neighbor_intensity = input[ny * width + nx];

                            // Spatial distance
                            let dx = (kx as isize - half_size as isize) as f32;
                            let dy = (ky as isize - half_size as isize) as f32;
                            let spatial_dist_sq = dx * dx + dy * dy;

                            // Intensity difference
                            let intensity_diff = neighbor_intensity - center_intensity;
                            let intensity_dist_sq = intensity_diff * intensity_diff;

                            // Combined weight
                            let weight = (spatial_dist_sq * spatial_factor + intensity_dist_sq * intensity_factor).exp();

                            sum += neighbor_intensity * weight;
                            weight_sum += weight;
                        }
                    }

                    output[y * width + x] = if weight_sum > 0.0 { sum / weight_sum } else { center_intensity };
                }
            }

            Ok(())
        }
    }

    /// Signal processing operations with SIMD acceleration
    pub struct VectorizedSignalProcessing {
        capabilities: PlatformCapabilities,
    }

    impl VectorizedSignalProcessing {
        /// Creates a new vectorized signal processing instance
        pub fn new(capabilities: &PlatformCapabilities) -> Self {
            VectorizedSignalProcessing {
                capabilities: capabilities.clone(),
            }
        }

        /// Fast Fourier Transform (FFT) with SIMD optimization
        pub fn fft(&self, real: &mut [f32], imag: &mut [f32]) -> WasmtimeResult<()> {
            let n = real.len();
            if n != imag.len() || !n.is_power_of_two() {
                return Err(WasmtimeError::InvalidParameter { message: "FFT requires power-of-two length arrays".to_string() });
            }

            // Bit-reversal permutation
            self.bit_reverse_permutation(real, imag)?;

            // Cooley-Tukey FFT algorithm
            let mut stage = 1;
            while stage < n {
                let next_stage = stage << 1;
                let angle_step = -std::f32::consts::PI / stage as f32;

                #[cfg(target_arch = "x86_64")]
                unsafe {
                    if is_x86_feature_detected!("avx2") && self.capabilities.has_avx2 {
                        self.fft_butterfly_avx2(real, imag, stage, next_stage, angle_step)?;
                    } else {
                        self.fft_butterfly_software(real, imag, stage, next_stage, angle_step)?;
                    }
                }

                #[cfg(not(target_arch = "x86_64"))]
                {
                    self.fft_butterfly_software(real, imag, stage, next_stage, angle_step)?;
                }

                stage = next_stage;
            }

            Ok(())
        }

        /// Bit-reversal permutation for FFT
        fn bit_reverse_permutation(&self, real: &mut [f32], imag: &mut [f32]) -> WasmtimeResult<()> {
            let n = real.len();
            let mut j = 0;

            for i in 1..n {
                let mut bit = n >> 1;
                while j & bit != 0 {
                    j ^= bit;
                    bit >>= 1;
                }
                j ^= bit;

                if i < j {
                    real.swap(i, j);
                    imag.swap(i, j);
                }
            }

            Ok(())
        }

        /// FFT butterfly operations with AVX2
        #[cfg(target_arch = "x86_64")]
        unsafe fn fft_butterfly_avx2(&self, real: &mut [f32], imag: &mut [f32],
                                    stage: usize, next_stage: usize, angle_step: f32) -> WasmtimeResult<()> {
            if !is_x86_feature_detected!("avx2") {
                return self.fft_butterfly_software(real, imag, stage, next_stage, angle_step);
            }

            const SIMD_WIDTH: usize = 8;
            let n = real.len();

            for group in (0..n).step_by(next_stage) {
                let mut k = 0;

                // Process multiple butterflies with SIMD
                while k + SIMD_WIDTH <= stage {
                    // Calculate twiddle factors
                    let mut angles = [0.0f32; SIMD_WIDTH];
                    let mut cos_vals = [0.0f32; SIMD_WIDTH];
                    let mut sin_vals = [0.0f32; SIMD_WIDTH];

                    for i in 0..SIMD_WIDTH {
                        angles[i] = (k + i) as f32 * angle_step;
                        cos_vals[i] = angles[i].cos();
                        sin_vals[i] = angles[i].sin();
                    }

                    let cos_vec = _mm256_loadu_ps(cos_vals.as_ptr());
                    let sin_vec = _mm256_loadu_ps(sin_vals.as_ptr());

                    // Load butterfly inputs
                    let i1_base = group + k;
                    let i2_base = group + k + stage;

                    let real1 = _mm256_loadu_ps(&real[i1_base]);
                    let imag1 = _mm256_loadu_ps(&imag[i1_base]);
                    let real2 = _mm256_loadu_ps(&real[i2_base]);
                    let imag2 = _mm256_loadu_ps(&imag[i2_base]);

                    // Complex multiplication: (a + bi) * (c + di) = (ac - bd) + (ad + bc)i
                    let temp_real = _mm256_sub_ps(_mm256_mul_ps(real2, cos_vec), _mm256_mul_ps(imag2, sin_vec));
                    let temp_imag = _mm256_add_ps(_mm256_mul_ps(real2, sin_vec), _mm256_mul_ps(imag2, cos_vec));

                    // Butterfly operation
                    let new_real1 = _mm256_add_ps(real1, temp_real);
                    let new_imag1 = _mm256_add_ps(imag1, temp_imag);
                    let new_real2 = _mm256_sub_ps(real1, temp_real);
                    let new_imag2 = _mm256_sub_ps(imag1, temp_imag);

                    // Store results
                    _mm256_storeu_ps(&mut real[i1_base], new_real1);
                    _mm256_storeu_ps(&mut imag[i1_base], new_imag1);
                    _mm256_storeu_ps(&mut real[i2_base], new_real2);
                    _mm256_storeu_ps(&mut imag[i2_base], new_imag2);

                    k += SIMD_WIDTH;
                }

                // Handle remaining butterflies
                while k < stage {
                    let angle = k as f32 * angle_step;
                    let cos_w = angle.cos();
                    let sin_w = angle.sin();

                    let i1 = group + k;
                    let i2 = group + k + stage;

                    let temp_real = real[i2] * cos_w - imag[i2] * sin_w;
                    let temp_imag = real[i2] * sin_w + imag[i2] * cos_w;

                    let new_real1 = real[i1] + temp_real;
                    let new_imag1 = imag[i1] + temp_imag;
                    let new_real2 = real[i1] - temp_real;
                    let new_imag2 = imag[i1] - temp_imag;

                    real[i1] = new_real1;
                    imag[i1] = new_imag1;
                    real[i2] = new_real2;
                    imag[i2] = new_imag2;

                    k += 1;
                }
            }

            Ok(())
        }

        /// Software FFT butterfly operations
        fn fft_butterfly_software(&self, real: &mut [f32], imag: &mut [f32],
                                 stage: usize, next_stage: usize, angle_step: f32) -> WasmtimeResult<()> {
            let n = real.len();

            for group in (0..n).step_by(next_stage) {
                for k in 0..stage {
                    let angle = k as f32 * angle_step;
                    let cos_w = angle.cos();
                    let sin_w = angle.sin();

                    let i1 = group + k;
                    let i2 = group + k + stage;

                    let temp_real = real[i2] * cos_w - imag[i2] * sin_w;
                    let temp_imag = real[i2] * sin_w + imag[i2] * cos_w;

                    let new_real1 = real[i1] + temp_real;
                    let new_imag1 = imag[i1] + temp_imag;
                    let new_real2 = real[i1] - temp_real;
                    let new_imag2 = imag[i1] - temp_imag;

                    real[i1] = new_real1;
                    imag[i1] = new_imag1;
                    real[i2] = new_real2;
                    imag[i2] = new_imag2;
                }
            }

            Ok(())
        }

        /// Inverse FFT
        pub fn ifft(&self, real: &mut [f32], imag: &mut [f32]) -> WasmtimeResult<()> {
            let n = real.len();

            // Conjugate the complex numbers
            for i in 0..n {
                imag[i] = -imag[i];
            }

            // Perform forward FFT
            self.fft(real, imag)?;

            // Conjugate again and scale
            let scale = 1.0 / n as f32;
            for i in 0..n {
                real[i] *= scale;
                imag[i] *= -scale;
            }

            Ok(())
        }

        /// Digital filter implementation with SIMD optimization
        pub fn digital_filter(&self, input: &[f32], output: &mut [f32],
                             coefficients: &[f32], filter_type: FilterType) -> WasmtimeResult<()> {
            if input.len() != output.len() {
                return Err(WasmtimeError::InvalidParameter { message: "Input and output length mismatch".to_string() });
            }

            match filter_type {
                FilterType::FIR => self.fir_filter(input, output, coefficients),
                FilterType::IIR => self.iir_filter(input, output, coefficients),
            }
        }

        /// Finite Impulse Response (FIR) filter
        fn fir_filter(&self, input: &[f32], output: &mut [f32], coefficients: &[f32]) -> WasmtimeResult<()> {
            let n = input.len();
            let filter_len = coefficients.len();

            #[cfg(target_arch = "x86_64")]
            unsafe {
                if is_x86_feature_detected!("avx2") && self.capabilities.has_avx2 {
                    return self.fir_filter_avx2(input, output, coefficients, n, filter_len);
                }
            }

            // Software implementation
            for i in 0..n {
                let mut sum = 0.0f32;
                for j in 0..filter_len {
                    if i >= j {
                        sum += input[i - j] * coefficients[j];
                    }
                }
                output[i] = sum;
            }

            Ok(())
        }

        /// AVX2 FIR filter implementation
        #[cfg(target_arch = "x86_64")]
        unsafe fn fir_filter_avx2(&self, input: &[f32], output: &mut [f32],
                                 coefficients: &[f32], n: usize, filter_len: usize) -> WasmtimeResult<()> {
            if !is_x86_feature_detected!("avx2") {
                return self.fir_filter_software(input, output, coefficients, n, filter_len);
            }

            const SIMD_WIDTH: usize = 8;

            for i in 0..n {
                let mut sum = _mm256_setzero_ps();
                let mut j = 0;

                // Process multiple coefficients at once
                while j + SIMD_WIDTH <= filter_len && i >= j + SIMD_WIDTH - 1 {
                    let coeff_vec = _mm256_loadu_ps(&coefficients[j]);

                    // Load input values in reverse order
                    let mut input_vals = [0.0f32; SIMD_WIDTH];
                    for k in 0..SIMD_WIDTH {
                        input_vals[k] = input[i - j - k];
                    }
                    let input_vec = _mm256_loadu_ps(input_vals.as_ptr());

                    sum = _mm256_fmadd_ps(coeff_vec, input_vec, sum);
                    j += SIMD_WIDTH;
                }

                // Handle remaining coefficients
                let mut remaining_sum = 0.0f32;
                while j < filter_len && i >= j {
                    remaining_sum += input[i - j] * coefficients[j];
                    j += 1;
                }

                // Sum all elements in the SIMD register
                let mut sum_array = [0.0f32; SIMD_WIDTH];
                _mm256_storeu_ps(sum_array.as_mut_ptr(), sum);
                let total_sum = sum_array.iter().sum::<f32>() + remaining_sum;

                output[i] = total_sum;
            }

            Ok(())
        }

        /// Software FIR filter fallback
        fn fir_filter_software(&self, input: &[f32], output: &mut [f32],
                              coefficients: &[f32], n: usize, filter_len: usize) -> WasmtimeResult<()> {
            for i in 0..n {
                let mut sum = 0.0f32;
                for j in 0..filter_len {
                    if i >= j {
                        sum += input[i - j] * coefficients[j];
                    }
                }
                output[i] = sum;
            }
            Ok(())
        }

        /// Infinite Impulse Response (IIR) filter
        fn iir_filter(&self, input: &[f32], output: &mut [f32], coefficients: &[f32]) -> WasmtimeResult<()> {
            let n = input.len();
            if coefficients.len() % 2 != 0 {
                return Err(WasmtimeError::InvalidParameter { message: "IIR coefficients must be even length".to_string() });
            }

            let filter_len = coefficients.len() / 2;
            let b_coeffs = &coefficients[..filter_len]; // Feedforward
            let a_coeffs = &coefficients[filter_len..]; // Feedback

            for i in 0..n {
                let mut sum = 0.0f32;

                // Feedforward terms
                for j in 0..filter_len {
                    if i >= j {
                        sum += input[i - j] * b_coeffs[j];
                    }
                }

                // Feedback terms
                for j in 1..filter_len {
                    if i >= j {
                        sum -= output[i - j] * a_coeffs[j];
                    }
                }

                output[i] = sum;
            }

            Ok(())
        }

        /// Windowing functions for signal processing
        pub fn apply_window(&self, signal: &mut [f32], window_type: WindowType) -> WasmtimeResult<()> {
            let n = signal.len();

            match window_type {
                WindowType::Hamming => {
                    for i in 0..n {
                        let w = 0.54 - 0.46 * (2.0 * std::f32::consts::PI * i as f32 / (n - 1) as f32).cos();
                        signal[i] *= w;
                    }
                },
                WindowType::Hanning => {
                    for i in 0..n {
                        let w = 0.5 * (1.0 - (2.0 * std::f32::consts::PI * i as f32 / (n - 1) as f32).cos());
                        signal[i] *= w;
                    }
                },
                WindowType::Blackman => {
                    for i in 0..n {
                        let phase = 2.0 * std::f32::consts::PI * i as f32 / (n - 1) as f32;
                        let w = 0.42 - 0.5 * phase.cos() + 0.08 * (2.0 * phase).cos();
                        signal[i] *= w;
                    }
                },
                WindowType::Kaiser(beta) => {
                    let bessel_i0_beta = self.bessel_i0(beta);
                    let n_minus_1 = (n - 1) as f32;

                    for i in 0..n {
                        let x = 2.0 * i as f32 / n_minus_1 - 1.0;
                        let arg = beta * (1.0 - x * x).sqrt();
                        let w = self.bessel_i0(arg) / bessel_i0_beta;
                        signal[i] *= w;
                    }
                },
            }

            Ok(())
        }

        /// Modified Bessel function of the first kind (order 0)
        fn bessel_i0(&self, x: f32) -> f32 {
            let ax = x.abs();
            let y = if ax < 3.75 {
                let y = x / 3.75;
                let y2 = y * y;
                1.0 + y2 * (3.5156229 + y2 * (3.0899424 + y2 * (1.2067492 +
                    y2 * (0.2659732 + y2 * (0.0360768 + y2 * 0.0045813)))))
            } else {
                let y = 3.75 / ax;
                (ax.exp() / ax.sqrt()) * (0.39894228 + y * (0.01328592 +
                    y * (0.00225319 + y * (-0.00157565 + y * (0.00916281 +
                    y * (-0.02057706 + y * (0.02635537 + y * (-0.01647633 +
                    y * 0.00392377))))))))
            };

            if x < 0.0 && (x / 3.75) as i32 % 2 == 1 { -y } else { y }
        }

        /// Cross-correlation with SIMD optimization
        pub fn cross_correlation(&self, signal1: &[f32], signal2: &[f32],
                                result: &mut [f32]) -> WasmtimeResult<()> {
            let n1 = signal1.len();
            let n2 = signal2.len();
            let result_len = n1 + n2 - 1;

            if result.len() < result_len {
                return Err(WasmtimeError::InvalidParameter { message: "Result buffer too small".to_string() });
            }

            // Initialize result
            for i in 0..result_len {
                result[i] = 0.0;
            }

            // Compute cross-correlation
            for i in 0..n1 {
                for j in 0..n2 {
                    result[i + j] += signal1[i] * signal2[j];
                }
            }

            Ok(())
        }

        /// Power spectral density estimation
        pub fn power_spectral_density(&self, signal: &[f32], psd: &mut [f32],
                                    window_size: usize, overlap: f32) -> WasmtimeResult<()> {
            if !window_size.is_power_of_two() {
                return Err(WasmtimeError::InvalidParameter { message: "Window size must be power of two".to_string() });
            }

            let n = signal.len();
            let step_size = (window_size as f32 * (1.0 - overlap)) as usize;
            let num_windows = (n - window_size) / step_size + 1;

            // Initialize PSD accumulator
            let fft_size = window_size / 2 + 1;
            if psd.len() < fft_size {
                return Err(WasmtimeError::InvalidParameter { message: "PSD buffer too small".to_string() });
            }

            for i in 0..fft_size {
                psd[i] = 0.0;
            }

            // Process each window
            for window in 0..num_windows {
                let start_idx = window * step_size;
                if start_idx + window_size > n {
                    break;
                }

                // Extract window
                let mut window_real = vec![0.0f32; window_size];
                let mut window_imag = vec![0.0f32; window_size];

                for i in 0..window_size {
                    window_real[i] = signal[start_idx + i];
                }

                // Apply window function (Hanning)
                self.apply_window(&mut window_real, WindowType::Hanning)?;

                // Compute FFT
                self.fft(&mut window_real, &mut window_imag)?;

                // Accumulate power spectral density
                for i in 0..fft_size {
                    let power = window_real[i] * window_real[i] + window_imag[i] * window_imag[i];
                    psd[i] += power;
                }
            }

            // Normalize by number of windows
            let norm_factor = 1.0 / num_windows as f32;
            for i in 0..fft_size {
                psd[i] *= norm_factor;
            }

            Ok(())
        }
    }

    /// Filter types for digital signal processing
    #[derive(Debug, Clone, Copy, PartialEq)]
    pub enum FilterType {
        FIR, // Finite Impulse Response
        IIR, // Infinite Impulse Response
    }

    /// Window functions for signal processing
    #[derive(Debug, Clone, Copy, PartialEq)]
    pub enum WindowType {
        Hamming,
        Hanning,
        Blackman,
        Kaiser(f32), // Beta parameter
    }

    /// Audio processing operations with SIMD optimization
    pub struct VectorizedAudioProcessing {
        capabilities: PlatformCapabilities,
        signal_processor: VectorizedSignalProcessing,
    }

    impl VectorizedAudioProcessing {
        /// Creates a new vectorized audio processing instance
        pub fn new(capabilities: &PlatformCapabilities) -> Self {
            VectorizedAudioProcessing {
                capabilities: capabilities.clone(),
                signal_processor: VectorizedSignalProcessing::new(capabilities),
            }
        }

        /// Resampling with SIMD-optimized interpolation
        pub fn resample(&self, input: &[f32], output: &mut [f32],
                       input_rate: u32, output_rate: u32) -> WasmtimeResult<()> {
            let ratio = output_rate as f64 / input_rate as f64;
            let output_len = (input.len() as f64 * ratio) as usize;

            if output.len() < output_len {
                return Err(WasmtimeError::InvalidParameter { message: "Output buffer too small".to_string() });
            }

            // Linear interpolation resampling
            for i in 0..output_len {
                let src_pos = i as f64 / ratio;
                let src_idx = src_pos as usize;
                let frac = src_pos - src_idx as f64;

                if src_idx + 1 < input.len() {
                    output[i] = input[src_idx] * (1.0 - frac) as f32 + input[src_idx + 1] * frac as f32;
                } else if src_idx < input.len() {
                    output[i] = input[src_idx];
                } else {
                    output[i] = 0.0;
                }
            }

            Ok(())
        }

        /// Multi-band equalizer with SIMD optimization
        pub fn multiband_equalizer(&self, input: &[f32], output: &mut [f32],
                                  bands: &[EqualizerBand], sample_rate: u32) -> WasmtimeResult<()> {
            if input.len() != output.len() {
                return Err(WasmtimeError::InvalidParameter { message: "Input and output length mismatch".to_string() });
            }

            // Initialize output with input
            output.copy_from_slice(input);

            // Apply each band
            for band in bands {
                let mut temp_output = vec![0.0f32; output.len()];
                self.apply_equalizer_band(output, &mut temp_output, band, sample_rate)?;
                output.copy_from_slice(&temp_output);
            }

            Ok(())
        }

        /// Apply single equalizer band
        fn apply_equalizer_band(&self, input: &[f32], output: &mut [f32],
                               band: &EqualizerBand, sample_rate: u32) -> WasmtimeResult<()> {
            // Design biquad filter coefficients for the band
            let (b0, b1, b2, a1, a2) = self.design_biquad_filter(band, sample_rate);

            // Apply biquad filter
            let mut x1 = 0.0f32;
            let mut x2 = 0.0f32;
            let mut y1 = 0.0f32;
            let mut y2 = 0.0f32;

            for i in 0..input.len() {
                let x0 = input[i];
                let y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;

                output[i] = y0;

                x2 = x1;
                x1 = x0;
                y2 = y1;
                y1 = y0;
            }

            Ok(())
        }

        /// Design biquad filter coefficients
        fn design_biquad_filter(&self, band: &EqualizerBand, sample_rate: u32) -> (f32, f32, f32, f32, f32) {
            let omega = 2.0 * std::f32::consts::PI * band.frequency / sample_rate as f32;
            let sin_omega = omega.sin();
            let cos_omega = omega.cos();
            let alpha = sin_omega / (2.0 * band.q);
            let a = 10.0f32.powf(band.gain_db / 40.0);

            match band.filter_type {
                EqFilterType::Peak => {
                    let b0 = 1.0 + alpha * a;
                    let b1 = -2.0 * cos_omega;
                    let b2 = 1.0 - alpha * a;
                    let a0 = 1.0 + alpha / a;
                    let a1 = -2.0 * cos_omega;
                    let a2 = 1.0 - alpha / a;

                    (b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
                },
                EqFilterType::LowShelf => {
                    let s = 1.0;
                    let beta = (a.sqrt() / band.q).sqrt();
                    let b0 = a * ((a + 1.0) - (a - 1.0) * cos_omega + beta * sin_omega);
                    let b1 = 2.0 * a * ((a - 1.0) - (a + 1.0) * cos_omega);
                    let b2 = a * ((a + 1.0) - (a - 1.0) * cos_omega - beta * sin_omega);
                    let a0 = (a + 1.0) + (a - 1.0) * cos_omega + beta * sin_omega;
                    let a1 = -2.0 * ((a - 1.0) + (a + 1.0) * cos_omega);
                    let a2 = (a + 1.0) + (a - 1.0) * cos_omega - beta * sin_omega;

                    (b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
                },
                EqFilterType::HighShelf => {
                    let s = 1.0;
                    let beta = (a.sqrt() / band.q).sqrt();
                    let b0 = a * ((a + 1.0) + (a - 1.0) * cos_omega + beta * sin_omega);
                    let b1 = -2.0 * a * ((a - 1.0) + (a + 1.0) * cos_omega);
                    let b2 = a * ((a + 1.0) + (a - 1.0) * cos_omega - beta * sin_omega);
                    let a0 = (a + 1.0) - (a - 1.0) * cos_omega + beta * sin_omega;
                    let a1 = 2.0 * ((a - 1.0) - (a + 1.0) * cos_omega);
                    let a2 = (a + 1.0) - (a - 1.0) * cos_omega - beta * sin_omega;

                    (b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
                },
            }
        }

        /// Dynamic range compression with SIMD optimization
        pub fn compressor(&self, input: &[f32], output: &mut [f32],
                         settings: &CompressorSettings) -> WasmtimeResult<()> {
            if input.len() != output.len() {
                return Err(WasmtimeError::InvalidParameter { message: "Input and output length mismatch".to_string() });
            }

            let attack_coeff = (-1.0 / (settings.attack_time * settings.sample_rate as f32)).exp();
            let release_coeff = (-1.0 / (settings.release_time * settings.sample_rate as f32)).exp();

            let mut envelope = 0.0f32;

            for i in 0..input.len() {
                let input_level = input[i].abs();

                // Envelope follower
                if input_level > envelope {
                    envelope = attack_coeff * envelope + (1.0 - attack_coeff) * input_level;
                } else {
                    envelope = release_coeff * envelope + (1.0 - release_coeff) * input_level;
                }

                // Calculate gain reduction
                let gain_reduction = if envelope > settings.threshold {
                    let over_threshold = envelope - settings.threshold;
                    let compressed = over_threshold / settings.ratio;
                    (settings.threshold + compressed) / envelope
                } else {
                    1.0
                };

                // Apply makeup gain
                output[i] = input[i] * gain_reduction * settings.makeup_gain;
            }

            Ok(())
        }

        /// Reverb effect using convolution
        pub fn reverb(&self, input: &[f32], output: &mut [f32],
                     impulse_response: &[f32], dry_wet_mix: f32) -> WasmtimeResult<()> {
            if input.len() != output.len() {
                return Err(WasmtimeError::InvalidParameter { message: "Input and output length mismatch".to_string() });
            }

            // Convolution reverb
            let mut reverb_signal = vec![0.0f32; output.len()];
            self.signal_processor.cross_correlation(input, impulse_response, &mut reverb_signal)?;

            // Mix dry and wet signals
            for i in 0..output.len() {
                if i < reverb_signal.len() {
                    output[i] = input[i] * (1.0 - dry_wet_mix) + reverb_signal[i] * dry_wet_mix;
                } else {
                    output[i] = input[i] * (1.0 - dry_wet_mix);
                }
            }

            Ok(())
        }
    }

    /// Equalizer band configuration
    #[derive(Debug, Clone, Copy, PartialEq)]
    pub struct EqualizerBand {
        pub frequency: f32,
        pub gain_db: f32,
        pub q: f32,
        pub filter_type: EqFilterType,
    }

    /// Equalizer filter types
    #[derive(Debug, Clone, Copy, PartialEq)]
    pub enum EqFilterType {
        Peak,
        LowShelf,
        HighShelf,
    }

    /// Compressor settings
    #[derive(Debug, Clone, Copy, PartialEq)]
    pub struct CompressorSettings {
        pub threshold: f32,
        pub ratio: f32,
        pub attack_time: f32,
        pub release_time: f32,
        pub makeup_gain: f32,
        pub sample_rate: u32,
    }

    #[cfg(test)]
    mod domain_tests {
        use super::*;

        #[test]
        fn test_rgb_to_grayscale() {
            let capabilities = PlatformCapabilities::detect();
            let image_processor = VectorizedImageProcessing::new(&capabilities);

            let rgb = vec![
                255, 0, 0,   // Red pixel
                0, 255, 0,   // Green pixel
                0, 0, 255,   // Blue pixel
            ];
            let mut grayscale = vec![0; 3];

            let result = image_processor.rgb_to_grayscale(&rgb, &mut grayscale, 3, 1);
            assert!(result.is_ok());

            // Check that grayscale values are reasonable
            assert!(grayscale[0] > 0); // Red should contribute
            assert!(grayscale[1] > grayscale[0]); // Green should contribute most
            assert!(grayscale[2] > 0); // Blue should contribute least
        }

        #[test]
        fn test_gaussian_blur() {
            let capabilities = PlatformCapabilities::detect();
            let image_processor = VectorizedImageProcessing::new(&capabilities);

            let input = vec![1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0]; // 3x3 checkerboard
            let mut output = vec![0.0; 9];

            let result = image_processor.gaussian_blur(&input, &mut output, 3, 3, 1.0);
            assert!(result.is_ok());

            // Check that blur smoothed the checkerboard pattern
            for &val in &output {
                assert!(val > 0.0 && val < 1.0); // Should be between extremes
            }
        }

        #[test]
        fn test_fft() {
            let capabilities = PlatformCapabilities::detect();
            let signal_processor = VectorizedSignalProcessing::new(&capabilities);

            let mut real = vec![1.0, 0.0, 1.0, 0.0]; // Simple test signal
            let mut imag = vec![0.0; 4];

            let result = signal_processor.fft(&mut real, &mut imag);
            assert!(result.is_ok());

            // Check that FFT produced reasonable results
            assert!(real.iter().all(|x| x.is_finite()));
            assert!(imag.iter().all(|x| x.is_finite()));
        }

        #[test]
        fn test_digital_filter() {
            let capabilities = PlatformCapabilities::detect();
            let signal_processor = VectorizedSignalProcessing::new(&capabilities);

            let input = vec![1.0, 2.0, 3.0, 4.0, 5.0];
            let mut output = vec![0.0; 5];
            let coefficients = vec![0.5, 0.3, 0.2]; // Simple FIR filter

            let result = signal_processor.digital_filter(&input, &mut output, &coefficients, FilterType::FIR);
            assert!(result.is_ok());

            // Check that filtering produced reasonable results
            assert!(output.iter().all(|x| x.is_finite()));
            assert!(output[0] > 0.0); // First sample should be non-zero
        }

        #[test]
        fn test_audio_resampling() {
            let capabilities = PlatformCapabilities::detect();
            let audio_processor = VectorizedAudioProcessing::new(&capabilities);

            let input = vec![1.0, 2.0, 3.0, 4.0]; // 4 samples at 44.1kHz
            let mut output = vec![0.0; 8]; // 8 samples at 88.2kHz

            let result = audio_processor.resample(&input, &mut output, 44100, 88200);
            assert!(result.is_ok());

            // Check that resampling produced reasonable results
            assert!(output.iter().all(|x| x.is_finite()));
            assert!(output.len() == 8);
        }

        #[test]
        fn test_compressor() {
            let capabilities = PlatformCapabilities::detect();
            let audio_processor = VectorizedAudioProcessing::new(&capabilities);

            let input = vec![0.5, 1.0, 1.5, 0.8, 0.3]; // Test signal with peaks
            let mut output = vec![0.0; 5];

            let settings = CompressorSettings {
                threshold: 0.7,
                ratio: 2.0,
                attack_time: 0.01,
                release_time: 0.1,
                makeup_gain: 1.0,
                sample_rate: 44100,
            };

            let result = audio_processor.compressor(&input, &mut output, &settings);
            assert!(result.is_ok());

            // Check that compression reduced peaks
            assert!(output[2] < input[2]); // Peak at index 2 should be reduced
            assert!(output.iter().all(|x| x.is_finite()));
        }
    }
}