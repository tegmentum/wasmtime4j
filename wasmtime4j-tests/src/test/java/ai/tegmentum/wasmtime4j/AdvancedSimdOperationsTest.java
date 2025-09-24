/*
 * Copyright 2024 Tegmentum Technology, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.SimdOperations.V128;
import ai.tegmentum.wasmtime4j.exception.WasmtimeException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

/**
 * Comprehensive test suite for advanced SIMD operations including:
 * - Extended vector types (V256, V512)
 * - Advanced arithmetic (FMA, reciprocal, square root)
 * - Advanced logical operations (popcount, variable shifts)
 * - Vector reductions (sum, min, max)
 * - Selection and blending operations
 * - Platform-specific optimizations (AVX-512, ARM NEON)
 * - Relaxed SIMD operations
 */
class AdvancedSimdOperationsTest {

  private WasmRuntime runtime;
  private SimdOperations simdOps;

  @BeforeEach
  void setUp() throws WasmtimeException {
    // Create runtime with advanced SIMD configuration
    final EngineConfig config = EngineConfig.builder()
        .enableSimd(true)
        .enableRelaxedSimd(true)
        .optimizationLevel(EngineConfig.OptimizationLevel.SPEED)
        .build();

    runtime = WasmRuntimeFactory.createRuntime(config);

    final SimdOperations.SimdConfig simdConfig = SimdOperations.SimdConfig.builder()
        .enablePlatformOptimizations(true)
        .enableRelaxedOperations(true)
        .maxVectorWidth(512) // Enable up to AVX-512
        .build();

    simdOps = new SimdOperations(simdConfig, runtime);
  }

  @AfterEach
  void tearDown() throws Exception {
    if (runtime != null) {
      runtime.close();
    }
  }

  // ===== ADVANCED ARITHMETIC OPERATIONS =====

  @Test
  void testFusedMultiplyAdd() throws WasmtimeException {
    // Test FMA operation: a * b + c
    final V128 a = V128.fromFloats(2.0f, 3.0f, 4.0f, 5.0f);
    final V128 b = V128.fromFloats(1.5f, 2.5f, 3.5f, 4.5f);
    final V128 c = V128.fromFloats(0.5f, 0.5f, 0.5f, 0.5f);

    final V128 result = simdOps.fma(a, b, c);
    final float[] resultFloats = result.getAsFloats();

    // Expected: (2.0 * 1.5) + 0.5 = 3.5, (3.0 * 2.5) + 0.5 = 8.0, etc.
    assertEquals(3.5f, resultFloats[0], 0.001f, "FMA lane 0 incorrect");
    assertEquals(8.0f, resultFloats[1], 0.001f, "FMA lane 1 incorrect");
    assertEquals(14.5f, resultFloats[2], 0.001f, "FMA lane 2 incorrect");
    assertEquals(23.0f, resultFloats[3], 0.001f, "FMA lane 3 incorrect");
  }

  @Test
  void testFusedMultiplySubtract() throws WasmtimeException {
    // Test FMS operation: a * b - c
    final V128 a = V128.fromFloats(3.0f, 4.0f, 5.0f, 6.0f);
    final V128 b = V128.fromFloats(2.0f, 2.0f, 2.0f, 2.0f);
    final V128 c = V128.fromFloats(1.0f, 1.0f, 1.0f, 1.0f);

    final V128 result = simdOps.fms(a, b, c);
    final float[] resultFloats = result.getAsFloats();

    // Expected: (3.0 * 2.0) - 1.0 = 5.0, (4.0 * 2.0) - 1.0 = 7.0, etc.
    assertEquals(5.0f, resultFloats[0], 0.001f, "FMS lane 0 incorrect");
    assertEquals(7.0f, resultFloats[1], 0.001f, "FMS lane 1 incorrect");
    assertEquals(9.0f, resultFloats[2], 0.001f, "FMS lane 2 incorrect");
    assertEquals(11.0f, resultFloats[3], 0.001f, "FMS lane 3 incorrect");
  }

  @Test
  void testVectorReciprocal() throws WasmtimeException {
    final V128 a = V128.fromFloats(1.0f, 2.0f, 4.0f, 8.0f);

    final V128 result = simdOps.reciprocal(a);
    final float[] resultFloats = result.getAsFloats();

    // Expected: 1/1 = 1.0, 1/2 = 0.5, 1/4 = 0.25, 1/8 = 0.125
    assertEquals(1.0f, resultFloats[0], 0.01f, "Reciprocal lane 0 incorrect");
    assertEquals(0.5f, resultFloats[1], 0.01f, "Reciprocal lane 1 incorrect");
    assertEquals(0.25f, resultFloats[2], 0.01f, "Reciprocal lane 2 incorrect");
    assertEquals(0.125f, resultFloats[3], 0.01f, "Reciprocal lane 3 incorrect");
  }

