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

package ai.tegmentum.wasmtime4j.experimental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for advanced SIMD operations experimental feature. */
final class SimdOperationsTest {

  @BeforeEach
  void setUp() {
    // Enable advanced SIMD feature for tests
    ExperimentalFeatures.enableFeature(ExperimentalFeatures.Feature.ADVANCED_SIMD);
  }

  @AfterEach
  void tearDown() {
    // Clean up experimental features
    ExperimentalFeatures.reset();
  }

  @Test
  void testV128Creation() {
    // Test zero vector
    final SimdOperations.V128 zero = SimdOperations.V128.zero();
    assertArrayEquals(new byte[16], zero.getData());

    // Test splat vector
    final SimdOperations.V128 splat = SimdOperations.V128.splat((byte) 0xFF);
    final byte[] expectedSplat = new byte[16];
    Arrays.fill(expectedSplat, (byte) 0xFF);
    assertArrayEquals(expectedSplat, splat.getData());

    // Test from integers
    final SimdOperations.V128 fromInts = SimdOperations.V128.fromInts(1, 2, 3, 4);
    final int[] resultInts = fromInts.getAsInts();
    assertArrayEquals(new int[] {1, 2, 3, 4}, resultInts);

    // Test from floats
    final SimdOperations.V128 fromFloats = SimdOperations.V128.fromFloats(1.0f, 2.0f, 3.0f, 4.0f);
    final float[] resultFloats = fromFloats.getAsFloats();
    assertArrayEquals(new float[] {1.0f, 2.0f, 3.0f, 4.0f}, resultFloats, 0.001f);
  }

  @Test
  void testV128WithInvalidData() {
    assertThrows(IllegalArgumentException.class, () -> new SimdOperations.V128(null));
    assertThrows(IllegalArgumentException.class, () -> new SimdOperations.V128(new byte[15]));
    assertThrows(IllegalArgumentException.class, () -> new SimdOperations.V128(new byte[17]));
  }

  @Test
  void testV128Equality() {
    final SimdOperations.V128 v1 = SimdOperations.V128.fromInts(1, 2, 3, 4);
    final SimdOperations.V128 v2 = SimdOperations.V128.fromInts(1, 2, 3, 4);
    final SimdOperations.V128 v3 = SimdOperations.V128.fromInts(5, 6, 7, 8);

    assertEquals(v1, v2);
    assertNotEquals(v1, v3);
    assertEquals(v1.hashCode(), v2.hashCode());
  }

  @Test
  void testV128ToString() {
    final SimdOperations.V128 vector = SimdOperations.V128.fromInts(1, 2, 3, 4);
    final String result = vector.toString();

    assertTrue(result.contains("V128"));
    assertTrue(result.contains("data="));
  }

  @Test
  void testSimdOperationsCreation() {
    final SimdOperations.SimdConfig config =
        SimdOperations.SimdConfig.builder()
            .enablePlatformOptimizations(true)
            .enableRelaxedOperations(false)
            .validateVectorOperands(true)
            .maxVectorWidth(256)
            .build();

    final SimdOperations simd = new SimdOperations(config);

    assertNotNull(simd);
    assertEquals(config, simd.getConfig());
    assertTrue(simd.getNativeHandle() > 0);
  }

  @Test
  void testSimdOperationsWithNullConfig() {
    assertThrows(IllegalArgumentException.class, () -> new SimdOperations(null));
  }

  @Test
  void testSimdOperationsWithFeatureDisabled() {
    ExperimentalFeatures.disableFeature(ExperimentalFeatures.Feature.ADVANCED_SIMD);

    final SimdOperations.SimdConfig config = SimdOperations.SimdConfig.builder().build();

    assertThrows(UnsupportedOperationException.class, () -> new SimdOperations(config));
  }

