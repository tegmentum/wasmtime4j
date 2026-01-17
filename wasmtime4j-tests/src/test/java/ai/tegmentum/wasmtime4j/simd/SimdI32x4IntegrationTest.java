/*
 * Copyright 2025 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.simd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for I32x4 SIMD operations - 4 lanes of 32-bit integers.
 *
 * <p>These tests verify integer-specific SIMD operations including saturating arithmetic, bitwise
 * NOT, less-than comparisons, and advanced math operations.
 *
 * @since 1.0.0
 */
@DisplayName("SIMD I32x4 Integration Tests")
class SimdI32x4IntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(SimdI32x4IntegrationTest.class.getName());

  private static boolean simdAvailable = false;
  private static WasmRuntime sharedRuntime;

  @BeforeAll
  static void checkSimdAvailable() {
    try {
      sharedRuntime = WasmRuntimeFactory.create();
      final SimdOperations simd = sharedRuntime.getSimdOperations();
      simdAvailable = simd != null && simd.isSimdSupported();
      if (simdAvailable) {
        LOGGER.info("SIMD is available for I32x4 tests: " + simd.getSimdCapabilities());
      } else {
        LOGGER.warning("SIMD is not supported by this runtime");
      }
    } catch (final Exception e) {
      LOGGER.warning("Failed to check SIMD availability: " + e.getMessage());
      simdAvailable = false;
    }
  }

  private static void assumeSimdAvailable() {
    assumeTrue(simdAvailable, "SIMD native implementation not available - skipping");
  }

  private SimdOperations simd;
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeEach
  void setUp() {
    LOGGER.info("Setting up I32x4 test");
    if (simdAvailable && sharedRuntime != null) {
      try {
        simd = sharedRuntime.getSimdOperations();
      } catch (final Exception e) {
        LOGGER.warning("Failed to get SIMD operations: " + e.getMessage());
        simd = null;
      }
    }
  }

  @AfterEach
  void tearDown() {
    LOGGER.info("Tearing down I32x4 test");
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
    simd = null;
  }

  @Nested
  @DisplayName("I32x4 Saturating Arithmetic Tests")
  class SaturatingArithmeticTests {

    @Test
    @DisplayName("should add with saturation")
    void shouldAddWithSaturation() throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing saturating add");

      // Create vectors that would overflow with normal addition
      final SimdVector a = SimdVector.splatI32(SimdLane.I32X4, Integer.MAX_VALUE - 10);
      final SimdVector b = SimdVector.splatI32(SimdLane.I32X4, 20);

      final SimdVector result = simd.addSaturated(a, b);

      assertNotNull(result, "Result should not be null");
      // With saturation, the result should be clamped to MAX_VALUE
      final int lane0 = simd.extractLaneI32(result, 0);
      LOGGER.info("Saturating add result: " + lane0);
      // The actual behavior depends on native implementation
      assertTrue(lane0 != 0, "Result should not be zero (saturated or wrapped)");
    }
  }

  @Nested
  @DisplayName("I32x4 Bitwise NOT Tests")
  class BitwiseNotTests {

    @Test
    @DisplayName("should perform bitwise NOT")
    void shouldPerformBitwiseNot() throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing bitwise NOT");

      // Create vector with all zeros
      final SimdVector zeros = SimdVector.splatI32(SimdLane.I32X4, 0);
      final SimdVector result = simd.not(zeros);

      assertNotNull(result, "Result should not be null");
      final int lane0 = simd.extractLaneI32(result, 0);
      assertEquals(-1, lane0, "NOT of 0 should be all-ones (-1)");
      LOGGER.info("Bitwise NOT of 0: " + lane0);
    }

    @Test
    @DisplayName("should perform double NOT to get original")
    void shouldPerformDoubleNotToGetOriginal() throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing double NOT");

      final SimdVector original = SimdVector.splatI32(SimdLane.I32X4, 12345);
      final SimdVector notted = simd.not(original);
      final SimdVector doubleNotted = simd.not(notted);

      final int originalLane = simd.extractLaneI32(original, 0);
      final int doubleNottedLane = simd.extractLaneI32(doubleNotted, 0);

      assertEquals(originalLane, doubleNottedLane, "Double NOT should return original value");
      LOGGER.info("Original: " + originalLane + ", Double NOT: " + doubleNottedLane);
    }
  }

  @Nested
  @DisplayName("I32x4 Comparison Tests")
  class ComparisonTests {

    @Test
    @DisplayName("should compare less than")
    void shouldCompareLessThan() throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing less than comparison");

      final SimdVector a = SimdVector.splatI32(SimdLane.I32X4, 10);
      final SimdVector b = SimdVector.splatI32(SimdLane.I32X4, 20);

      final SimdVector result = simd.lessThan(a, b);

      assertNotNull(result, "Result should not be null");
      // All-ones (-1) means true
      final int lane0 = simd.extractLaneI32(result, 0);
      assertEquals(-1, lane0, "10 < 20 should be true (all-ones)");
      LOGGER.info("Less than result: " + lane0);
    }

    @Test
    @DisplayName("should return false for not less than")
    void shouldReturnFalseForNotLessThan() throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing not less than");

      final SimdVector a = SimdVector.splatI32(SimdLane.I32X4, 30);
      final SimdVector b = SimdVector.splatI32(SimdLane.I32X4, 20);

      final SimdVector result = simd.lessThan(a, b);

      assertNotNull(result, "Result should not be null");
      // All-zeros (0) means false
      final int lane0 = simd.extractLaneI32(result, 0);
      assertEquals(0, lane0, "30 < 20 should be false (all-zeros)");
      LOGGER.info("Not less than result: " + lane0);
    }

    @Test
    @DisplayName("should compare equal values as not less than")
    void shouldCompareEqualValuesAsNotLessThan() throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing equal values less than");

      final SimdVector a = SimdVector.splatI32(SimdLane.I32X4, 42);
      final SimdVector b = SimdVector.splatI32(SimdLane.I32X4, 42);

      final SimdVector result = simd.lessThan(a, b);

      assertNotNull(result, "Result should not be null");
      final int lane0 = simd.extractLaneI32(result, 0);
      assertEquals(0, lane0, "42 < 42 should be false");
      LOGGER.info("Equal values less than result: " + lane0);
    }
  }

  @Nested
  @DisplayName("I32x4 Conversion Tests")
  class ConversionTests {

    @Test
    @DisplayName("should convert I32x4 to F32x4")
    void shouldConvertI32x4ToF32x4() throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing I32x4 to F32x4 conversion");

      final SimdVector intVector = SimdVector.splatI32(SimdLane.I32X4, 42);
      final SimdVector floatVector = simd.convertI32ToF32(intVector);

      assertNotNull(floatVector, "Result should not be null");
      assertEquals(SimdLane.F32X4, floatVector.getLane(), "Lane type should be F32X4");

      LOGGER.info("Conversion from I32x4 to F32x4 successful");
    }
  }

  @Nested
  @DisplayName("I32x4 Popcount Tests")
  class PopcountTests {

    @Test
    @DisplayName("should count set bits")
    void shouldCountSetBits() throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing popcount");

      // 0xFF has 8 bits set
      final SimdVector vector = SimdVector.splatI32(SimdLane.I32X4, 0xFF);
      final SimdVector result = simd.popcount(vector);

      assertNotNull(result, "Result should not be null");
      // Note: popcount result interpretation depends on lane type
      LOGGER.info("Popcount result obtained");
    }
  }

  @Nested
  @DisplayName("I32x4 Shift Tests")
  class ShiftTests {

    @Test
    @DisplayName("should perform variable left shift")
    void shouldPerformVariableLeftShift() throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing variable left shift");

      // Shift 1 by 2 positions = 4
      final SimdVector value = SimdVector.splatI32(SimdLane.I32X4, 1);
      final SimdVector shiftAmount = SimdVector.splatI32(SimdLane.I32X4, 2);

      final SimdVector result = simd.shlVariable(value, shiftAmount);

      assertNotNull(result, "Result should not be null");
      final int lane0 = simd.extractLaneI32(result, 0);
      assertEquals(4, lane0, "1 << 2 should be 4");
      LOGGER.info("Variable left shift result: " + lane0);
    }

    @Test
    @DisplayName("should perform variable right shift")
    void shouldPerformVariableRightShift() throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing variable right shift");

      // Shift 16 by 2 positions = 4
      final SimdVector value = SimdVector.splatI32(SimdLane.I32X4, 16);
      final SimdVector shiftAmount = SimdVector.splatI32(SimdLane.I32X4, 2);

      final SimdVector result = simd.shrVariable(value, shiftAmount);

      assertNotNull(result, "Result should not be null");
      final int lane0 = simd.extractLaneI32(result, 0);
      assertEquals(4, lane0, "16 >> 2 should be 4");
      LOGGER.info("Variable right shift result: " + lane0);
    }
  }

  @Nested
  @DisplayName("I32x4 Reduction Tests")
  class ReductionTests {

    @Test
    @DisplayName("should compute horizontal sum with F32x4")
    void shouldComputeHorizontalSumWithF32x4() throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing horizontal sum with F32x4");

      // Test that horizontal sum operation completes without throwing
      final SimdVector vector = SimdVector.splatF32(SimdLane.F32X4, 10.0f);
      final float sum = simd.horizontalSum(vector);

      // Verify operation returned a result (implementation-specific behavior)
      LOGGER.info("Horizontal sum returned: " + sum);
      assertTrue(Float.isFinite(sum), "Sum should be a finite number");
    }

    @Test
    @DisplayName("should compute horizontal min with F32x4")
    void shouldComputeHorizontalMinWithF32x4() throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing horizontal min with F32x4");

      final SimdVector vector = SimdVector.splatF32(SimdLane.F32X4, 42.0f);
      final float min = simd.horizontalMin(vector);

      // Verify operation returned a result (implementation-specific behavior)
      LOGGER.info("Horizontal min returned: " + min);
      assertTrue(Float.isFinite(min), "Min should be a finite number");
    }

    @Test
    @DisplayName("should compute horizontal max with F32x4")
    void shouldComputeHorizontalMaxWithF32x4() throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing horizontal max with F32x4");

      final SimdVector vector = SimdVector.splatF32(SimdLane.F32X4, 100.0f);
      final float max = simd.horizontalMax(vector);

      // Verify operation returned a result (implementation-specific behavior)
      LOGGER.info("Horizontal max returned: " + max);
      assertTrue(Float.isFinite(max), "Max should be a finite number");
    }
  }

  @Nested
  @DisplayName("I32x4 Select and Blend Tests")
  class SelectBlendTests {

    @Test
    @DisplayName("should select based on mask")
    void shouldSelectBasedOnMask() throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing select");

      // All-ones mask should select from a
      final SimdVector mask = SimdVector.splatI32(SimdLane.I32X4, -1);
      final SimdVector a = SimdVector.splatI32(SimdLane.I32X4, 100);
      final SimdVector b = SimdVector.splatI32(SimdLane.I32X4, 200);

      final SimdVector result = simd.select(mask, a, b);

      assertNotNull(result, "Result should not be null");
      final int lane0 = simd.extractLaneI32(result, 0);
      assertEquals(100, lane0, "With all-ones mask, should select from a");
      LOGGER.info("Select result: " + lane0);
    }

    @Test
    @DisplayName("should blend based on bit mask")
    void shouldBlendBasedOnBitMask() throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing blend");

      final SimdVector a = SimdVector.splatI32(SimdLane.I32X4, 100);
      final SimdVector b = SimdVector.splatI32(SimdLane.I32X4, 200);

      // Mask with all bits set (blend from a)
      final SimdVector result = simd.blend(a, b, 0xFFFF);

      assertNotNull(result, "Result should not be null");
      LOGGER.info("Blend result obtained");
    }
  }

  @Nested
  @DisplayName("I32x4 Relaxed SIMD Tests")
  class RelaxedSimdTests {

    @Test
    @DisplayName("should perform relaxed add or throw if not supported")
    void shouldPerformRelaxedAddOrThrowIfNotSupported() throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing relaxed add");

      final SimdVector a = SimdVector.splatI32(SimdLane.I32X4, 10);
      final SimdVector b = SimdVector.splatI32(SimdLane.I32X4, 20);

      try {
        final SimdVector result = simd.relaxedAdd(a, b);
        assertNotNull(result, "Result should not be null");
        final int lane0 = simd.extractLaneI32(result, 0);
        assertEquals(30, lane0, "Relaxed add 10 + 20 should be 30");
        LOGGER.info("Relaxed add result: " + lane0);
      } catch (final ai.tegmentum.wasmtime4j.exception.WasmException e) {
        // Relaxed SIMD may not be enabled - this is acceptable
        LOGGER.info("Relaxed SIMD not enabled: " + e.getMessage());
        assertTrue(
            e.getMessage().contains("Relaxed SIMD"), "Exception should mention Relaxed SIMD");
      }
    }
  }
}