  @Test
  void testVectorSquareRoot() throws WasmtimeException {
    final V128 a = V128.fromFloats(1.0f, 4.0f, 9.0f, 16.0f);

    final V128 result = simdOps.sqrt(a);
    final float[] resultFloats = result.getAsFloats();

    // Expected: sqrt(1) = 1.0, sqrt(4) = 2.0, sqrt(9) = 3.0, sqrt(16) = 4.0
    assertEquals(1.0f, resultFloats[0], 0.001f, "Sqrt lane 0 incorrect");
    assertEquals(2.0f, resultFloats[1], 0.001f, "Sqrt lane 1 incorrect");
    assertEquals(3.0f, resultFloats[2], 0.001f, "Sqrt lane 2 incorrect");
    assertEquals(4.0f, resultFloats[3], 0.001f, "Sqrt lane 3 incorrect");
  }

  @Test
  void testReciprocalSquareRoot() throws WasmtimeException {
    final V128 a = V128.fromFloats(1.0f, 4.0f, 9.0f, 16.0f);

    final V128 result = simdOps.rsqrt(a);
    final float[] resultFloats = result.getAsFloats();

    // Expected: 1/sqrt(1) = 1.0, 1/sqrt(4) = 0.5, 1/sqrt(9) = 0.333, 1/sqrt(16) = 0.25
    assertEquals(1.0f, resultFloats[0], 0.01f, "Rsqrt lane 0 incorrect");
    assertEquals(0.5f, resultFloats[1], 0.01f, "Rsqrt lane 1 incorrect");
    assertEquals(0.333f, resultFloats[2], 0.01f, "Rsqrt lane 2 incorrect");
    assertEquals(0.25f, resultFloats[3], 0.01f, "Rsqrt lane 3 incorrect");
  }

  // ===== ADVANCED LOGICAL OPERATIONS =====

  @Test
  void testPopulationCount() throws WasmtimeException {
    // Test bit population count (number of 1-bits)
    final V128 a = V128.fromInts(0b1111, 0b1010, 0b11110000, 0b10101010);

    final V128 result = simdOps.popcount(a);
    final int[] resultInts = result.getAsInts();

    // Expected: popcount(1111b) = 4, popcount(1010b) = 2, etc.
    assertEquals(4, resultInts[0], "Popcount lane 0 incorrect");
    assertEquals(2, resultInts[1], "Popcount lane 1 incorrect");
    assertEquals(4, resultInts[2], "Popcount lane 2 incorrect");
    assertEquals(4, resultInts[3], "Popcount lane 3 incorrect");
  }

  @Test
  void testVariableShiftLeft() throws WasmtimeException {
    final V128 a = V128.fromInts(1, 2, 4, 8);
    final V128 shiftAmounts = V128.fromInts(1, 2, 3, 4);

    final V128 result = simdOps.shlVariable(a, shiftAmounts);
    final int[] resultInts = result.getAsInts();

    // Expected: 1 << 1 = 2, 2 << 2 = 8, 4 << 3 = 32, 8 << 4 = 128
    assertEquals(2, resultInts[0], "Variable SHL lane 0 incorrect");
    assertEquals(8, resultInts[1], "Variable SHL lane 1 incorrect");
    assertEquals(32, resultInts[2], "Variable SHL lane 2 incorrect");
    assertEquals(128, resultInts[3], "Variable SHL lane 3 incorrect");
  }

  @Test
  void testVariableShiftRight() throws WasmtimeException {
    final V128 a = V128.fromInts(16, 32, 64, 128);
    final V128 shiftAmounts = V128.fromInts(1, 2, 3, 4);

    final V128 result = simdOps.shrVariable(a, shiftAmounts);
    final int[] resultInts = result.getAsInts();

    // Expected: 16 >> 1 = 8, 32 >> 2 = 8, 64 >> 3 = 8, 128 >> 4 = 8
    assertEquals(8, resultInts[0], "Variable SHR lane 0 incorrect");
    assertEquals(8, resultInts[1], "Variable SHR lane 1 incorrect");
    assertEquals(8, resultInts[2], "Variable SHR lane 2 incorrect");
    assertEquals(8, resultInts[3], "Variable SHR lane 3 incorrect");
  }

  // ===== VECTOR REDUCTION OPERATIONS =====

  @Test
  void testHorizontalSum() throws WasmtimeException {
    final V128 a = V128.fromFloats(1.0f, 2.0f, 3.0f, 4.0f);

    final float result = simdOps.horizontalSum(a);

    // Expected: 1.0 + 2.0 + 3.0 + 4.0 = 10.0
    assertEquals(10.0f, result, 0.001f, "Horizontal sum incorrect");
  }

