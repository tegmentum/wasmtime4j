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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for V128 SIMD value class with native library loaded.
 *
 * <p>These tests verify V128 factory methods, data extraction, lane manipulation, and conversions.
 *
 * @since 1.0.0
 */
@DisplayName("V128 SIMD Integration Tests")
class SimdV128IntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(SimdV128IntegrationTest.class.getName());

  @BeforeAll
  static void loadNativeLibrary() {
    LOGGER.info("Loading native library for V128 integration tests");
    try {
      NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
      assertTrue(loader.isLoaded(), "Native library should be loaded");
      LOGGER.info("Native library loaded successfully");
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library: " + e.getMessage());
      throw new RuntimeException("Native library required for integration tests", e);
    }
  }

  @Nested
  @DisplayName("V128 Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should create V128 from four integers")
    void shouldCreateV128FromFourIntegers() {
      LOGGER.info("Testing V128.fromInts with four integers");

      final V128 vector = V128.fromInts(1, 2, 3, 4);

      assertNotNull(vector, "Vector should not be null");
      final int[] values = vector.getAsInts();
      assertArrayEquals(new int[] {1, 2, 3, 4}, values, "Values should match input");

      LOGGER.info("V128.fromInts created: " + vector);
    }

    @Test
    @DisplayName("should create V128 from integer array")
    void shouldCreateV128FromIntegerArray() {
      LOGGER.info("Testing V128.fromInts with array");

      final int[] input = {10, 20, 30, 40};
      final V128 vector = V128.fromInts(input);

      assertNotNull(vector, "Vector should not be null");
      final int[] values = vector.getAsInts();
      assertArrayEquals(input, values, "Values should match input array");

      LOGGER.info("V128.fromInts(array) created: " + vector);
    }

    @Test
    @DisplayName("should create V128 from four floats")
    void shouldCreateV128FromFourFloats() {
      LOGGER.info("Testing V128.fromFloats with four floats");

      final V128 vector = V128.fromFloats(1.1f, 2.2f, 3.3f, 4.4f);

      assertNotNull(vector, "Vector should not be null");
      final float[] values = vector.getAsFloats();
      assertEquals(1.1f, values[0], 0.001f, "Lane 0 should match");
      assertEquals(2.2f, values[1], 0.001f, "Lane 1 should match");
      assertEquals(3.3f, values[2], 0.001f, "Lane 2 should match");
      assertEquals(4.4f, values[3], 0.001f, "Lane 3 should match");

      LOGGER.info("V128.fromFloats created with values");
    }

    @Test
    @DisplayName("should create V128 from float array")
    void shouldCreateV128FromFloatArray() {
      LOGGER.info("Testing V128.fromFloats with array");

      final float[] input = {1.5f, 2.5f, 3.5f, 4.5f};
      final V128 vector = V128.fromFloats(input);

      assertNotNull(vector, "Vector should not be null");
      final float[] values = vector.getAsFloats();
      for (int i = 0; i < 4; i++) {
        assertEquals(input[i], values[i], 0.001f, "Lane " + i + " should match");
      }

      LOGGER.info("V128.fromFloats(array) created successfully");
    }

    @Test
    @DisplayName("should create V128 from two longs")
    void shouldCreateV128FromTwoLongs() {
      LOGGER.info("Testing V128.fromLongs with two longs");

      final V128 vector = V128.fromLongs(0x123456789ABCDEF0L, 0xFEDCBA9876543210L);

      assertNotNull(vector, "Vector should not be null");
      final long[] values = vector.getAsLongs();
      assertEquals(0x123456789ABCDEF0L, values[0], "Lane 0 should match");
      assertEquals(0xFEDCBA9876543210L, values[1], "Lane 1 should match");

      LOGGER.info("V128.fromLongs created: lanes = [" + values[0] + ", " + values[1] + "]");
    }

    @Test
    @DisplayName("should create V128 from two doubles")
    void shouldCreateV128FromTwoDoubles() {
      LOGGER.info("Testing V128.fromDoubles with two doubles");

      final V128 vector = V128.fromDoubles(3.14159, 2.71828);

      assertNotNull(vector, "Vector should not be null");
      final double[] values = vector.getAsDoubles();
      assertEquals(3.14159, values[0], 0.00001, "Lane 0 should match");
      assertEquals(2.71828, values[1], 0.00001, "Lane 1 should match");

      LOGGER.info("V128.fromDoubles created with pi and e");
    }

    @Test
    @DisplayName("should create zero V128")
    void shouldCreateZeroV128() {
      LOGGER.info("Testing V128.zero()");

      final V128 vector = V128.zero();

      assertNotNull(vector, "Vector should not be null");
      final int[] values = vector.getAsInts();
      assertArrayEquals(new int[] {0, 0, 0, 0}, values, "All lanes should be zero");

      LOGGER.info("V128.zero() created: " + vector);
    }

    @Test
    @DisplayName("should create splatted integer V128")
    void shouldCreateSplattedIntegerV128() {
      LOGGER.info("Testing V128.splatInt()");

      final V128 vector = V128.splatInt(42);

      assertNotNull(vector, "Vector should not be null");
      final int[] values = vector.getAsInts();
      assertArrayEquals(new int[] {42, 42, 42, 42}, values, "All lanes should be 42");

      LOGGER.info("V128.splatInt(42) created: " + vector);
    }

    @Test
    @DisplayName("should create splatted float V128")
    void shouldCreateSplattedFloatV128() {
      LOGGER.info("Testing V128.splatFloat()");

      final V128 vector = V128.splatFloat(3.14f);

      assertNotNull(vector, "Vector should not be null");
      final float[] values = vector.getAsFloats();
      for (int i = 0; i < 4; i++) {
        assertEquals(3.14f, values[i], 0.001f, "Lane " + i + " should be 3.14");
      }

      LOGGER.info("V128.splatFloat(3.14) created successfully");
    }
  }

  @Nested
  @DisplayName("V128 Lane Access Tests")
  class LaneAccessTests {

    @Test
    @DisplayName("should get individual integer lanes")
    void shouldGetIndividualIntegerLanes() {
      LOGGER.info("Testing V128.getIntLane()");

      final V128 vector = V128.fromInts(100, 200, 300, 400);

      assertEquals(100, vector.getIntLane(0), "Lane 0 should be 100");
      assertEquals(200, vector.getIntLane(1), "Lane 1 should be 200");
      assertEquals(300, vector.getIntLane(2), "Lane 2 should be 300");
      assertEquals(400, vector.getIntLane(3), "Lane 3 should be 400");

      LOGGER.info("All integer lanes extracted correctly");
    }

    @Test
    @DisplayName("should get individual float lanes")
    void shouldGetIndividualFloatLanes() {
      LOGGER.info("Testing V128.getFloatLane()");

      final V128 vector = V128.fromFloats(1.0f, 2.0f, 3.0f, 4.0f);

      assertEquals(1.0f, vector.getFloatLane(0), 0.001f, "Lane 0 should be 1.0");
      assertEquals(2.0f, vector.getFloatLane(1), 0.001f, "Lane 1 should be 2.0");
      assertEquals(3.0f, vector.getFloatLane(2), 0.001f, "Lane 2 should be 3.0");
      assertEquals(4.0f, vector.getFloatLane(3), 0.001f, "Lane 3 should be 4.0");

      LOGGER.info("All float lanes extracted correctly");
    }

    @Test
    @DisplayName("should throw on out of bounds lane index")
    void shouldThrowOnOutOfBoundsLaneIndex() {
      LOGGER.info("Testing out of bounds lane index");

      final V128 vector = V128.fromInts(1, 2, 3, 4);

      assertThrows(
          IndexOutOfBoundsException.class,
          () -> vector.getIntLane(-1),
          "Should throw for negative index");

      assertThrows(
          IndexOutOfBoundsException.class,
          () -> vector.getIntLane(4),
          "Should throw for index >= 4");

      LOGGER.info("Out of bounds exceptions thrown correctly");
    }
  }

  @Nested
  @DisplayName("V128 Lane Replacement Tests")
  class LaneReplacementTests {

    @Test
    @DisplayName("should replace integer lane")
    void shouldReplaceIntegerLane() {
      LOGGER.info("Testing V128.withIntLane()");

      final V128 original = V128.fromInts(1, 2, 3, 4);
      final V128 modified = original.withIntLane(2, 999);

      // Original should be unchanged
      assertEquals(3, original.getIntLane(2), "Original lane 2 should still be 3");

      // Modified should have new value
      assertEquals(999, modified.getIntLane(2), "Modified lane 2 should be 999");
      assertEquals(1, modified.getIntLane(0), "Other lanes should be unchanged");
      assertEquals(2, modified.getIntLane(1), "Other lanes should be unchanged");
      assertEquals(4, modified.getIntLane(3), "Other lanes should be unchanged");

      LOGGER.info("Integer lane replacement worked correctly");
    }

    @Test
    @DisplayName("should replace float lane")
    void shouldReplaceFloatLane() {
      LOGGER.info("Testing V128.withFloatLane()");

      final V128 original = V128.fromFloats(1.0f, 2.0f, 3.0f, 4.0f);
      final V128 modified = original.withFloatLane(1, 99.9f);

      // Original should be unchanged
      assertEquals(2.0f, original.getFloatLane(1), 0.001f, "Original lane 1 should still be 2.0");

      // Modified should have new value
      assertEquals(99.9f, modified.getFloatLane(1), 0.001f, "Modified lane 1 should be 99.9");

      LOGGER.info("Float lane replacement worked correctly");
    }
  }

  @Nested
  @DisplayName("V128 Raw Bytes Tests")
  class RawBytesTests {

    @Test
    @DisplayName("should create V128 from raw bytes")
    void shouldCreateV128FromRawBytes() {
      LOGGER.info("Testing V128.fromBytes()");

      final byte[] input = new byte[16];
      for (int i = 0; i < 16; i++) {
        input[i] = (byte) i;
      }

      final V128 vector = V128.fromBytes(input);
      final byte[] output = vector.getBytes();

      assertArrayEquals(input, output, "Bytes should match input");

      LOGGER.info("V128.fromBytes() created and verified");
    }

    @Test
    @DisplayName("should reject wrong size byte array")
    void shouldRejectWrongSizeByteArray() {
      LOGGER.info("Testing byte array size validation");

      assertThrows(
          IllegalArgumentException.class,
          () -> V128.fromBytes(new byte[15]),
          "Should reject 15 bytes");

      assertThrows(
          IllegalArgumentException.class,
          () -> V128.fromBytes(new byte[17]),
          "Should reject 17 bytes");

      LOGGER.info("Size validation working correctly");
    }

    @Test
    @DisplayName("should return defensive copy of bytes")
    void shouldReturnDefensiveCopyOfBytes() {
      LOGGER.info("Testing defensive copy of bytes");

      final V128 vector = V128.fromInts(1, 2, 3, 4);
      final byte[] bytes1 = vector.getBytes();
      final byte[] bytes2 = vector.getBytes();

      // Modify first copy
      bytes1[0] = (byte) 0xFF;

      // Second copy should be unaffected
      assertNotEquals(bytes1[0], bytes2[0], "Second copy should not be affected by modification");

      LOGGER.info("Defensive copy verified");
    }
  }

  @Nested
  @DisplayName("V128 Conversion Tests")
  class ConversionTests {

    @Test
    @DisplayName("should convert to SimdVector")
    void shouldConvertToSimdVector() {
      LOGGER.info("Testing V128.toSimdVector()");

      final V128 v128 = V128.fromInts(1, 2, 3, 4);
      final SimdVector simdVector = v128.toSimdVector(SimdLane.I32X4);

      assertNotNull(simdVector, "SimdVector should not be null");
      assertEquals(SimdLane.I32X4, simdVector.getLane(), "Lane type should be I32X4");
      assertArrayEquals(v128.getBytes(), simdVector.getData(), "Data should match");

      LOGGER.info("V128 to SimdVector conversion successful");
    }

    @Test
    @DisplayName("should convert from SimdVector")
    void shouldConvertFromSimdVector() {
      LOGGER.info("Testing V128.fromSimdVector()");

      final SimdVector simdVector = SimdVector.splatI32(SimdLane.I32X4, 42);
      final V128 v128 = V128.fromSimdVector(simdVector);

      assertNotNull(v128, "V128 should not be null");
      final int[] values = v128.getAsInts();
      for (int i = 0; i < 4; i++) {
        assertEquals(42, values[i], "Lane " + i + " should be 42");
      }

      LOGGER.info("SimdVector to V128 conversion successful");
    }

    @Test
    @DisplayName("should produce hex string")
    void shouldProduceHexString() {
      LOGGER.info("Testing V128.toHexString()");

      final V128 vector = V128.zero();
      final String hex = vector.toHexString();

      assertNotNull(hex, "Hex string should not be null");
      assertEquals(32, hex.length(), "Hex string should be 32 characters");
      assertEquals("00000000000000000000000000000000", hex, "Zero vector should be all zeros");

      LOGGER.info("Hex string: " + hex);
    }
  }

  @Nested
  @DisplayName("V128 Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("should be equal if same values")
    void shouldBeEqualIfSameValues() {
      LOGGER.info("Testing V128 equality");

      final V128 a = V128.fromInts(1, 2, 3, 4);
      final V128 b = V128.fromInts(1, 2, 3, 4);

      assertEquals(a, b, "Same values should be equal");
      assertEquals(a.hashCode(), b.hashCode(), "Hash codes should match");

      LOGGER.info("Equality verified");
    }

    @Test
    @DisplayName("should not be equal if different values")
    void shouldNotBeEqualIfDifferentValues() {
      LOGGER.info("Testing V128 inequality");

      final V128 a = V128.fromInts(1, 2, 3, 4);
      final V128 b = V128.fromInts(1, 2, 3, 5);

      assertNotEquals(a, b, "Different values should not be equal");

      LOGGER.info("Inequality verified");
    }
  }
}