  @Test
  void testSimdConfigBuilder() {
    final SimdOperations.SimdConfig config =
        SimdOperations.SimdConfig.builder()
            .enablePlatformOptimizations(false)
            .enableRelaxedOperations(true)
            .validateVectorOperands(false)
            .maxVectorWidth(512)
            .build();

    assertFalse(config.isPlatformOptimizationsEnabled());
    assertTrue(config.isRelaxedOperationsEnabled());
    assertFalse(config.isVectorOperandValidationEnabled());
    assertEquals(512, config.getMaxVectorWidth());
  }

  @Test
  void testSimdConfigBuilderWithInvalidWidth() {
    assertThrows(
        IllegalArgumentException.class,
        () -> SimdOperations.SimdConfig.builder().maxVectorWidth(0));

    assertThrows(
        IllegalArgumentException.class,
        () -> SimdOperations.SimdConfig.builder().maxVectorWidth(100)); // Not multiple of 128
  }

  @Test
  void testVectorAddition() {
    final SimdOperations.SimdConfig config = SimdOperations.SimdConfig.builder().build();
    final SimdOperations simd = new SimdOperations(config);

    final SimdOperations.V128 a = SimdOperations.V128.fromInts(1, 2, 3, 4);
    final SimdOperations.V128 b = SimdOperations.V128.fromInts(5, 6, 7, 8);

    final SimdOperations.V128 result = simd.add(a, b);
    final int[] resultInts = result.getAsInts();

    assertArrayEquals(new int[] {6, 8, 10, 12}, resultInts);
  }

  @Test
  void testVectorSubtraction() {
    final SimdOperations.SimdConfig config = SimdOperations.SimdConfig.builder().build();
    final SimdOperations simd = new SimdOperations(config);

    final SimdOperations.V128 a = SimdOperations.V128.fromInts(10, 20, 30, 40);
    final SimdOperations.V128 b = SimdOperations.V128.fromInts(1, 2, 3, 4);

    final SimdOperations.V128 result = simd.subtract(a, b);
    final int[] resultInts = result.getAsInts();

    assertArrayEquals(new int[] {9, 18, 27, 36}, resultInts);
  }

  @Test
  void testVectorMultiplication() {
    final SimdOperations.SimdConfig config = SimdOperations.SimdConfig.builder().build();
    final SimdOperations simd = new SimdOperations(config);

    final SimdOperations.V128 a = SimdOperations.V128.fromInts(2, 3, 4, 5);
    final SimdOperations.V128 b = SimdOperations.V128.fromInts(3, 4, 5, 6);

    final SimdOperations.V128 result = simd.multiply(a, b);
    final int[] resultInts = result.getAsInts();

    assertArrayEquals(new int[] {6, 12, 20, 30}, resultInts);
  }

  @Test
  void testVectorShuffle() {
    final SimdOperations.SimdConfig config = SimdOperations.SimdConfig.builder().build();
    final SimdOperations simd = new SimdOperations(config);

    final SimdOperations.V128 a =
        SimdOperations.V128.fromInts(0x01020304, 0x05060708, 0x090A0B0C, 0x0D0E0F10);
    final SimdOperations.V128 b =
        SimdOperations.V128.fromInts(0x11121314, 0x15161718, 0x191A1B1C, 0x1D1E1F20);

    // Reverse the bytes from vector a
    final byte[] indices = {3, 2, 1, 0, 7, 6, 5, 4, 11, 10, 9, 8, 15, 14, 13, 12};
    final SimdOperations.V128 result = simd.shuffle(a, b, indices);

    final SimdOperations.V128 expected =
        SimdOperations.V128.fromInts(0x04030201, 0x08070605, 0x0C0B0A09, 0x100F0E0D);
    assertEquals(expected, result);
  }

  @Test
  void testVectorShuffleWithInvalidIndices() {
    final SimdOperations.SimdConfig config = SimdOperations.SimdConfig.builder().build();
    final SimdOperations simd = new SimdOperations(config);

    final SimdOperations.V128 a = SimdOperations.V128.zero();
    final SimdOperations.V128 b = SimdOperations.V128.zero();

    // Index too large
    final byte[] invalidIndices = new byte[16];
    Arrays.fill(invalidIndices, (byte) 32);

    assertThrows(RuntimeException.class, () -> simd.shuffle(a, b, invalidIndices));
  }

