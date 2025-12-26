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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SimdVector} class.
 *
 * <p>SimdVector represents an immutable 128-bit SIMD vector.
 */
@DisplayName("SimdVector Class Tests")
class SimdVectorTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(SimdVector.class.getModifiers()), "SimdVector should be a final class");
    }

    @Test
    @DisplayName("should have getLane method")
    void shouldHaveGetLaneMethod() throws NoSuchMethodException {
      final var method = SimdVector.class.getMethod("getLane");
      assertNotNull(method, "getLane method should exist");
      assertEquals(SimdLane.class, method.getReturnType(), "Should return SimdLane");
    }

    @Test
    @DisplayName("should have getData method")
    void shouldHaveGetDataMethod() throws NoSuchMethodException {
      final var method = SimdVector.class.getMethod("getData");
      assertNotNull(method, "getData method should exist");
      assertEquals(byte[].class, method.getReturnType(), "Should return byte[]");
    }

    @Test
    @DisplayName("should have getDataInternal method")
    void shouldHaveGetDataInternalMethod() throws NoSuchMethodException {
      final var method = SimdVector.class.getMethod("getDataInternal");
      assertNotNull(method, "getDataInternal method should exist");
      assertEquals(byte[].class, method.getReturnType(), "Should return byte[]");
    }

    @Test
    @DisplayName("should have static splatI32 method")
    void shouldHaveStaticSplatI32Method() throws NoSuchMethodException {
      final var method = SimdVector.class.getMethod("splatI32", SimdLane.class, int.class);
      assertNotNull(method, "splatI32 method should exist");
      assertEquals(SimdVector.class, method.getReturnType(), "Should return SimdVector");
      assertTrue(Modifier.isStatic(method.getModifiers()), "splatI32 should be static");
    }

    @Test
    @DisplayName("should have static splatF32 method")
    void shouldHaveStaticSplatF32Method() throws NoSuchMethodException {
      final var method = SimdVector.class.getMethod("splatF32", SimdLane.class, float.class);
      assertNotNull(method, "splatF32 method should exist");
      assertEquals(SimdVector.class, method.getReturnType(), "Should return SimdVector");
      assertTrue(Modifier.isStatic(method.getModifiers()), "splatF32 should be static");
    }

    @Test
    @DisplayName("should have static splatF64 method")
    void shouldHaveStaticSplatF64Method() throws NoSuchMethodException {
      final var method = SimdVector.class.getMethod("splatF64", SimdLane.class, double.class);
      assertNotNull(method, "splatF64 method should exist");
      assertEquals(SimdVector.class, method.getReturnType(), "Should return SimdVector");
      assertTrue(Modifier.isStatic(method.getModifiers()), "splatF64 should be static");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create vector with valid lane and data")
    void shouldCreateVectorWithValidLaneAndData() {
      final byte[] data = new byte[16];
      final SimdVector vector = new SimdVector(SimdLane.I32X4, data);

      assertNotNull(vector, "Vector should not be null");
      assertEquals(SimdLane.I32X4, vector.getLane(), "Lane should be I32X4");
    }

    @Test
    @DisplayName("should reject null lane")
    void shouldRejectNullLane() {
      final byte[] data = new byte[16];

      final NullPointerException exception =
          assertThrows(
              NullPointerException.class,
              () -> new SimdVector(null, data),
              "Should throw NullPointerException for null lane");

      assertTrue(exception.getMessage().contains("lane"), "Exception message should mention lane");
    }

    @Test
    @DisplayName("should reject null data")
    void shouldRejectNullData() {
      final NullPointerException exception =
          assertThrows(
              NullPointerException.class,
              () -> new SimdVector(SimdLane.I32X4, null),
              "Should throw NullPointerException for null data");

      assertTrue(exception.getMessage().contains("data"), "Exception message should mention data");
    }

    @Test
    @DisplayName("should reject data with wrong size")
    void shouldRejectDataWithWrongSize() {
      final byte[] tooSmall = new byte[8];
      final byte[] tooLarge = new byte[32];

      assertThrows(
          IllegalArgumentException.class,
          () -> new SimdVector(SimdLane.I32X4, tooSmall),
          "Should throw IllegalArgumentException for data smaller than 16 bytes");

      assertThrows(
          IllegalArgumentException.class,
          () -> new SimdVector(SimdLane.I32X4, tooLarge),
          "Should throw IllegalArgumentException for data larger than 16 bytes");
    }

    @Test
    @DisplayName("should make defensive copy of data")
    void shouldMakeDefensiveCopyOfData() {
      final byte[] originalData = new byte[16];
      originalData[0] = 42;

      final SimdVector vector = new SimdVector(SimdLane.I32X4, originalData);

      // Modify original
      originalData[0] = 99;

      // Vector should still have original value
      assertEquals(42, vector.getData()[0], "Vector should have made defensive copy");
    }
  }

  @Nested
  @DisplayName("GetData Tests")
  class GetDataTests {

    @Test
    @DisplayName("getData should return copy of data")
    void getDataShouldReturnCopyOfData() {
      final byte[] originalData = new byte[16];
      originalData[0] = 42;

      final SimdVector vector = new SimdVector(SimdLane.I32X4, originalData);

      final byte[] data1 = vector.getData();
      final byte[] data2 = vector.getData();

      assertNotSame(data1, data2, "getData should return different array instances");
      assertArrayEquals(data1, data2, "Data content should be the same");
    }

    @Test
    @DisplayName("getData should return 16 bytes")
    void getDataShouldReturn16Bytes() {
      final SimdVector vector = new SimdVector(SimdLane.I32X4, new byte[16]);
      assertEquals(16, vector.getData().length, "getData should return 16 bytes");
    }
  }

  @Nested
  @DisplayName("SplatI32 Tests")
  class SplatI32Tests {

    @Test
    @DisplayName("should create I8X16 vector with splatted value")
    void shouldCreateI8x16VectorWithSplattedValue() {
      final SimdVector vector = SimdVector.splatI32(SimdLane.I8X16, 42);

      assertEquals(SimdLane.I8X16, vector.getLane(), "Lane should be I8X16");
      final byte[] data = vector.getData();
      for (int i = 0; i < 16; i++) {
        assertEquals(42, data[i], "All bytes should be 42");
      }
    }

    @Test
    @DisplayName("should create I16X8 vector with splatted value")
    void shouldCreateI16x8VectorWithSplattedValue() {
      final SimdVector vector = SimdVector.splatI32(SimdLane.I16X8, 0x1234);

      assertEquals(SimdLane.I16X8, vector.getLane(), "Lane should be I16X8");
      final byte[] data = vector.getData();
      // Little-endian encoding
      for (int i = 0; i < 16; i += 2) {
        assertEquals(0x34, data[i] & 0xFF, "Low byte should be 0x34");
        assertEquals(0x12, data[i + 1] & 0xFF, "High byte should be 0x12");
      }
    }

    @Test
    @DisplayName("should create I32X4 vector with splatted value")
    void shouldCreateI32x4VectorWithSplattedValue() {
      final SimdVector vector = SimdVector.splatI32(SimdLane.I32X4, 0x12345678);

      assertEquals(SimdLane.I32X4, vector.getLane(), "Lane should be I32X4");
      final byte[] data = vector.getData();
      // Little-endian encoding
      for (int i = 0; i < 16; i += 4) {
        assertEquals(0x78, data[i] & 0xFF, "Byte 0 should be 0x78");
        assertEquals(0x56, data[i + 1] & 0xFF, "Byte 1 should be 0x56");
        assertEquals(0x34, data[i + 2] & 0xFF, "Byte 2 should be 0x34");
        assertEquals(0x12, data[i + 3] & 0xFF, "Byte 3 should be 0x12");
      }
    }

    @Test
    @DisplayName("should reject float lane type")
    void shouldRejectFloatLaneType() {
      assertThrows(
          IllegalArgumentException.class,
          () -> SimdVector.splatI32(SimdLane.F32X4, 42),
          "Should throw IllegalArgumentException for float lane type");

      assertThrows(
          IllegalArgumentException.class,
          () -> SimdVector.splatI32(SimdLane.F64X2, 42),
          "Should throw IllegalArgumentException for float lane type");
    }
  }

  @Nested
  @DisplayName("SplatF32 Tests")
  class SplatF32Tests {

    @Test
    @DisplayName("should create F32X4 vector with splatted value")
    void shouldCreateF32x4VectorWithSplattedValue() {
      final SimdVector vector = SimdVector.splatF32(SimdLane.F32X4, 1.5f);

      assertEquals(SimdLane.F32X4, vector.getLane(), "Lane should be F32X4");
      assertNotNull(vector.getData(), "Data should not be null");
    }

    @Test
    @DisplayName("should reject non-F32X4 lane type")
    void shouldRejectNonF32x4LaneType() {
      assertThrows(
          IllegalArgumentException.class,
          () -> SimdVector.splatF32(SimdLane.I32X4, 1.5f),
          "Should throw IllegalArgumentException for non-F32X4 lane type");

      assertThrows(
          IllegalArgumentException.class,
          () -> SimdVector.splatF32(SimdLane.F64X2, 1.5f),
          "Should throw IllegalArgumentException for non-F32X4 lane type");
    }
  }

  @Nested
  @DisplayName("SplatF64 Tests")
  class SplatF64Tests {

    @Test
    @DisplayName("should create F64X2 vector with splatted value")
    void shouldCreateF64x2VectorWithSplattedValue() {
      final SimdVector vector = SimdVector.splatF64(SimdLane.F64X2, 3.14159);

      assertEquals(SimdLane.F64X2, vector.getLane(), "Lane should be F64X2");
      assertNotNull(vector.getData(), "Data should not be null");
    }

    @Test
    @DisplayName("should reject non-F64X2 lane type")
    void shouldRejectNonF64x2LaneType() {
      assertThrows(
          IllegalArgumentException.class,
          () -> SimdVector.splatF64(SimdLane.F32X4, 3.14159),
          "Should throw IllegalArgumentException for non-F64X2 lane type");

      assertThrows(
          IllegalArgumentException.class,
          () -> SimdVector.splatF64(SimdLane.I64X2, 3.14159),
          "Should throw IllegalArgumentException for non-F64X2 lane type");
    }
  }

  @Nested
  @DisplayName("Equals Tests")
  class EqualsTests {

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      final SimdVector vector = new SimdVector(SimdLane.I32X4, new byte[16]);
      assertEquals(vector, vector, "Vector should be equal to itself");
    }

    @Test
    @DisplayName("should be equal to vector with same lane and data")
    void shouldBeEqualToVectorWithSameLaneAndData() {
      final byte[] data = new byte[16];
      data[0] = 42;

      final SimdVector vector1 = new SimdVector(SimdLane.I32X4, data);
      final SimdVector vector2 = new SimdVector(SimdLane.I32X4, data);

      assertEquals(vector1, vector2, "Vectors with same lane and data should be equal");
    }

    @Test
    @DisplayName("should not be equal to vector with different lane")
    void shouldNotBeEqualToVectorWithDifferentLane() {
      final byte[] data = new byte[16];

      final SimdVector vector1 = new SimdVector(SimdLane.I32X4, data);
      final SimdVector vector2 = new SimdVector(SimdLane.I64X2, data);

      assertNotEquals(vector1, vector2, "Vectors with different lanes should not be equal");
    }

    @Test
    @DisplayName("should not be equal to vector with different data")
    void shouldNotBeEqualToVectorWithDifferentData() {
      final byte[] data1 = new byte[16];
      final byte[] data2 = new byte[16];
      data2[0] = 42;

      final SimdVector vector1 = new SimdVector(SimdLane.I32X4, data1);
      final SimdVector vector2 = new SimdVector(SimdLane.I32X4, data2);

      assertNotEquals(vector1, vector2, "Vectors with different data should not be equal");
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      final SimdVector vector = new SimdVector(SimdLane.I32X4, new byte[16]);
      assertFalse(vector.equals(null), "Vector should not be equal to null");
    }

    @Test
    @DisplayName("should not be equal to different type")
    void shouldNotBeEqualToDifferentType() {
      final SimdVector vector = new SimdVector(SimdLane.I32X4, new byte[16]);
      assertFalse(vector.equals("not a vector"), "Vector should not be equal to String");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("equal vectors should have same hash code")
    void equalVectorsShouldHaveSameHashCode() {
      final byte[] data = new byte[16];
      data[0] = 42;

      final SimdVector vector1 = new SimdVector(SimdLane.I32X4, data);
      final SimdVector vector2 = new SimdVector(SimdLane.I32X4, data);

      assertEquals(
          vector1.hashCode(), vector2.hashCode(), "Equal vectors should have same hash code");
    }

    @Test
    @DisplayName("hashCode should be consistent")
    void hashCodeShouldBeConsistent() {
      final SimdVector vector = new SimdVector(SimdLane.I32X4, new byte[16]);

      final int hashCode1 = vector.hashCode();
      final int hashCode2 = vector.hashCode();

      assertEquals(hashCode1, hashCode2, "hashCode should be consistent across calls");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain lane type")
    void toStringShouldContainLaneType() {
      final SimdVector vector = new SimdVector(SimdLane.I32X4, new byte[16]);
      final String str = vector.toString();

      assertTrue(
          str.contains("I32X4") || str.contains("i32x4"), "toString should contain lane type");
    }

    @Test
    @DisplayName("toString should contain SimdVector")
    void toStringShouldContainSimdVector() {
      final SimdVector vector = new SimdVector(SimdLane.I32X4, new byte[16]);
      final String str = vector.toString();

      assertTrue(str.contains("SimdVector"), "toString should contain SimdVector");
    }
  }
}
