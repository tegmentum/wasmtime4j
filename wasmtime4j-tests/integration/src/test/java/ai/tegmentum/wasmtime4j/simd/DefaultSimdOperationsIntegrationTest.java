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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for DefaultSimdOperations.
 *
 * <p>This class tests the pure Java SIMD implementation without requiring native library calls.
 */
@DisplayName("Default SIMD Operations Integration Tests")
public class DefaultSimdOperationsIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(DefaultSimdOperationsIntegrationTest.class.getName());

  private DefaultSimdOperations simdOps;

  @BeforeEach
  void setUp() {
    simdOps = new DefaultSimdOperations();
    LOGGER.info("Created DefaultSimdOperations instance");
  }

  @Nested
  @DisplayName("Arithmetic Operations Tests")
  class ArithmeticTests {

    @Test
    @DisplayName("Should add i32x4 vectors correctly")
    void shouldAddI32x4VectorsCorrectly() throws Exception {
      LOGGER.info("Testing i32x4 vector addition");

      final byte[] aData = new byte[16];
      final byte[] bData = new byte[16];
      final ByteBuffer aBuf = ByteBuffer.wrap(aData).order(ByteOrder.LITTLE_ENDIAN);
      final ByteBuffer bBuf = ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN);

      // Set values: a = [1, 2, 3, 4], b = [5, 6, 7, 8]
      for (int i = 0; i < 4; i++) {
        aBuf.putInt(i * 4, i + 1);
        bBuf.putInt(i * 4, i + 5);
      }

      final SimdVector a = new SimdVector(SimdLane.I32X4, aData);
      final SimdVector b = new SimdVector(SimdLane.I32X4, bData);

      final SimdVector result = simdOps.add(a, b);

      assertNotNull(result, "Result should not be null");
      assertEquals(SimdLane.I32X4, result.getLane(), "Result should have I32X4 lane");

      final ByteBuffer resBuf =
          ByteBuffer.wrap(result.getDataInternal()).order(ByteOrder.LITTLE_ENDIAN);
      for (int i = 0; i < 4; i++) {
        final int expected = (i + 1) + (i + 5);
        assertEquals(expected, resBuf.getInt(i * 4), "Lane " + i + " should be correct");
      }
      LOGGER.info("i32x4 addition verified");
    }

    @Test
    @DisplayName("Should subtract i32x4 vectors correctly")
    void shouldSubtractI32x4VectorsCorrectly() throws Exception {
      LOGGER.info("Testing i32x4 vector subtraction");

      final byte[] aData = new byte[16];
      final byte[] bData = new byte[16];
      final ByteBuffer aBuf = ByteBuffer.wrap(aData).order(ByteOrder.LITTLE_ENDIAN);
      final ByteBuffer bBuf = ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN);

      // Set values: a = [10, 20, 30, 40], b = [1, 2, 3, 4]
      for (int i = 0; i < 4; i++) {
        aBuf.putInt(i * 4, (i + 1) * 10);
        bBuf.putInt(i * 4, i + 1);
      }

      final SimdVector a = new SimdVector(SimdLane.I32X4, aData);
      final SimdVector b = new SimdVector(SimdLane.I32X4, bData);

      final SimdVector result = simdOps.subtract(a, b);

      assertNotNull(result, "Result should not be null");
      final ByteBuffer resBuf =
          ByteBuffer.wrap(result.getDataInternal()).order(ByteOrder.LITTLE_ENDIAN);
      for (int i = 0; i < 4; i++) {
        final int expected = ((i + 1) * 10) - (i + 1);
        assertEquals(expected, resBuf.getInt(i * 4), "Lane " + i + " should be correct");
      }
      LOGGER.info("i32x4 subtraction verified");
    }

    @Test
    @DisplayName("Should multiply i32x4 vectors correctly")
    void shouldMultiplyI32x4VectorsCorrectly() throws Exception {
      LOGGER.info("Testing i32x4 vector multiplication");

      final byte[] aData = new byte[16];
      final byte[] bData = new byte[16];
      final ByteBuffer aBuf = ByteBuffer.wrap(aData).order(ByteOrder.LITTLE_ENDIAN);
      final ByteBuffer bBuf = ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN);

      // Set values: a = [2, 3, 4, 5], b = [3, 4, 5, 6]
      for (int i = 0; i < 4; i++) {
        aBuf.putInt(i * 4, i + 2);
        bBuf.putInt(i * 4, i + 3);
      }

      final SimdVector a = new SimdVector(SimdLane.I32X4, aData);
      final SimdVector b = new SimdVector(SimdLane.I32X4, bData);

      final SimdVector result = simdOps.multiply(a, b);

      assertNotNull(result, "Result should not be null");
      final ByteBuffer resBuf =
          ByteBuffer.wrap(result.getDataInternal()).order(ByteOrder.LITTLE_ENDIAN);
      for (int i = 0; i < 4; i++) {
        final int expected = (i + 2) * (i + 3);
        assertEquals(expected, resBuf.getInt(i * 4), "Lane " + i + " should be correct");
      }
      LOGGER.info("i32x4 multiplication verified");
    }

    @Test
    @DisplayName("Should divide f32x4 vectors correctly")
    void shouldDivideF32x4VectorsCorrectly() throws Exception {
      LOGGER.info("Testing f32x4 vector division");

      final byte[] aData = new byte[16];
      final byte[] bData = new byte[16];
      final ByteBuffer aBuf = ByteBuffer.wrap(aData).order(ByteOrder.LITTLE_ENDIAN);
      final ByteBuffer bBuf = ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN);

      // Set values: a = [10.0, 20.0, 30.0, 40.0], b = [2.0, 4.0, 5.0, 8.0]
      aBuf.putFloat(0, 10.0f);
      aBuf.putFloat(4, 20.0f);
      aBuf.putFloat(8, 30.0f);
      aBuf.putFloat(12, 40.0f);
      bBuf.putFloat(0, 2.0f);
      bBuf.putFloat(4, 4.0f);
      bBuf.putFloat(8, 5.0f);
      bBuf.putFloat(12, 8.0f);

      final SimdVector a = new SimdVector(SimdLane.F32X4, aData);
      final SimdVector b = new SimdVector(SimdLane.F32X4, bData);

      final SimdVector result = simdOps.divide(a, b);

      assertNotNull(result, "Result should not be null");
      final ByteBuffer resBuf =
          ByteBuffer.wrap(result.getDataInternal()).order(ByteOrder.LITTLE_ENDIAN);
      assertEquals(5.0f, resBuf.getFloat(0), 0.001f, "Lane 0 should be 5.0");
      assertEquals(5.0f, resBuf.getFloat(4), 0.001f, "Lane 1 should be 5.0");
      assertEquals(6.0f, resBuf.getFloat(8), 0.001f, "Lane 2 should be 6.0");
      assertEquals(5.0f, resBuf.getFloat(12), 0.001f, "Lane 3 should be 5.0");
      LOGGER.info("f32x4 division verified");
    }
  }

  @Nested
  @DisplayName("Bitwise Operations Tests")
  class BitwiseTests {

    @Test
    @DisplayName("Should perform AND operation correctly")
    void shouldPerformAndOperationCorrectly() throws Exception {
      LOGGER.info("Testing bitwise AND operation");

      final byte[] aData = new byte[16];
      final byte[] bData = new byte[16];
      for (int i = 0; i < 16; i++) {
        aData[i] = (byte) 0xFF;
        bData[i] = (byte) 0x0F;
      }

      final SimdVector a = new SimdVector(SimdLane.I8X16, aData);
      final SimdVector b = new SimdVector(SimdLane.I8X16, bData);

      final SimdVector result = simdOps.and(a, b);

      assertNotNull(result, "Result should not be null");
      for (int i = 0; i < 16; i++) {
        assertEquals((byte) 0x0F, result.getDataInternal()[i], "Byte " + i + " should be 0x0F");
      }
      LOGGER.info("Bitwise AND verified");
    }

    @Test
    @DisplayName("Should perform OR operation correctly")
    void shouldPerformOrOperationCorrectly() throws Exception {
      LOGGER.info("Testing bitwise OR operation");

      final byte[] aData = new byte[16];
      final byte[] bData = new byte[16];
      for (int i = 0; i < 16; i++) {
        aData[i] = (byte) 0xF0;
        bData[i] = (byte) 0x0F;
      }

      final SimdVector a = new SimdVector(SimdLane.I8X16, aData);
      final SimdVector b = new SimdVector(SimdLane.I8X16, bData);

      final SimdVector result = simdOps.or(a, b);

      assertNotNull(result, "Result should not be null");
      for (int i = 0; i < 16; i++) {
        assertEquals((byte) 0xFF, result.getDataInternal()[i], "Byte " + i + " should be 0xFF");
      }
      LOGGER.info("Bitwise OR verified");
    }

    @Test
    @DisplayName("Should perform XOR operation correctly")
    void shouldPerformXorOperationCorrectly() throws Exception {
      LOGGER.info("Testing bitwise XOR operation");

      final byte[] aData = new byte[16];
      final byte[] bData = new byte[16];
      for (int i = 0; i < 16; i++) {
        aData[i] = (byte) 0xFF;
        bData[i] = (byte) 0xFF;
      }

      final SimdVector a = new SimdVector(SimdLane.I8X16, aData);
      final SimdVector b = new SimdVector(SimdLane.I8X16, bData);

      final SimdVector result = simdOps.xor(a, b);

      assertNotNull(result, "Result should not be null");
      for (int i = 0; i < 16; i++) {
        assertEquals((byte) 0x00, result.getDataInternal()[i], "Byte " + i + " should be 0x00");
      }
      LOGGER.info("Bitwise XOR verified");
    }

    @Test
    @DisplayName("Should perform NOT operation correctly")
    void shouldPerformNotOperationCorrectly() throws Exception {
      LOGGER.info("Testing bitwise NOT operation");

      final byte[] aData = new byte[16];
      for (int i = 0; i < 16; i++) {
        aData[i] = (byte) 0xF0;
      }

      final SimdVector a = new SimdVector(SimdLane.I8X16, aData);

      final SimdVector result = simdOps.not(a);

      assertNotNull(result, "Result should not be null");
      for (int i = 0; i < 16; i++) {
        assertEquals((byte) 0x0F, result.getDataInternal()[i], "Byte " + i + " should be 0x0F");
      }
      LOGGER.info("Bitwise NOT verified");
    }
  }

  @Nested
  @DisplayName("Comparison Operations Tests")
  class ComparisonTests {

    @Test
    @DisplayName("Should perform equals comparison correctly")
    void shouldPerformEqualsComparisonCorrectly() throws Exception {
      LOGGER.info("Testing equals comparison");

      final byte[] aData = new byte[16];
      final byte[] bData = new byte[16];
      final ByteBuffer aBuf = ByteBuffer.wrap(aData).order(ByteOrder.LITTLE_ENDIAN);
      final ByteBuffer bBuf = ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN);

      // Set values: a = [1, 2, 3, 4], b = [1, 5, 3, 6]
      aBuf.putInt(0, 1);
      aBuf.putInt(4, 2);
      aBuf.putInt(8, 3);
      aBuf.putInt(12, 4);
      bBuf.putInt(0, 1);
      bBuf.putInt(4, 5);
      bBuf.putInt(8, 3);
      bBuf.putInt(12, 6);

      final SimdVector a = new SimdVector(SimdLane.I32X4, aData);
      final SimdVector b = new SimdVector(SimdLane.I32X4, bData);

      final SimdVector result = simdOps.equals(a, b);

      assertNotNull(result, "Result should not be null");
      final ByteBuffer resBuf =
          ByteBuffer.wrap(result.getDataInternal()).order(ByteOrder.LITTLE_ENDIAN);
      assertEquals(0xFFFFFFFF, resBuf.getInt(0), "Lane 0 should be all 1s (equal)");
      assertEquals(0x00000000, resBuf.getInt(4), "Lane 1 should be all 0s (not equal)");
      assertEquals(0xFFFFFFFF, resBuf.getInt(8), "Lane 2 should be all 1s (equal)");
      assertEquals(0x00000000, resBuf.getInt(12), "Lane 3 should be all 0s (not equal)");
      LOGGER.info("Equals comparison verified");
    }

    @Test
    @DisplayName("Should perform less than comparison correctly")
    void shouldPerformLessThanComparisonCorrectly() throws Exception {
      LOGGER.info("Testing less than comparison");

      final byte[] aData = new byte[16];
      final byte[] bData = new byte[16];
      final ByteBuffer aBuf = ByteBuffer.wrap(aData).order(ByteOrder.LITTLE_ENDIAN);
      final ByteBuffer bBuf = ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN);

      // Set values: a = [1, 5, 3, 4], b = [2, 3, 3, 5]
      aBuf.putInt(0, 1);
      aBuf.putInt(4, 5);
      aBuf.putInt(8, 3);
      aBuf.putInt(12, 4);
      bBuf.putInt(0, 2);
      bBuf.putInt(4, 3);
      bBuf.putInt(8, 3);
      bBuf.putInt(12, 5);

      final SimdVector a = new SimdVector(SimdLane.I32X4, aData);
      final SimdVector b = new SimdVector(SimdLane.I32X4, bData);

      final SimdVector result = simdOps.lessThan(a, b);

      assertNotNull(result, "Result should not be null");
      final ByteBuffer resBuf =
          ByteBuffer.wrap(result.getDataInternal()).order(ByteOrder.LITTLE_ENDIAN);
      assertEquals(0xFFFFFFFF, resBuf.getInt(0), "Lane 0: 1 < 2 should be true");
      assertEquals(0x00000000, resBuf.getInt(4), "Lane 1: 5 < 3 should be false");
      assertEquals(0x00000000, resBuf.getInt(8), "Lane 2: 3 < 3 should be false");
      assertEquals(0xFFFFFFFF, resBuf.getInt(12), "Lane 3: 4 < 5 should be true");
      LOGGER.info("Less than comparison verified");
    }
  }

  @Nested
  @DisplayName("Lane Operations Tests")
  class LaneOperationsTests {

    @Test
    @DisplayName("Should extract lane correctly")
    void shouldExtractLaneCorrectly() throws Exception {
      LOGGER.info("Testing lane extraction");

      final byte[] data = new byte[16];
      final ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
      buf.putInt(0, 100);
      buf.putInt(4, 200);
      buf.putInt(8, 300);
      buf.putInt(12, 400);

      final SimdVector vector = new SimdVector(SimdLane.I32X4, data);

      assertEquals(100, simdOps.extractLaneI32(vector, 0), "Lane 0 should be 100");
      assertEquals(200, simdOps.extractLaneI32(vector, 1), "Lane 1 should be 200");
      assertEquals(300, simdOps.extractLaneI32(vector, 2), "Lane 2 should be 300");
      assertEquals(400, simdOps.extractLaneI32(vector, 3), "Lane 3 should be 400");
      LOGGER.info("Lane extraction verified");
    }

    @Test
    @DisplayName("Should replace lane correctly")
    void shouldReplaceLaneCorrectly() throws Exception {
      LOGGER.info("Testing lane replacement");

      final byte[] data = new byte[16];
      final ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
      buf.putInt(0, 100);
      buf.putInt(4, 200);
      buf.putInt(8, 300);
      buf.putInt(12, 400);

      final SimdVector vector = new SimdVector(SimdLane.I32X4, data);

      final SimdVector result = simdOps.replaceLaneI32(vector, 1, 999);

      final ByteBuffer resBuf =
          ByteBuffer.wrap(result.getDataInternal()).order(ByteOrder.LITTLE_ENDIAN);
      assertEquals(100, resBuf.getInt(0), "Lane 0 should be unchanged");
      assertEquals(999, resBuf.getInt(4), "Lane 1 should be 999");
      assertEquals(300, resBuf.getInt(8), "Lane 2 should be unchanged");
      assertEquals(400, resBuf.getInt(12), "Lane 3 should be unchanged");
      LOGGER.info("Lane replacement verified");
    }
  }

  @Nested
  @DisplayName("Math Operations Tests")
  class MathOperationsTests {

    @Test
    @DisplayName("Should compute sqrt correctly for f32x4")
    void shouldComputeSqrtCorrectlyForF32x4() throws Exception {
      LOGGER.info("Testing sqrt operation");

      final byte[] data = new byte[16];
      final ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
      buf.putFloat(0, 4.0f);
      buf.putFloat(4, 9.0f);
      buf.putFloat(8, 16.0f);
      buf.putFloat(12, 25.0f);

      final SimdVector vector = new SimdVector(SimdLane.F32X4, data);

      final SimdVector result = simdOps.sqrt(vector);

      final ByteBuffer resBuf =
          ByteBuffer.wrap(result.getDataInternal()).order(ByteOrder.LITTLE_ENDIAN);
      assertEquals(2.0f, resBuf.getFloat(0), 0.001f, "sqrt(4) should be 2");
      assertEquals(3.0f, resBuf.getFloat(4), 0.001f, "sqrt(9) should be 3");
      assertEquals(4.0f, resBuf.getFloat(8), 0.001f, "sqrt(16) should be 4");
      assertEquals(5.0f, resBuf.getFloat(12), 0.001f, "sqrt(25) should be 5");
      LOGGER.info("sqrt operation verified");
    }

    @Test
    @DisplayName("Should compute reciprocal correctly for f32x4")
    void shouldComputeReciprocalCorrectlyForF32x4() throws Exception {
      LOGGER.info("Testing reciprocal operation");

      final byte[] data = new byte[16];
      final ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
      buf.putFloat(0, 2.0f);
      buf.putFloat(4, 4.0f);
      buf.putFloat(8, 5.0f);
      buf.putFloat(12, 10.0f);

      final SimdVector vector = new SimdVector(SimdLane.F32X4, data);

      final SimdVector result = simdOps.reciprocal(vector);

      final ByteBuffer resBuf =
          ByteBuffer.wrap(result.getDataInternal()).order(ByteOrder.LITTLE_ENDIAN);
      assertEquals(0.5f, resBuf.getFloat(0), 0.001f, "1/2 should be 0.5");
      assertEquals(0.25f, resBuf.getFloat(4), 0.001f, "1/4 should be 0.25");
      assertEquals(0.2f, resBuf.getFloat(8), 0.001f, "1/5 should be 0.2");
      assertEquals(0.1f, resBuf.getFloat(12), 0.001f, "1/10 should be 0.1");
      LOGGER.info("reciprocal operation verified");
    }

    @Test
    @DisplayName("Should compute fma correctly for f32x4")
    void shouldComputeFmaCorrectlyForF32x4() throws Exception {
      LOGGER.info("Testing FMA operation");

      final byte[] aData = new byte[16];
      final byte[] bData = new byte[16];
      final byte[] cData = new byte[16];
      final ByteBuffer aBuf = ByteBuffer.wrap(aData).order(ByteOrder.LITTLE_ENDIAN);
      final ByteBuffer bBuf = ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN);
      final ByteBuffer cBuf = ByteBuffer.wrap(cData).order(ByteOrder.LITTLE_ENDIAN);

      // FMA: a * b + c
      for (int i = 0; i < 4; i++) {
        aBuf.putFloat(i * 4, 2.0f);
        bBuf.putFloat(i * 4, 3.0f);
        cBuf.putFloat(i * 4, 1.0f);
      }

      final SimdVector a = new SimdVector(SimdLane.F32X4, aData);
      final SimdVector b = new SimdVector(SimdLane.F32X4, bData);
      final SimdVector c = new SimdVector(SimdLane.F32X4, cData);

      final SimdVector result = simdOps.fma(a, b, c);

      final ByteBuffer resBuf =
          ByteBuffer.wrap(result.getDataInternal()).order(ByteOrder.LITTLE_ENDIAN);
      for (int i = 0; i < 4; i++) {
        assertEquals(7.0f, resBuf.getFloat(i * 4), 0.001f, "2*3+1 should be 7 at lane " + i);
      }
      LOGGER.info("FMA operation verified");
    }
  }

  @Nested
  @DisplayName("Reduction Operations Tests")
  class ReductionTests {

    @Test
    @DisplayName("Should compute horizontal sum correctly")
    void shouldComputeHorizontalSumCorrectly() throws Exception {
      LOGGER.info("Testing horizontal sum");

      final byte[] data = new byte[16];
      final ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
      buf.putInt(0, 10);
      buf.putInt(4, 20);
      buf.putInt(8, 30);
      buf.putInt(12, 40);

      final SimdVector vector = new SimdVector(SimdLane.I32X4, data);

      final float sum = simdOps.horizontalSum(vector);
      assertEquals(100.0f, sum, 0.001f, "Sum should be 100");
      LOGGER.info("Horizontal sum verified");
    }

    @Test
    @DisplayName("Should compute horizontal min correctly")
    void shouldComputeHorizontalMinCorrectly() throws Exception {
      LOGGER.info("Testing horizontal min");

      final byte[] data = new byte[16];
      final ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
      buf.putInt(0, 50);
      buf.putInt(4, 10);
      buf.putInt(8, 30);
      buf.putInt(12, 20);

      final SimdVector vector = new SimdVector(SimdLane.I32X4, data);

      final float min = simdOps.horizontalMin(vector);
      assertEquals(10.0f, min, 0.001f, "Min should be 10");
      LOGGER.info("Horizontal min verified");
    }

    @Test
    @DisplayName("Should compute horizontal max correctly")
    void shouldComputeHorizontalMaxCorrectly() throws Exception {
      LOGGER.info("Testing horizontal max");

      final byte[] data = new byte[16];
      final ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
      buf.putInt(0, 50);
      buf.putInt(4, 10);
      buf.putInt(8, 30);
      buf.putInt(12, 20);

      final SimdVector vector = new SimdVector(SimdLane.I32X4, data);

      final float max = simdOps.horizontalMax(vector);
      assertEquals(50.0f, max, 0.001f, "Max should be 50");
      LOGGER.info("Horizontal max verified");
    }
  }

  @Nested
  @DisplayName("Capability Tests")
  class CapabilityTests {

    @Test
    @DisplayName("Should report SIMD as supported")
    void shouldReportSimdAsSupported() {
      LOGGER.info("Testing SIMD capability reporting");

      assertTrue(simdOps.isSimdSupported(), "SIMD should be supported");
      LOGGER.info("SIMD support verified");
    }

    @Test
    @DisplayName("Should return detailed capabilities string")
    void shouldReturnDetailedCapabilitiesString() {
      LOGGER.info("Testing capabilities string");

      final String capabilities = simdOps.getSimdCapabilities();
      assertNotNull(capabilities, "Capabilities string should not be null");
      assertTrue(capabilities.contains("Arithmetic"), "Should mention arithmetic operations");
      assertTrue(capabilities.contains("Bitwise"), "Should mention bitwise operations");
      LOGGER.info("Capabilities: " + capabilities);
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should throw on null vector in add")
    void shouldThrowOnNullVectorInAdd() {
      LOGGER.info("Testing null vector handling in add");

      final byte[] data = new byte[16];
      final SimdVector a = new SimdVector(SimdLane.I32X4, data);

      assertThrows(
          NullPointerException.class,
          () -> simdOps.add(null, a),
          "Should throw on null first vector");
      assertThrows(
          NullPointerException.class,
          () -> simdOps.add(a, null),
          "Should throw on null second vector");
      LOGGER.info("Null vector handling verified");
    }

    @Test
    @DisplayName("Should throw on lane type mismatch")
    void shouldThrowOnLaneTypeMismatch() {
      LOGGER.info("Testing lane type mismatch handling");

      final byte[] data = new byte[16];
      final SimdVector i32Vector = new SimdVector(SimdLane.I32X4, data);
      final SimdVector f32Vector = new SimdVector(SimdLane.F32X4, data);

      assertThrows(
          IllegalArgumentException.class,
          () -> simdOps.add(i32Vector, f32Vector),
          "Should throw on lane type mismatch");
      LOGGER.info("Lane type mismatch handling verified");
    }

    @Test
    @DisplayName("Should throw on invalid lane index")
    void shouldThrowOnInvalidLaneIndex() {
      LOGGER.info("Testing invalid lane index handling");

      final byte[] data = new byte[16];
      final SimdVector vector = new SimdVector(SimdLane.I32X4, data);

      assertThrows(
          IllegalArgumentException.class,
          () -> simdOps.extractLaneI32(vector, -1),
          "Should throw on negative index");
      assertThrows(
          IllegalArgumentException.class,
          () -> simdOps.extractLaneI32(vector, 4),
          "Should throw on out of bounds index");
      LOGGER.info("Invalid lane index handling verified");
    }
  }
}