  @Test
  void testRelaxedAdditionEnabled() {
    final SimdOperations.SimdConfig config =
        SimdOperations.SimdConfig.builder().enableRelaxedOperations(true).build();
    final SimdOperations simd = new SimdOperations(config);

    final SimdOperations.V128 a = SimdOperations.V128.fromFloats(1.0f, 2.0f, 3.0f, 4.0f);
    final SimdOperations.V128 b = SimdOperations.V128.fromFloats(0.5f, 1.5f, 2.5f, 3.5f);

    final SimdOperations.V128 result = simd.relaxedAdd(a, b);
    final float[] resultFloats = result.getAsFloats();

    assertArrayEquals(new float[] {1.5f, 3.5f, 5.5f, 7.5f}, resultFloats, 0.001f);
  }

  @Test
  void testRelaxedAdditionDisabled() {
    final SimdOperations.SimdConfig config =
        SimdOperations.SimdConfig.builder().enableRelaxedOperations(false).build();
    final SimdOperations simd = new SimdOperations(config);

    final SimdOperations.V128 a = SimdOperations.V128.fromFloats(1.0f, 2.0f, 3.0f, 4.0f);
    final SimdOperations.V128 b = SimdOperations.V128.fromFloats(0.5f, 1.5f, 2.5f, 3.5f);

    assertThrows(UnsupportedOperationException.class, () -> simd.relaxedAdd(a, b));
  }

  @Test
  void testVectorOperationsWithNullOperands() {
    final SimdOperations.SimdConfig config = SimdOperations.SimdConfig.builder().build();
    final SimdOperations simd = new SimdOperations(config);

    final SimdOperations.V128 vector = SimdOperations.V128.zero();

    assertThrows(IllegalArgumentException.class, () -> simd.add(null, vector));
    assertThrows(IllegalArgumentException.class, () -> simd.add(vector, null));
    assertThrows(IllegalArgumentException.class, () -> simd.subtract(null, vector));
    assertThrows(IllegalArgumentException.class, () -> simd.multiply(vector, null));
  }

  @Test
  void testVectorOperationsWithValidationDisabled() {
    final SimdOperations.SimdConfig config =
        SimdOperations.SimdConfig.builder().validateVectorOperands(false).build();
    final SimdOperations simd = new SimdOperations(config);

    final SimdOperations.V128 a = SimdOperations.V128.fromInts(1, 2, 3, 4);
    final SimdOperations.V128 b = SimdOperations.V128.fromInts(5, 6, 7, 8);

    // With validation disabled, operations should still work
    final SimdOperations.V128 result = simd.add(a, b);
    assertNotNull(result);
  }

  @Test
  void testSimdResourceManagement() {
    final SimdOperations.SimdConfig config = SimdOperations.SimdConfig.builder().build();
    final SimdOperations simd = new SimdOperations(config);

    assertTrue(simd.getNativeHandle() > 0);

    // Test that close can be called multiple times safely
    assertDoesNotThrow(simd::close);
    assertDoesNotThrow(simd::close);
  }

  @Test
  void testMemoryOperations() {
    final SimdOperations.SimdConfig config = SimdOperations.SimdConfig.builder().build();
    final SimdOperations simd = new SimdOperations(config);

    // Note: These tests would require actual WebAssembly memory instances
    // For now, we test the parameter validation

    assertThrows(IllegalArgumentException.class, () -> simd.load(null, 0));
    assertThrows(IllegalArgumentException.class, () -> simd.load(null, -1));

    final SimdOperations.V128 vector = SimdOperations.V128.zero();
    assertThrows(IllegalArgumentException.class, () -> simd.store(null, 0, vector));
    assertThrows(IllegalArgumentException.class, () -> simd.store(null, 0, null));
  }
}
