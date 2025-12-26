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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SimdLane} enum.
 *
 * <p>SimdLane defines SIMD lane types for 128-bit vectors.
 */
@DisplayName("SimdLane Enum Tests")
class SimdLaneTest {

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have six enum values")
    void shouldHaveSixEnumValues() {
      final SimdLane[] values = SimdLane.values();
      assertEquals(6, values.length, "Should have exactly six SIMD lane types");
    }

    @Test
    @DisplayName("should have correct enum order")
    void shouldHaveCorrectEnumOrder() {
      final SimdLane[] expected = {
        SimdLane.I8X16,
        SimdLane.I16X8,
        SimdLane.I32X4,
        SimdLane.I64X2,
        SimdLane.F32X4,
        SimdLane.F64X2
      };
      assertArrayEquals(expected, SimdLane.values(), "Enum values should be in correct order");
    }

    @Test
    @DisplayName("valueOf should return correct enum for I8X16")
    void valueOfShouldReturnCorrectEnumForI8X16() {
      assertEquals(SimdLane.I8X16, SimdLane.valueOf("I8X16"), "valueOf should return I8X16");
    }

    @Test
    @DisplayName("valueOf should return correct enum for I16X8")
    void valueOfShouldReturnCorrectEnumForI16X8() {
      assertEquals(SimdLane.I16X8, SimdLane.valueOf("I16X8"), "valueOf should return I16X8");
    }

    @Test
    @DisplayName("valueOf should return correct enum for I32X4")
    void valueOfShouldReturnCorrectEnumForI32X4() {
      assertEquals(SimdLane.I32X4, SimdLane.valueOf("I32X4"), "valueOf should return I32X4");
    }

    @Test
    @DisplayName("valueOf should return correct enum for I64X2")
    void valueOfShouldReturnCorrectEnumForI64X2() {
      assertEquals(SimdLane.I64X2, SimdLane.valueOf("I64X2"), "valueOf should return I64X2");
    }

    @Test
    @DisplayName("valueOf should return correct enum for F32X4")
    void valueOfShouldReturnCorrectEnumForF32X4() {
      assertEquals(SimdLane.F32X4, SimdLane.valueOf("F32X4"), "valueOf should return F32X4");
    }

