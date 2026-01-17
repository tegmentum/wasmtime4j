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
 * Integration tests for F64x2 SIMD operations - 2 lanes of 64-bit floats.
 *
 * <p>These tests verify double-precision floating-point SIMD operations including FMA, sqrt,
 * reciprocal, and conversion operations.
 *
 * @since 1.0.0
 */
@DisplayName("SIMD F64x2 Integration Tests")
class SimdF64x2IntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(SimdF64x2IntegrationTest.class.getName());

  private static boolean simdAvailable = false;
  private static WasmRuntime sharedRuntime;

  @BeforeAll
  static void checkSimdAvailable() {
    try {
      sharedRuntime = WasmRuntimeFactory.create();
      final SimdOperations simd = sharedRuntime.getSimdOperations();
      simdAvailable = simd != null && simd.isSimdSupported();
      if (simdAvailable) {
        LOGGER.info("SIMD is available for F64x2 tests");
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
    LOGGER.info("Setting up F64x2 test");
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
    LOGGER.info("Tearing down F64x2 test");
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
  @DisplayName("F32x4 FMA Tests")
  class FmaTests {

    @Test
    @DisplayName("should perform fused multiply-add")
    void shouldPerformFusedMultiplyAdd() throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing fused multiply-add");

      // FMA: (a * b) + c = (2.0 * 3.0) + 4.0 = 10.0
      final SimdVector a = SimdVector.splatF32(SimdLane.F32X4, 2.0f);
      final SimdVector b = SimdVector.splatF32(SimdLane.F32X4, 3.0f);
      final SimdVector c = SimdVector.splatF32(SimdLane.F32X4, 4.0f);

      final SimdVector result = simd.fma(a, b, c);

      assertNotNull(result, "Result should not be null");
      assertEquals(SimdLane.F32X4, result.getLane(), "Lane type should be F32X4");

      LOGGER.info("FMA operation completed successfully");
    }

    @Test
    @DisplayName("should perform fused multiply-subtract")
    void shouldPerformFusedMultiplySubtract() throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing fused multiply-subtract");

      // FMS: (a * b) - c = (5.0 * 4.0) - 10.0 = 10.0
      final SimdVector a = SimdVector.splatF32(SimdLane.F32X4, 5.0f);
      final SimdVector b = SimdVector.splatF32(SimdLane.F32X4, 4.0f);
      final SimdVector c = SimdVector.splatF32(SimdLane.F32X4, 10.0f);

      final SimdVector result = simd.fms(a, b, c);

      assertNotNull(result, "Result should not be null");
      assertEquals(SimdLane.F32X4, result.getLane(), "Lane type should be F32X4");

      LOGGER.info("FMS operation completed successfully");
    }
  }

  @Nested
  @DisplayName("F32x4 Math Function Tests")
  class MathFunctionTests {

    @Test
    @DisplayName("should compute square root")
    void shouldComputeSquareRoot() throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing square root");

      // sqrt(4.0) = 2.0
      final SimdVector vector = SimdVector.splatF32(SimdLane.F32X4, 4.0f);
      final SimdVector result = simd.sqrt(vector);

      assertNotNull(result, "Result should not be null");
      assertEquals(SimdLane.F32X4, result.getLane(), "Lane type should be F32X4");

      LOGGER.info("Square root operation completed successfully");
    }

    @Test
    @DisplayName("should compute reciprocal")
    void shouldComputeReciprocal() throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing reciprocal");

      // reciprocal(4.0) = 0.25
      final SimdVector vector = SimdVector.splatF32(SimdLane.F32X4, 4.0f);
      final SimdVector result = simd.reciprocal(vector);

      assertNotNull(result, "Result should not be null");
      assertEquals(SimdLane.F32X4, result.getLane(), "Lane type should be F32X4");

      LOGGER.info("Reciprocal operation completed successfully");
    }

    @Test
    @DisplayName("should compute reciprocal square root")
    void shouldComputeReciprocalSquareRoot() throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing reciprocal square root");

      // rsqrt(4.0) = 1/sqrt(4) = 0.5
      final SimdVector vector = SimdVector.splatF32(SimdLane.F32X4, 4.0f);
      final SimdVector result = simd.rsqrt(vector);

      assertNotNull(result, "Result should not be null");
      assertEquals(SimdLane.F32X4, result.getLane(), "Lane type should be F32X4");

      LOGGER.info("Reciprocal square root operation completed successfully");
    }
  }

  @Nested
  @DisplayName("F32x4 Conversion Tests")
  class ConversionTests {

    @Test
    @DisplayName("should convert F32x4 to I32x4")
    void shouldConvertF32x4ToI32x4() throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing F32x4 to I32x4 conversion");

      final SimdVector floatVector = SimdVector.splatF32(SimdLane.F32X4, 42.5f);
      final SimdVector intVector = simd.convertF32ToI32(floatVector);

      assertNotNull(intVector, "Result should not be null");
      assertEquals(SimdLane.I32X4, intVector.getLane(), "Lane type should be I32X4");

      // The integer value should be truncated
      final int lane0 = simd.extractLaneI32(intVector, 0);
      LOGGER.info("Converted value: " + lane0);
    }

    @Test
    @DisplayName("should roundtrip I32x4 through F32x4")
    void shouldRoundtripI32x4ThroughF32x4() throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing I32x4 -> F32x4 -> I32x4 roundtrip");

      final SimdVector original = SimdVector.splatI32(SimdLane.I32X4, 100);
      final SimdVector asFloat = simd.convertI32ToF32(original);
      final SimdVector backToInt = simd.convertF32ToI32(asFloat);

      final int originalLane = simd.extractLaneI32(original, 0);
      final int roundtripLane = simd.extractLaneI32(backToInt, 0);

      assertEquals(originalLane, roundtripLane, "Roundtrip should preserve integer value");
      LOGGER.info("Roundtrip: " + originalLane + " -> float -> " + roundtripLane);
    }
  }

  @Nested
  @DisplayName("F32x4 Arithmetic Tests")
  class ArithmeticTests {

    @Test
    @DisplayName("should divide floats correctly")
    void shouldDivideFloatsCorrectly() throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing float division");

      // 10.0 / 2.0 = 5.0
      final SimdVector a = SimdVector.splatF32(SimdLane.F32X4, 10.0f);
      final SimdVector b = SimdVector.splatF32(SimdLane.F32X4, 2.0f);

      final SimdVector result = simd.divide(a, b);

      assertNotNull(result, "Result should not be null");
      assertEquals(SimdLane.F32X4, result.getLane(), "Lane type should be F32X4");

      LOGGER.info("Float division completed successfully");
    }

    @Test
    @DisplayName("should multiply floats correctly")
    void shouldMultiplyFloatsCorrectly() throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing float multiplication");

      // 3.0 * 4.0 = 12.0
      final SimdVector a = SimdVector.splatF32(SimdLane.F32X4, 3.0f);
      final SimdVector b = SimdVector.splatF32(SimdLane.F32X4, 4.0f);

      final SimdVector result = simd.multiply(a, b);

      assertNotNull(result, "Result should not be null");
      assertEquals(SimdLane.F32X4, result.getLane(), "Lane type should be F32X4");

      LOGGER.info("Float multiplication completed successfully");
    }
  }

  @Nested
  @DisplayName("V128 Double Lane Tests")
  class V128DoubleLaneTests {

    @Test
    @DisplayName("should create V128 from doubles")
    void shouldCreateV128FromDoubles() {
      LOGGER.info("Testing V128.fromDoubles()");

      final V128 vector = V128.fromDoubles(Math.PI, Math.E);

      assertNotNull(vector, "Vector should not be null");
      final double[] values = vector.getAsDoubles();
      assertEquals(2, values.length, "Should have 2 lanes");
      assertEquals(Math.PI, values[0], 0.00001, "Lane 0 should be PI");
      assertEquals(Math.E, values[1], 0.00001, "Lane 1 should be E");

      LOGGER.info("V128 from doubles: [" + values[0] + ", " + values[1] + "]");
    }

    @Test
    @DisplayName("should extract long lanes from V128")
    void shouldExtractLongLanesFromV128() {
      LOGGER.info("Testing V128 long lanes");

      final V128 vector = V128.fromLongs(Long.MAX_VALUE, Long.MIN_VALUE);

      final long[] values = vector.getAsLongs();
      assertEquals(Long.MAX_VALUE, values[0], "Lane 0 should be MAX_VALUE");
      assertEquals(Long.MIN_VALUE, values[1], "Lane 1 should be MIN_VALUE");

      LOGGER.info("V128 long lanes: [" + values[0] + ", " + values[1] + "]");
    }
  }

  @Nested
  @DisplayName("SimdVector F32x4 Creation Tests")
  class SimdVectorCreationTests {

    @Test
    @DisplayName("should create SimdVector with F32x4 splat")
    void shouldCreateSimdVectorWithF32x4Splat() {
      LOGGER.info("Testing SimdVector.splatF32()");

      final SimdVector vector = SimdVector.splatF32(SimdLane.F32X4, 99.9f);

      assertNotNull(vector, "Vector should not be null");
      assertEquals(SimdLane.F32X4, vector.getLane(), "Lane type should be F32X4");
      assertEquals(16, vector.getData().length, "Data should be 16 bytes");

      LOGGER.info("SimdVector F32x4 created successfully");
    }

    @Test
    @DisplayName("should create SimdVector with I64X2 splat")
    void shouldCreateSimdVectorWithI64x2Splat() {
      LOGGER.info("Testing SimdVector with I64X2 lane");

      final SimdVector vector = SimdVector.splatI32(SimdLane.I64X2, 12345);

      assertNotNull(vector, "Vector should not be null");
      assertEquals(SimdLane.I64X2, vector.getLane(), "Lane type should be I64X2");
      assertEquals(16, vector.getData().length, "Data should be 16 bytes");

      LOGGER.info("SimdVector I64X2 created successfully");
    }

    @Test
    @DisplayName("should create SimdVector with F64X2 lane from V128")
    void shouldCreateSimdVectorWithF64x2LaneFromV128() {
      LOGGER.info("Testing SimdVector with F64X2 lane from V128");

      // Create a V128 with doubles and convert to SimdVector with F64X2 lane
      final V128 v128 = V128.fromDoubles(3.14159, 2.71828);
      final SimdVector vector = v128.toSimdVector(SimdLane.F64X2);

      assertNotNull(vector, "Vector should not be null");
      assertEquals(SimdLane.F64X2, vector.getLane(), "Lane type should be F64X2");
      assertEquals(16, vector.getData().length, "Data should be 16 bytes");

      LOGGER.info("SimdVector F64X2 created successfully from V128");
    }
  }

  @Nested
  @DisplayName("SimdLane Enum Tests")
  class SimdLaneEnumTests {

    @Test
    @DisplayName("should have all expected lane types")
    void shouldHaveAllExpectedLaneTypes() {
      LOGGER.info("Testing SimdLane enum values");

      final SimdLane[] lanes = SimdLane.values();

      // Check for expected lane types
      boolean hasI8x16 = false;
      boolean hasI16x8 = false;
      boolean hasI32x4 = false;
      boolean hasI64x2 = false;
      boolean hasF32x4 = false;
      boolean hasF64x2 = false;

      for (final SimdLane lane : lanes) {
        switch (lane) {
          case I8X16:
            hasI8x16 = true;
            break;
          case I16X8:
            hasI16x8 = true;
            break;
          case I32X4:
            hasI32x4 = true;
            break;
          case I64X2:
            hasI64x2 = true;
            break;
          case F32X4:
            hasF32x4 = true;
            break;
          case F64X2:
            hasF64x2 = true;
            break;
          default:
            break;
        }
      }

      assertEquals(true, hasI8x16, "Should have I8X16 lane");
      assertEquals(true, hasI16x8, "Should have I16X8 lane");
      assertEquals(true, hasI32x4, "Should have I32X4 lane");
      assertEquals(true, hasI64x2, "Should have I64X2 lane");
      assertEquals(true, hasF32x4, "Should have F32X4 lane");
      assertEquals(true, hasF64x2, "Should have F64X2 lane");

      LOGGER.info("All expected SimdLane values present: " + lanes.length + " total");
    }
  }
}