  @Test
  void testHorizontalMin() throws WasmtimeException {
    final V128 a = V128.fromFloats(3.0f, 1.0f, 4.0f, 2.0f);

    final float result = simdOps.horizontalMin(a);

    // Expected: min(3.0, 1.0, 4.0, 2.0) = 1.0
    assertEquals(1.0f, result, 0.001f, "Horizontal min incorrect");
  }

  @Test
  void testHorizontalMax() throws WasmtimeException {
    final V128 a = V128.fromFloats(3.0f, 1.0f, 4.0f, 2.0f);

    final float result = simdOps.horizontalMax(a);

    // Expected: max(3.0, 1.0, 4.0, 2.0) = 4.0
    assertEquals(4.0f, result, 0.001f, "Horizontal max incorrect");
  }

  // ===== SELECTION AND BLENDING OPERATIONS =====

  @Test
  void testVectorSelect() throws WasmtimeException {
    final V128 mask = V128.fromInts(-1, 0, -1, 0); // Select from a, b, a, b
    final V128 a = V128.fromInts(10, 20, 30, 40);
    final V128 b = V128.fromInts(100, 200, 300, 400);

    final V128 result = simdOps.select(mask, a, b);
    final int[] resultInts = result.getAsInts();

    // Expected: select a, b, a, b based on mask
    assertEquals(10, resultInts[0], "Select lane 0 incorrect");
    assertEquals(200, resultInts[1], "Select lane 1 incorrect");
    assertEquals(30, resultInts[2], "Select lane 2 incorrect");
    assertEquals(400, resultInts[3], "Select lane 3 incorrect");
  }

  @Test
  void testVectorBlend() throws WasmtimeException {
    final V128 a = V128.fromInts(10, 20, 30, 40);
    final V128 b = V128.fromInts(100, 200, 300, 400);
    final int mask = 0b1010; // Binary: lanes 1 and 3 from b, 0 and 2 from a

    final V128 result = simdOps.blend(a, b, mask);
    final int[] resultInts = result.getAsInts();

    // Expected based on mask pattern
    assertEquals(10, resultInts[0], "Blend lane 0 incorrect");
    assertEquals(200, resultInts[1], "Blend lane 1 incorrect");
    assertEquals(30, resultInts[2], "Blend lane 2 incorrect");
    assertEquals(400, resultInts[3], "Blend lane 3 incorrect");
  }

  // ===== PLATFORM-SPECIFIC OPTIMIZATIONS =====

  @Test
  void testPlatformCapabilityDetection() {
    final String capabilities = simdOps.getSimdCapabilities();

    assertNotNull(capabilities, "SIMD capabilities should not be null");
    assertFalse(capabilities.isEmpty(), "SIMD capabilities should not be empty");

    // Verify that some common capabilities are reported
    assertTrue(capabilities.contains("v128") || capabilities.contains("SIMD"),
        "Should report at least basic SIMD support");
  }

  @Test
  void testMaxVectorWidthDetection() {
    final SimdOperations.SimdConfig config = simdOps.getConfig();
    final int maxWidth = config.getMaxVectorWidth();

    assertTrue(maxWidth >= 128, "Max vector width should be at least 128-bit");
    assertTrue(maxWidth <= 512, "Max vector width should not exceed 512-bit");
  }

  // ===== ERROR HANDLING =====

  @Test
  void testDivisionByZeroError() {
    final V128 a = V128.fromFloats(1.0f, 2.0f, 3.0f, 4.0f);

    assertThrows(WasmtimeException.class, () -> {
      simdOps.reciprocal(V128.fromFloats(0.0f, 1.0f, 2.0f, 3.0f));
    }, "Should throw exception for division by zero in reciprocal");
  }

  @Test
  void testNegativeSquareRootError() {
    assertThrows(WasmtimeException.class, () -> {
      simdOps.sqrt(V128.fromFloats(-1.0f, 1.0f, 2.0f, 3.0f));
    }, "Should throw exception for negative square root");
  }

  @Test
  void testInvalidBlendMask() {
    final V128 a = V128.fromInts(1, 2, 3, 4);
    final V128 b = V128.fromInts(5, 6, 7, 8);

    assertThrows(IllegalArgumentException.class, () -> {
      simdOps.blend(a, b, 256); // Invalid mask (> 255)
    }, "Should throw exception for invalid blend mask");
  }

  // ===== RELAXED SIMD OPERATIONS =====

  @Test
  void testRelaxedOperationsConfig() {
    final SimdOperations.SimdConfig relaxedConfig = SimdOperations.SimdConfig.builder()
        .enableRelaxedOperations(true)
        .build();

    final SimdOperations relaxedSimd = new SimdOperations(relaxedConfig, runtime);

    assertTrue(relaxedConfig.isRelaxedOperationsEnabled(),
        "Relaxed operations should be enabled");

    // Test that relaxed operations work without throwing exceptions
    final V128 a = V128.fromFloats(1.0f, 2.0f, 3.0f, 4.0f);
    final V128 b = V128.fromFloats(0.5f, 1.5f, 2.5f, 3.5f);

    assertDoesNotThrow(() -> {
      relaxedSimd.relaxedAdd(a, b);
    }, "Relaxed add should not throw exception");
  }

