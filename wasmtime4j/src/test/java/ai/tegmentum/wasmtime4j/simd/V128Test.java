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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link V128} - 128-bit SIMD vector value.
 *
 * <p>Validates constructors, factory methods, lane accessors, defensive copies, and
 * equals/hashCode.
 */
@DisplayName("V128 Tests")
class V128Test {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create V128 from valid 16-byte array")
    void shouldCreateFromValidByteArray() {
      final byte[] data = new byte[16];
      data[0] = 1;
      data[15] = (byte) 0xFF;
      final V128 v = new V128(data);
      assertNotNull(v, "V128 should be created from valid byte array");
    }

    @Test
    @DisplayName("should reject null data")
    void shouldRejectNullData() {
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> new V128(null),
              "Constructor should reject null data");
      assertNotNull(ex.getMessage(), "Exception should have a message");
    }

    @Test
    @DisplayName("should reject wrong size byte array")
    void shouldRejectWrongSizeByteArray() {
      final byte[] tooSmall = new byte[8];
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> new V128(tooSmall),
              "Constructor should reject non-16-byte arrays");
      assertNotNull(ex.getMessage(), "Exception should have a message");
    }

    @Test
    @DisplayName("should make defensive copy of input data")
    void shouldMakeDefensiveCopyOfInputData() {
      final byte[] data = new byte[16];
      data[0] = 42;
      final V128 v = new V128(data);
      data[0] = 0;
      final byte[] retrieved = v.getBytes();
      assertEquals(42, retrieved[0], "V128 should store a defensive copy, unaffected by mutations");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests - fromInts")
  class FromIntsTests {

    @Test
    @DisplayName("fromInts with four values should create correct vector")
    void fromIntsWithFourValuesShouldCreateCorrectVector() {
      final V128 v = V128.fromInts(1, 2, 3, 4);
      final int[] ints = v.getAsInts();
      assertArrayEquals(
          new int[] {1, 2, 3, 4}, ints, "fromInts should produce correct integer lanes");
    }

    @Test
    @DisplayName("fromInts with array should create correct vector")
    void fromIntsWithArrayShouldCreateCorrectVector() {
      final int[] values = {10, 20, 30, 40};
      final V128 v = V128.fromInts(values);
      final int[] ints = v.getAsInts();
      assertArrayEquals(values, ints, "fromInts(array) should produce correct integer lanes");
    }

    @Test
    @DisplayName("fromInts with array should reject null")
    void fromIntsWithArrayShouldRejectNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> V128.fromInts((int[]) null),
          "fromInts should reject null array");
    }

    @Test
    @DisplayName("fromInts with array should reject wrong length")
    void fromIntsWithArrayShouldRejectWrongLength() {
      final int[] wrong = {1, 2, 3};
      assertThrows(
          IllegalArgumentException.class,
          () -> V128.fromInts(wrong),
          "fromInts should reject arrays with length != 4");
    }

    @Test
    @DisplayName("fromInts should handle negative values")
    void fromIntsShouldHandleNegativeValues() {
      final V128 v = V128.fromInts(-1, Integer.MIN_VALUE, Integer.MAX_VALUE, 0);
      final int[] ints = v.getAsInts();
      assertEquals(-1, ints[0], "Lane 0 should be -1");
      assertEquals(Integer.MIN_VALUE, ints[1], "Lane 1 should be Integer.MIN_VALUE");
      assertEquals(Integer.MAX_VALUE, ints[2], "Lane 2 should be Integer.MAX_VALUE");
      assertEquals(0, ints[3], "Lane 3 should be 0");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests - fromFloats")
  class FromFloatsTests {

    @Test
    @DisplayName("fromFloats with four values should create correct vector")
    void fromFloatsWithFourValuesShouldCreateCorrectVector() {
      final V128 v = V128.fromFloats(1.0f, 2.5f, 3.14f, 4.0f);
      final float[] floats = v.getAsFloats();
      assertEquals(1.0f, floats[0], 0.001f, "Lane 0 should be 1.0");
      assertEquals(2.5f, floats[1], 0.001f, "Lane 1 should be 2.5");
      assertEquals(3.14f, floats[2], 0.001f, "Lane 2 should be 3.14");
      assertEquals(4.0f, floats[3], 0.001f, "Lane 3 should be 4.0");
    }

    @Test
    @DisplayName("fromFloats with array should create correct vector")
    void fromFloatsWithArrayShouldCreateCorrectVector() {
      final float[] values = {1.0f, 2.0f, 3.0f, 4.0f};
      final V128 v = V128.fromFloats(values);
      final float[] floats = v.getAsFloats();
      assertArrayEquals(values, floats, 0.001f, "fromFloats(array) should produce correct lanes");
    }

    @Test
    @DisplayName("fromFloats with array should reject null")
    void fromFloatsWithArrayShouldRejectNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> V128.fromFloats((float[]) null),
          "fromFloats should reject null array");
    }

    @Test
    @DisplayName("fromFloats with array should reject wrong length")
    void fromFloatsWithArrayShouldRejectWrongLength() {
      final float[] wrong = {1.0f, 2.0f};
      assertThrows(
          IllegalArgumentException.class,
          () -> V128.fromFloats(wrong),
          "fromFloats should reject arrays with length != 4");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests - fromLongs")
  class FromLongsTests {

    @Test
    @DisplayName("fromLongs should create correct vector")
    void fromLongsShouldCreateCorrectVector() {
      final V128 v = V128.fromLongs(100L, 200L);
      final long[] longs = v.getAsLongs();
      assertArrayEquals(
          new long[] {100L, 200L}, longs, "fromLongs should produce correct long lanes");
    }

    @Test
    @DisplayName("fromLongs should handle extreme values")
    void fromLongsShouldHandleExtremeValues() {
      final V128 v = V128.fromLongs(Long.MIN_VALUE, Long.MAX_VALUE);
      final long[] longs = v.getAsLongs();
      assertEquals(Long.MIN_VALUE, longs[0], "Lane 0 should be Long.MIN_VALUE");
      assertEquals(Long.MAX_VALUE, longs[1], "Lane 1 should be Long.MAX_VALUE");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests - fromDoubles")
  class FromDoublesTests {

    @Test
    @DisplayName("fromDoubles should create correct vector")
    void fromDoublesShouldCreateCorrectVector() {
      final V128 v = V128.fromDoubles(3.14, 2.718);
      final double[] doubles = v.getAsDoubles();
      assertEquals(3.14, doubles[0], 0.001, "Lane 0 should be 3.14");
      assertEquals(2.718, doubles[1], 0.001, "Lane 1 should be 2.718");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests - fromBytes")
  class FromBytesTests {

    @Test
    @DisplayName("fromBytes should create correct vector")
    void fromBytesShouldCreateCorrectVector() {
      final byte[] data = new byte[16];
      for (int i = 0; i < 16; i++) {
        data[i] = (byte) i;
      }
      final V128 v = V128.fromBytes(data);
      final byte[] result = v.getBytes();
      assertArrayEquals(data, result, "fromBytes should preserve exact byte content");
    }

    @Test
    @DisplayName("fromBytes should reject null")
    void fromBytesShouldRejectNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> V128.fromBytes(null),
          "fromBytes should reject null");
    }

    @Test
    @DisplayName("fromBytes should reject wrong size")
    void fromBytesShouldRejectWrongSize() {
      assertThrows(
          IllegalArgumentException.class,
          () -> V128.fromBytes(new byte[10]),
          "fromBytes should reject non-16-byte arrays");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests - zero and splat")
  class ZeroAndSplatTests {

    @Test
    @DisplayName("zero should create all-zeros vector")
    void zeroShouldCreateAllZerosVector() {
      final V128 v = V128.zero();
      final int[] ints = v.getAsInts();
      assertArrayEquals(new int[] {0, 0, 0, 0}, ints, "zero() should produce all-zero lanes");
    }

    @Test
    @DisplayName("splatInt should fill all lanes with same value")
    void splatIntShouldFillAllLanesWithSameValue() {
      final V128 v = V128.splatInt(42);
      final int[] ints = v.getAsInts();
      assertArrayEquals(
          new int[] {42, 42, 42, 42}, ints, "splatInt should fill all lanes with the given value");
    }

    @Test
    @DisplayName("splatFloat should fill all lanes with same value")
    void splatFloatShouldFillAllLanesWithSameValue() {
      final V128 v = V128.splatFloat(1.5f);
      final float[] floats = v.getAsFloats();
      for (int i = 0; i < 4; i++) {
        assertEquals(1.5f, floats[i], 0.001f, "Lane " + i + " should be 1.5");
      }
    }
  }

  @Nested
  @DisplayName("Lane Accessor Tests")
  class LaneAccessorTests {

    @Test
    @DisplayName("getIntLane should return correct lane values")
    void getIntLaneShouldReturnCorrectValues() {
      final V128 v = V128.fromInts(10, 20, 30, 40);
      assertEquals(10, v.getIntLane(0), "Lane 0 should be 10");
      assertEquals(20, v.getIntLane(1), "Lane 1 should be 20");
      assertEquals(30, v.getIntLane(2), "Lane 2 should be 30");
      assertEquals(40, v.getIntLane(3), "Lane 3 should be 40");
    }

    @Test
    @DisplayName("getIntLane should reject negative index")
    void getIntLaneShouldRejectNegativeIndex() {
      final V128 v = V128.zero();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> v.getIntLane(-1),
          "getIntLane should reject negative index");
    }

    @Test
    @DisplayName("getIntLane should reject index above 3")
    void getIntLaneShouldRejectIndexAbove3() {
      final V128 v = V128.zero();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> v.getIntLane(4),
          "getIntLane should reject index > 3");
    }

    @Test
    @DisplayName("getFloatLane should return correct lane values")
    void getFloatLaneShouldReturnCorrectValues() {
      final V128 v = V128.fromFloats(1.0f, 2.0f, 3.0f, 4.0f);
      assertEquals(1.0f, v.getFloatLane(0), 0.001f, "Lane 0 should be 1.0");
      assertEquals(2.0f, v.getFloatLane(1), 0.001f, "Lane 1 should be 2.0");
      assertEquals(3.0f, v.getFloatLane(2), 0.001f, "Lane 2 should be 3.0");
      assertEquals(4.0f, v.getFloatLane(3), 0.001f, "Lane 3 should be 4.0");
    }

    @Test
    @DisplayName("getFloatLane should reject negative index")
    void getFloatLaneShouldRejectNegativeIndex() {
      final V128 v = V128.zero();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> v.getFloatLane(-1),
          "getFloatLane should reject negative index");
    }

    @Test
    @DisplayName("getFloatLane should reject index above 3")
    void getFloatLaneShouldRejectIndexAbove3() {
      final V128 v = V128.zero();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> v.getFloatLane(4),
          "getFloatLane should reject index > 3");
    }
  }

  @Nested
  @DisplayName("WithLane Tests")
  class WithLaneTests {

    @Test
    @DisplayName("withIntLane should create new vector with replaced lane")
    void withIntLaneShouldCreateNewVector() {
      final V128 original = V128.fromInts(1, 2, 3, 4);
      final V128 modified = original.withIntLane(1, 99);
      assertEquals(99, modified.getIntLane(1), "Modified lane should have new value");
      assertEquals(2, original.getIntLane(1), "Original vector should be unchanged");
      assertNotSame(original, modified, "withIntLane should return a new instance");
    }

    @Test
    @DisplayName("withIntLane should reject invalid index")
    void withIntLaneShouldRejectInvalidIndex() {
      final V128 v = V128.zero();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> v.withIntLane(4, 0),
          "withIntLane should reject index > 3");
    }

    @Test
    @DisplayName("withFloatLane should create new vector with replaced lane")
    void withFloatLaneShouldCreateNewVector() {
      final V128 original = V128.fromFloats(1.0f, 2.0f, 3.0f, 4.0f);
      final V128 modified = original.withFloatLane(2, 99.9f);
      assertEquals(99.9f, modified.getFloatLane(2), 0.001f, "Modified lane should have new value");
      assertEquals(3.0f, original.getFloatLane(2), 0.001f, "Original vector should be unchanged");
    }

    @Test
    @DisplayName("withFloatLane should reject invalid index")
    void withFloatLaneShouldRejectInvalidIndex() {
      final V128 v = V128.zero();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> v.withFloatLane(-1, 0.0f),
          "withFloatLane should reject negative index");
    }
  }

  @Nested
  @DisplayName("Byte Operations Tests")
  class ByteOperationsTests {

    @Test
    @DisplayName("getBytes should return defensive copy")
    void getBytesShouldReturnDefensiveCopy() {
      final V128 v = V128.fromInts(1, 0, 0, 0);
      final byte[] bytes1 = v.getBytes();
      final byte[] bytes2 = v.getBytes();
      assertNotSame(bytes1, bytes2, "getBytes should return different array instances");
      assertArrayEquals(bytes1, bytes2, "getBytes should return identical content");
    }

    @Test
    @DisplayName("getBytes result mutation should not affect original")
    void getBytesMutationShouldNotAffectOriginal() {
      final V128 v = V128.fromInts(1, 2, 3, 4);
      final byte[] bytes = v.getBytes();
      final byte originalByte = bytes[0];
      bytes[0] = (byte) 0xFF;
      final byte[] bytesAgain = v.getBytes();
      assertEquals(
          originalByte, bytesAgain[0], "Mutating getBytes() result should not affect the V128");
    }

    @Test
    @DisplayName("SIZE_BYTES constant should be 16")
    void sizeBytesShouldBe16() {
      assertEquals(16, V128.SIZE_BYTES, "V128 should always be 16 bytes");
    }

    @Test
    @DisplayName("getBytes should return 16 bytes")
    void getBytesShouldReturn16Bytes() {
      final V128 v = V128.zero();
      assertEquals(16, v.getBytes().length, "getBytes() should always return 16 bytes");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equals should return true for identical vectors")
    void equalsShouldReturnTrueForIdenticalVectors() {
      final V128 a = V128.fromInts(1, 2, 3, 4);
      final V128 b = V128.fromInts(1, 2, 3, 4);
      assertEquals(a, b, "Vectors with same content should be equal");
    }

    @Test
    @DisplayName("equals should return false for different vectors")
    void equalsShouldReturnFalseForDifferentVectors() {
      final V128 a = V128.fromInts(1, 2, 3, 4);
      final V128 b = V128.fromInts(4, 3, 2, 1);
      assertNotEquals(a, b, "Vectors with different content should not be equal");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final V128 v = V128.zero();
      assertNotEquals(null, v, "V128 should not equal null");
    }

    @Test
    @DisplayName("hashCode should be consistent for equal vectors")
    void hashCodeShouldBeConsistentForEqualVectors() {
      final V128 a = V128.fromInts(5, 6, 7, 8);
      final V128 b = V128.fromInts(5, 6, 7, 8);
      assertEquals(a.hashCode(), b.hashCode(), "Equal vectors should have same hashCode");
    }

    @Test
    @DisplayName("equals should be reflexive")
    void equalsShouldBeReflexive() {
      final V128 v = V128.fromLongs(42L, 84L);
      assertEquals(v, v, "A V128 should equal itself");
    }
  }

  @Nested
  @DisplayName("toString and toHexString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain lane values")
    void toStringShouldContainLaneValues() {
      final V128 v = V128.fromInts(1, 2, 3, 4);
      final String str = v.toString();
      assertNotNull(str, "toString should not return null");
      assertEquals(
          "V128[i32x4: 1, 2, 3, 4]", str, "toString should format as V128[i32x4: a, b, c, d]");
    }

    @Test
    @DisplayName("toHexString should return 32 character hex string")
    void toHexStringShouldReturn32CharHexString() {
      final V128 v = V128.zero();
      final String hex = v.toHexString();
      assertEquals(32, hex.length(), "Hex string should be 32 characters (2 per byte)");
      assertEquals("00000000000000000000000000000000", hex, "Zero vector hex should be all zeros");
    }

    @Test
    @DisplayName("toHexString should reflect byte content")
    void toHexStringShouldReflectByteContent() {
      final byte[] data = new byte[16];
      data[0] = (byte) 0xAB;
      data[1] = (byte) 0xCD;
      final V128 v = new V128(data);
      final String hex = v.toHexString();
      assertEquals("ab", hex.substring(0, 2), "First byte hex should be 'ab'");
      assertEquals("cd", hex.substring(2, 4), "Second byte hex should be 'cd'");
    }
  }
}