    @Test
    @DisplayName("valueOf should return correct enum for F64X2")
    void valueOfShouldReturnCorrectEnumForF64X2() {
      assertEquals(SimdLane.F64X2, SimdLane.valueOf("F64X2"), "valueOf should return F64X2");
    }
  }

  @Nested
  @DisplayName("Bytes Per Element Tests")
  class BytesPerElementTests {

    @Test
    @DisplayName("I8X16 should have 1 byte per element")
    void i8x16ShouldHave1BytePerElement() {
      assertEquals(1, SimdLane.I8X16.getBytesPerElement(), "I8X16 should have 1 byte per element");
    }

    @Test
    @DisplayName("I16X8 should have 2 bytes per element")
    void i16x8ShouldHave2BytesPerElement() {
      assertEquals(2, SimdLane.I16X8.getBytesPerElement(), "I16X8 should have 2 bytes per element");
    }

    @Test
    @DisplayName("I32X4 should have 4 bytes per element")
    void i32x4ShouldHave4BytesPerElement() {
      assertEquals(4, SimdLane.I32X4.getBytesPerElement(), "I32X4 should have 4 bytes per element");
    }

    @Test
    @DisplayName("I64X2 should have 8 bytes per element")
    void i64x2ShouldHave8BytesPerElement() {
      assertEquals(8, SimdLane.I64X2.getBytesPerElement(), "I64X2 should have 8 bytes per element");
    }

    @Test
    @DisplayName("F32X4 should have 4 bytes per element")
    void f32x4ShouldHave4BytesPerElement() {
      assertEquals(4, SimdLane.F32X4.getBytesPerElement(), "F32X4 should have 4 bytes per element");
    }

    @Test
    @DisplayName("F64X2 should have 8 bytes per element")
    void f64x2ShouldHave8BytesPerElement() {
      assertEquals(8, SimdLane.F64X2.getBytesPerElement(), "F64X2 should have 8 bytes per element");
    }
  }

  @Nested
  @DisplayName("Lane Count Tests")
  class LaneCountTests {

    @Test
    @DisplayName("I8X16 should have 16 lanes")
    void i8x16ShouldHave16Lanes() {
      assertEquals(16, SimdLane.I8X16.getLaneCount(), "I8X16 should have 16 lanes");
    }

    @Test
    @DisplayName("I16X8 should have 8 lanes")
    void i16x8ShouldHave8Lanes() {
      assertEquals(8, SimdLane.I16X8.getLaneCount(), "I16X8 should have 8 lanes");
    }

    @Test
    @DisplayName("I32X4 should have 4 lanes")
    void i32x4ShouldHave4Lanes() {
      assertEquals(4, SimdLane.I32X4.getLaneCount(), "I32X4 should have 4 lanes");
    }

    @Test
    @DisplayName("I64X2 should have 2 lanes")
    void i64x2ShouldHave2Lanes() {
      assertEquals(2, SimdLane.I64X2.getLaneCount(), "I64X2 should have 2 lanes");
    }

    @Test
    @DisplayName("F32X4 should have 4 lanes")
    void f32x4ShouldHave4Lanes() {
      assertEquals(4, SimdLane.F32X4.getLaneCount(), "F32X4 should have 4 lanes");
    }

    @Test
    @DisplayName("F64X2 should have 2 lanes")
    void f64x2ShouldHave2Lanes() {
      assertEquals(2, SimdLane.F64X2.getLaneCount(), "F64X2 should have 2 lanes");
    }
  }

  @Nested
  @DisplayName("Vector Size Tests")
  class VectorSizeTests {

    @Test
    @DisplayName("all lane types should have 16 byte vector size")
    void allLaneTypesShouldHave16ByteVectorSize() {
      for (final SimdLane lane : SimdLane.values()) {
        assertEquals(
            16, lane.getVectorSizeBytes(), lane + " should have 16 byte vector size (128 bits)");
      }
    }

    @Test
    @DisplayName("bytes per element times lane count should equal 16")
    void bytesPerElementTimesLaneCountShouldEqual16() {
      for (final SimdLane lane : SimdLane.values()) {
        final int totalBytes = lane.getBytesPerElement() * lane.getLaneCount();
        assertEquals(16, totalBytes, lane + " bytesPerElement * laneCount should equal 16");
      }
    }
  }

  @Nested
  @DisplayName("Display Name Tests")
  class DisplayNameTests {

    @Test
    @DisplayName("I8X16 should have display name i8x16")
    void i8x16ShouldHaveDisplayNameI8x16() {
      assertEquals("i8x16", SimdLane.I8X16.getDisplayName(), "I8X16 display name should be i8x16");
    }

    @Test
    @DisplayName("I16X8 should have display name i16x8")
    void i16x8ShouldHaveDisplayNameI16x8() {
      assertEquals("i16x8", SimdLane.I16X8.getDisplayName(), "I16X8 display name should be i16x8");
    }

    @Test
    @DisplayName("I32X4 should have display name i32x4")
    void i32x4ShouldHaveDisplayNameI32x4() {
      assertEquals("i32x4", SimdLane.I32X4.getDisplayName(), "I32X4 display name should be i32x4");
    }

    @Test
    @DisplayName("I64X2 should have display name i64x2")
    void i64x2ShouldHaveDisplayNameI64x2() {
      assertEquals("i64x2", SimdLane.I64X2.getDisplayName(), "I64X2 display name should be i64x2");
    }

    @Test
    @DisplayName("F32X4 should have display name f32x4")
    void f32x4ShouldHaveDisplayNameF32x4() {
      assertEquals("f32x4", SimdLane.F32X4.getDisplayName(), "F32X4 display name should be f32x4");
    }

    @Test
    @DisplayName("F64X2 should have display name f64x2")
    void f64x2ShouldHaveDisplayNameF64x2() {
      assertEquals("f64x2", SimdLane.F64X2.getDisplayName(), "F64X2 display name should be f64x2");
    }
  }

  @Nested
  @DisplayName("Type Classification Tests")
  class TypeClassificationTests {

    @Test
    @DisplayName("I8X16 should be integer type")
    void i8x16ShouldBeIntegerType() {
      assertTrue(SimdLane.I8X16.isIntegerType(), "I8X16 should be integer type");
      assertFalse(SimdLane.I8X16.isFloatType(), "I8X16 should not be float type");
    }

    @Test
    @DisplayName("I16X8 should be integer type")
    void i16x8ShouldBeIntegerType() {
      assertTrue(SimdLane.I16X8.isIntegerType(), "I16X8 should be integer type");
      assertFalse(SimdLane.I16X8.isFloatType(), "I16X8 should not be float type");
    }

    @Test
    @DisplayName("I32X4 should be integer type")
    void i32x4ShouldBeIntegerType() {
      assertTrue(SimdLane.I32X4.isIntegerType(), "I32X4 should be integer type");
      assertFalse(SimdLane.I32X4.isFloatType(), "I32X4 should not be float type");
    }

    @Test
    @DisplayName("I64X2 should be integer type")
    void i64x2ShouldBeIntegerType() {
      assertTrue(SimdLane.I64X2.isIntegerType(), "I64X2 should be integer type");
      assertFalse(SimdLane.I64X2.isFloatType(), "I64X2 should not be float type");
    }

    @Test
    @DisplayName("F32X4 should be float type")
    void f32x4ShouldBeFloatType() {
      assertTrue(SimdLane.F32X4.isFloatType(), "F32X4 should be float type");
      assertFalse(SimdLane.F32X4.isIntegerType(), "F32X4 should not be integer type");
    }

    @Test
    @DisplayName("F64X2 should be float type")
    void f64x2ShouldBeFloatType() {
      assertTrue(SimdLane.F64X2.isFloatType(), "F64X2 should be float type");
      assertFalse(SimdLane.F64X2.isIntegerType(), "F64X2 should not be integer type");
    }

    @Test
    @DisplayName("integer and float types should be mutually exclusive")
    void integerAndFloatTypesShouldBeMutuallyExclusive() {
      for (final SimdLane lane : SimdLane.values()) {
        final boolean isInteger = lane.isIntegerType();
        final boolean isFloat = lane.isFloatType();
        assertTrue(
            isInteger != isFloat,
            lane + " should be either integer or float type, but not both or neither");
      }
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return display name")
    void toStringShouldReturnDisplayName() {
      for (final SimdLane lane : SimdLane.values()) {
        assertEquals(
            lane.getDisplayName(),
            lane.toString(),
            lane.name() + ".toString() should return display name");
      }
    }
  }
}