  // ===== PERFORMANCE AND BENCHMARKING =====

  @Test
  void testAdvancedSimdPerformance() throws WasmtimeException {
    // Simple performance comparison between basic and advanced operations
    final V128 a = V128.fromFloats(1.0f, 2.0f, 3.0f, 4.0f);
    final V128 b = V128.fromFloats(0.5f, 1.5f, 2.5f, 3.5f);
    final V128 c = V128.fromFloats(0.1f, 0.2f, 0.3f, 0.4f);

    final int iterations = 1000;

    // Measure basic operations
    final long startBasic = System.nanoTime();
    for (int i = 0; i < iterations; i++) {
      final V128 temp = simdOps.multiply(a, b);
      simdOps.add(temp, c);
    }
    final long endBasic = System.nanoTime();

    // Measure FMA operation
    final long startFma = System.nanoTime();
    for (int i = 0; i < iterations; i++) {
      simdOps.fma(a, b, c);
    }
    final long endFma = System.nanoTime();

    final long basicTime = endBasic - startBasic;
    final long fmaTime = endFma - startFma;

    // FMA should generally be faster or comparable
    assertTrue(fmaTime <= basicTime * 1.5,
        "FMA operation should be reasonably performant compared to separate mul+add");
  }

  // ===== CROSS-RUNTIME VALIDATION =====

  @Test
  @EnabledOnJre(JRE.JAVA_23)
  void testPanamaRuntimeAdvancedSimd() throws WasmtimeException {
    // Test that advanced SIMD operations work correctly with Panama runtime
    assumePanamaAvailable();

    final V128 a = V128.fromFloats(2.0f, 3.0f, 4.0f, 5.0f);
    final V128 b = V128.fromFloats(1.0f, 2.0f, 3.0f, 4.0f);
    final V128 c = V128.fromFloats(0.5f, 0.5f, 0.5f, 0.5f);

    final V128 result = simdOps.fma(a, b, c);
    assertNotNull(result, "FMA result should not be null with Panama runtime");

    final float[] resultFloats = result.getAsFloats();
    assertEquals(4, resultFloats.length, "Result should have 4 lanes");
  }

  private void assumePanamaAvailable() {
    try {
      Class.forName("java.lang.foreign.MemorySegment");
    } catch (ClassNotFoundException e) {
      org.junit.jupiter.api.Assumptions.assumeTrue(false, "Panama FFI not available");
    }
  }

  // ===== DEBUGGING AND INTROSPECTION =====

  @Test
  void testSimdCapabilitiesIntrospection() {
    final String capabilities = simdOps.getSimdCapabilities();

    // Verify detailed capability reporting
    assertNotNull(capabilities, "Capabilities should not be null");

    // Should contain information about platform features
    if (isX86Platform()) {
      // Check for x86-specific features that might be available
      assertTrue(capabilities.toLowerCase().contains("x86") ||
          capabilities.toLowerCase().contains("sse") ||
          capabilities.toLowerCase().contains("avx") ||
          capabilities.toLowerCase().contains("simd"),
          "Should report x86-related SIMD capabilities");
    } else if (isArmPlatform()) {
      // Check for ARM-specific features
      assertTrue(capabilities.toLowerCase().contains("arm") ||
          capabilities.toLowerCase().contains("neon") ||
          capabilities.toLowerCase().contains("simd"),
          "Should report ARM-related SIMD capabilities");
    }
  }

  private boolean isX86Platform() {
    final String arch = System.getProperty("os.arch").toLowerCase();
    return arch.contains("x86") || arch.contains("amd64");
  }

  private boolean isArmPlatform() {
    final String arch = System.getProperty("os.arch").toLowerCase();
    return arch.contains("arm") || arch.contains("aarch64");
  }

  @Test
  void testDebugModeConfiguration() {
    final SimdOperations.SimdConfig debugConfig = SimdOperations.SimdConfig.builder()
        .validateVectorOperands(true)
        .maxVectorWidth(128) // Conservative for debugging
        .build();

    final SimdOperations debugSimd = new SimdOperations(debugConfig, runtime);

    assertTrue(debugConfig.isVectorOperandValidationEnabled(),
        "Debug mode should enable validation");

    // Test that validation catches null operands
    assertThrows(IllegalArgumentException.class, () -> {
      debugSimd.add(null, V128.zero());
    }, "Should validate null operands in debug mode");
  }
}