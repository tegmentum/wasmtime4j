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
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for SIMD Operations - WebAssembly 128-bit vector operations.
 *
 * <p>These tests verify vector creation, arithmetic, bitwise operations, comparisons, lane
 * operations, and shuffle.
 *
 * @since 1.0.0
 */
@DisplayName("SIMD Operations Integration Tests")
public final class SimdOperationsIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(SimdOperationsIntegrationTest.class.getName());

  private static boolean simdAvailable = false;
  private static WasmRuntime sharedRuntime;

  @BeforeAll
  static void checkSimdAvailable() {
    try {
      sharedRuntime = WasmRuntimeFactory.create();
      final SimdOperations simd = sharedRuntime.getSimdOperations();
      simdAvailable = simd != null && simd.isSimdSupported();
      if (simdAvailable) {
        LOGGER.info("SIMD is available: " + simd.getSimdCapabilities());
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
  void setUp(final TestInfo testInfo) throws Exception {
    LOGGER.info("Setting up: " + testInfo.getDisplayName());
    if (simdAvailable && sharedRuntime != null) {
      simd = sharedRuntime.getSimdOperations();
    }
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    LOGGER.info("Tearing down: " + testInfo.getDisplayName());
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
  @DisplayName("Vector Creation Tests")
  class VectorCreationTests {

    @Test
    @DisplayName("should create v128 from i32x4")
    void shouldCreateV128FromI32x4(final TestInfo testInfo) throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create a vector with 4 lanes of 32-bit integers using splat
      final SimdVector vector = SimdVector.splatI32(SimdLane.I32X4, 42);

      assertNotNull(vector, "Vector should not be null");
      assertEquals(SimdLane.I32X4, vector.getLane(), "Lane type should be I32X4");
      assertEquals(16, vector.getData().length, "Vector data should be 16 bytes");
      LOGGER.info("Created I32X4 vector: " + vector);
    }

    @Test
    @DisplayName("should create v128 from f32x4")
    void shouldCreateV128FromF32x4(final TestInfo testInfo) throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create a vector with 4 lanes of 32-bit floats using splat
      final SimdVector vector = SimdVector.splatF32(SimdLane.F32X4, 3.14f);

      assertNotNull(vector, "Vector should not be null");
      assertEquals(SimdLane.F32X4, vector.getLane(), "Lane type should be F32X4");
      assertEquals(16, vector.getData().length, "Vector data should be 16 bytes");
      LOGGER.info("Created F32X4 vector: " + vector);
    }

    @Test
    @DisplayName("should create v128 from i64x2")
    void shouldCreateV128FromI64x2(final TestInfo testInfo) throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create a vector with 2 lanes of 64-bit integers using splat
      final SimdVector vector = SimdVector.splatI32(SimdLane.I64X2, 100);

      assertNotNull(vector, "Vector should not be null");
      assertEquals(SimdLane.I64X2, vector.getLane(), "Lane type should be I64X2");
      assertEquals(16, vector.getData().length, "Vector data should be 16 bytes");
      LOGGER.info("Created I64X2 vector: " + vector);
    }
  }

  @Nested
  @DisplayName("Vector Arithmetic Tests")
  class VectorArithmeticTests {

    @Test
    @DisplayName("should add i32x4 vectors")
    void shouldAddI32x4Vectors(final TestInfo testInfo) throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create two vectors: [10, 10, 10, 10] and [20, 20, 20, 20]
      final SimdVector a = SimdVector.splatI32(SimdLane.I32X4, 10);
      final SimdVector b = SimdVector.splatI32(SimdLane.I32X4, 20);

      // Add them
      final SimdVector result = simd.add(a, b);

      assertNotNull(result, "Result should not be null");
      assertEquals(SimdLane.I32X4, result.getLane(), "Result lane type should be I32X4");

      // Extract a lane to verify the addition worked
      final int lane0 = simd.extractLaneI32(result, 0);
      assertEquals(30, lane0, "Lane 0 should be 10 + 20 = 30");
      LOGGER.info("Add result lane 0: " + lane0);
    }

    @Test
    @DisplayName("should subtract i32x4 vectors")
    void shouldSubtractI32x4Vectors(final TestInfo testInfo) throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create two vectors: [50, 50, 50, 50] and [20, 20, 20, 20]
      final SimdVector a = SimdVector.splatI32(SimdLane.I32X4, 50);
      final SimdVector b = SimdVector.splatI32(SimdLane.I32X4, 20);

      // Subtract them
      final SimdVector result = simd.subtract(a, b);

      assertNotNull(result, "Result should not be null");
      assertEquals(SimdLane.I32X4, result.getLane(), "Result lane type should be I32X4");

      // Extract a lane to verify the subtraction worked
      final int lane0 = simd.extractLaneI32(result, 0);
      assertEquals(30, lane0, "Lane 0 should be 50 - 20 = 30");
      LOGGER.info("Subtract result lane 0: " + lane0);
    }

    @Test
    @DisplayName("should multiply i32x4 vectors")
    void shouldMultiplyI32x4Vectors(final TestInfo testInfo) throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create two vectors: [5, 5, 5, 5] and [6, 6, 6, 6]
      final SimdVector a = SimdVector.splatI32(SimdLane.I32X4, 5);
      final SimdVector b = SimdVector.splatI32(SimdLane.I32X4, 6);

      // Multiply them
      final SimdVector result = simd.multiply(a, b);

      assertNotNull(result, "Result should not be null");
      assertEquals(SimdLane.I32X4, result.getLane(), "Result lane type should be I32X4");

      // Extract a lane to verify the multiplication worked
      final int lane0 = simd.extractLaneI32(result, 0);
      assertEquals(30, lane0, "Lane 0 should be 5 * 6 = 30");
      LOGGER.info("Multiply result lane 0: " + lane0);
    }

    @Test
    @DisplayName("should divide f32x4 vectors")
    void shouldDivideF32x4Vectors(final TestInfo testInfo) throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create two vectors: [100.0, 100.0, 100.0, 100.0] and [5.0, 5.0, 5.0, 5.0]
      final SimdVector a = SimdVector.splatF32(SimdLane.F32X4, 100.0f);
      final SimdVector b = SimdVector.splatF32(SimdLane.F32X4, 5.0f);

      // Divide them
      final SimdVector result = simd.divide(a, b);

      assertNotNull(result, "Result should not be null");
      assertEquals(SimdLane.F32X4, result.getLane(), "Result lane type should be F32X4");
      LOGGER.info("Divide result: " + result);
    }
  }

  @Nested
  @DisplayName("Vector Bitwise Tests")
  class VectorBitwiseTests {

    @Test
    @DisplayName("should perform v128 AND")
    void shouldPerformV128And(final TestInfo testInfo) throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create vectors: 0xFF (all bits) and 0x0F (lower nibble)
      final SimdVector a = SimdVector.splatI32(SimdLane.I32X4, 0xFF);
      final SimdVector b = SimdVector.splatI32(SimdLane.I32X4, 0x0F);

      // AND them
      final SimdVector result = simd.and(a, b);

      assertNotNull(result, "Result should not be null");
      final int lane0 = simd.extractLaneI32(result, 0);
      assertEquals(0x0F, lane0, "Lane 0 should be 0xFF & 0x0F = 0x0F");
      LOGGER.info("AND result lane 0: 0x" + Integer.toHexString(lane0));
    }

    @Test
    @DisplayName("should perform v128 OR")
    void shouldPerformV128Or(final TestInfo testInfo) throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create vectors: 0xF0 and 0x0F
      final SimdVector a = SimdVector.splatI32(SimdLane.I32X4, 0xF0);
      final SimdVector b = SimdVector.splatI32(SimdLane.I32X4, 0x0F);

      // OR them
      final SimdVector result = simd.or(a, b);

      assertNotNull(result, "Result should not be null");
      final int lane0 = simd.extractLaneI32(result, 0);
      assertEquals(0xFF, lane0, "Lane 0 should be 0xF0 | 0x0F = 0xFF");
      LOGGER.info("OR result lane 0: 0x" + Integer.toHexString(lane0));
    }

    @Test
    @DisplayName("should perform v128 XOR")
    void shouldPerformV128Xor(final TestInfo testInfo) throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create vectors: 0xFF and 0xFF
      final SimdVector a = SimdVector.splatI32(SimdLane.I32X4, 0xFF);
      final SimdVector b = SimdVector.splatI32(SimdLane.I32X4, 0xFF);

      // XOR them
      final SimdVector result = simd.xor(a, b);

      assertNotNull(result, "Result should not be null");
      final int lane0 = simd.extractLaneI32(result, 0);
      assertEquals(0, lane0, "Lane 0 should be 0xFF ^ 0xFF = 0");
      LOGGER.info("XOR result lane 0: " + lane0);
    }
  }

  @Nested
  @DisplayName("Vector Comparison Tests")
  class VectorComparisonTests {

    @Test
    @DisplayName("should compare i32x4 equality")
    void shouldCompareI32x4Equality(final TestInfo testInfo) throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create equal vectors
      final SimdVector a = SimdVector.splatI32(SimdLane.I32X4, 42);
      final SimdVector b = SimdVector.splatI32(SimdLane.I32X4, 42);

      // Compare for equality
      final SimdVector result = simd.equals(a, b);

      assertNotNull(result, "Result should not be null");
      // All-ones (0xFFFFFFFF) means equal
      final int lane0 = simd.extractLaneI32(result, 0);
      assertEquals(-1, lane0, "Lane 0 should be all-ones (-1) for equal values");
      LOGGER.info("Equality result lane 0: " + lane0);
    }

    @Test
    @DisplayName("should compare i32x4 greater than")
    void shouldCompareI32x4GreaterThan(final TestInfo testInfo) throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create vectors: [50, 50, 50, 50] and [30, 30, 30, 30]
      final SimdVector a = SimdVector.splatI32(SimdLane.I32X4, 50);
      final SimdVector b = SimdVector.splatI32(SimdLane.I32X4, 30);

      // Compare for greater than
      final SimdVector result = simd.greaterThan(a, b);

      assertNotNull(result, "Result should not be null");
      // All-ones (0xFFFFFFFF) means true
      final int lane0 = simd.extractLaneI32(result, 0);
      assertEquals(-1, lane0, "Lane 0 should be all-ones (-1) for 50 > 30");
      LOGGER.info("Greater than result lane 0: " + lane0);
    }
  }

  @Nested
  @DisplayName("Lane Operation Tests")
  class LaneOperationTests {

    @Test
    @DisplayName("should extract lane from i32x4")
    void shouldExtractLaneFromI32x4(final TestInfo testInfo) throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create a vector with a known value
      final SimdVector vector = SimdVector.splatI32(SimdLane.I32X4, 12345);

      // Extract each lane
      for (int i = 0; i < 4; i++) {
        final int value = simd.extractLaneI32(vector, i);
        assertEquals(12345, value, "Lane " + i + " should contain 12345");
        LOGGER.info("Lane " + i + " value: " + value);
      }
    }

    @Test
    @DisplayName("should replace lane in i32x4")
    void shouldReplaceLaneInI32x4(final TestInfo testInfo) throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create a vector with zeros
      final SimdVector original = SimdVector.splatI32(SimdLane.I32X4, 0);

      // Replace lane 2 with 999
      final SimdVector modified = simd.replaceLaneI32(original, 2, 999);

      assertNotNull(modified, "Modified vector should not be null");

      // Verify lane 2 was replaced
      final int lane2 = simd.extractLaneI32(modified, 2);
      assertEquals(999, lane2, "Lane 2 should be 999 after replacement");

      // Verify other lanes are still 0
      final int lane0 = simd.extractLaneI32(modified, 0);
      assertEquals(0, lane0, "Lane 0 should still be 0");

      LOGGER.info("After replace: lane 0 = " + lane0 + ", lane 2 = " + lane2);
    }
  }

  @Nested
  @DisplayName("Shuffle Tests")
  class ShuffleTests {

    @Test
    @DisplayName("should shuffle v128")
    void shouldShuffleV128(final TestInfo testInfo) throws Exception {
      assumeSimdAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create two vectors with different patterns
      final SimdVector a = SimdVector.splatI32(SimdLane.I32X4, 1);
      final SimdVector b = SimdVector.splatI32(SimdLane.I32X4, 2);

      // Shuffle indices: select bytes from both vectors
      // Indices 0-15 select from a, indices 16-31 select from b
      final int[] indices = {0, 1, 2, 3, 4, 5, 6, 7, 16, 17, 18, 19, 20, 21, 22, 23};

      // Shuffle
      final SimdVector result = simd.shuffle(a, b, indices);

      assertNotNull(result, "Result should not be null");
      LOGGER.info("Shuffle result: " + result);
    }
  }
}
