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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmtimeException;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/** Comprehensive tests for SIMD operations. */
final class SimdOperationsTest {

  private SimdOperations.SimdConfig config;
  private MockWasmRuntime mockRuntime;
  private SimdOperations simdOps;

  @BeforeEach
  void setUp(final TestInfo testInfo) {
    System.out.printf("Starting test: %s%n", testInfo.getDisplayName());
    config = SimdOperations.SimdConfig.defaultConfig();
    mockRuntime = new MockWasmRuntime();
    simdOps = new SimdOperations(config, mockRuntime);
  }

  // ===== V128 VECTOR TESTS =====

  @Test
  void testV128Creation() {
    System.out.println("Testing V128 vector creation methods");

    // Test zero vector
    final SimdOperations.V128 zero = SimdOperations.V128.zero();
    assertArrayEquals(new byte[16], zero.getData());
    System.out.println("✓ Zero vector creation works");

    // Test splat vector
    final SimdOperations.V128 splat = SimdOperations.V128.splat((byte) 0xFF);
    final byte[] expectedSplat = new byte[16];
    Arrays.fill(expectedSplat, (byte) 0xFF);
    assertArrayEquals(expectedSplat, splat.getData());
    System.out.println("✓ Splat vector creation works");

    // Test from integers
    final SimdOperations.V128 fromInts = SimdOperations.V128.fromInts(1, 2, 3, 4);
    final int[] resultInts = fromInts.getAsInts();
    assertArrayEquals(new int[] {1, 2, 3, 4}, resultInts);
    System.out.println("✓ Integer vector creation works");

    // Test from floats
    final SimdOperations.V128 fromFloats = SimdOperations.V128.fromFloats(1.0f, 2.0f, 3.0f, 4.0f);
    final float[] resultFloats = fromFloats.getAsFloats();
    assertArrayEquals(new float[] {1.0f, 2.0f, 3.0f, 4.0f}, resultFloats, 0.001f);
    System.out.println("✓ Float vector creation works");

    // Test from longs
    final SimdOperations.V128 fromLongs =
        SimdOperations.V128.fromLongs(0x123456789ABCDEFL, 0xFEDCBA9876543210L);
    final long[] resultLongs = fromLongs.getAsLongs();
    assertArrayEquals(new long[] {0x123456789ABCDEFL, 0xFEDCBA9876543210L}, resultLongs);
    System.out.println("✓ Long vector creation works");

    // Test from doubles
    final SimdOperations.V128 fromDoubles = SimdOperations.V128.fromDoubles(1.5, 2.5);
    final double[] resultDoubles = fromDoubles.getAsDoubles();
    assertArrayEquals(new double[] {1.5, 2.5}, resultDoubles, 0.001);
    System.out.println("✓ Double vector creation works");
  }

  @Test
  void testV128WithInvalidData() {
    System.out.println("Testing V128 with invalid data");

    assertThrows(IllegalArgumentException.class, () -> new SimdOperations.V128(null));
    assertThrows(IllegalArgumentException.class, () -> new SimdOperations.V128(new byte[15]));
    assertThrows(IllegalArgumentException.class, () -> new SimdOperations.V128(new byte[17]));
    System.out.println("✓ Invalid V128 data properly rejected");
  }

  @Test
  void testV128Equality() {
    System.out.println("Testing V128 equality and hash code");

    final SimdOperations.V128 v1 = SimdOperations.V128.fromInts(1, 2, 3, 4);
    final SimdOperations.V128 v2 = SimdOperations.V128.fromInts(1, 2, 3, 4);
    final SimdOperations.V128 v3 = SimdOperations.V128.fromInts(5, 6, 7, 8);

    assertEquals(v1, v2);
    assertNotEquals(v1, v3);
    assertEquals(v1.hashCode(), v2.hashCode());
    System.out.println("✓ V128 equality and hash code work correctly");
  }

  @Test
  void testV128ToString() {
    System.out.println("Testing V128 toString");

    final SimdOperations.V128 vector = SimdOperations.V128.fromInts(1, 2, 3, 4);
    final String result = vector.toString();

    assertTrue(result.contains("V128"));
    assertTrue(result.contains("data="));
    System.out.println("✓ V128 toString produces valid output: " + result);
  }

  // ===== SIMD CONFIG TESTS =====

  @Test
  void testSimdConfigCreation() {
    System.out.println("Testing SIMD configuration creation");

    final SimdOperations.SimdConfig customConfig =
        SimdOperations.SimdConfig.builder()
            .enablePlatformOptimizations(true)
            .enableRelaxedOperations(false)
            .validateVectorOperands(true)
            .maxVectorWidth(256)
            .build();

    assertTrue(customConfig.isPlatformOptimizationsEnabled());
    assertFalse(customConfig.isRelaxedOperationsEnabled());
    assertTrue(customConfig.isVectorOperandValidationEnabled());
    assertEquals(256, customConfig.getMaxVectorWidth());
    System.out.println("✓ Custom SIMD config creation works");

    final SimdOperations.SimdConfig defaultConfig = SimdOperations.SimdConfig.defaultConfig();
    assertNotNull(defaultConfig);
    System.out.println("✓ Default SIMD config creation works");
  }

  @Test
  void testSimdConfigBuilderValidation() {
    System.out.println("Testing SIMD config builder validation");

    assertThrows(
        IllegalArgumentException.class,
        () -> SimdOperations.SimdConfig.builder().maxVectorWidth(0));

    assertThrows(
        IllegalArgumentException.class,
        () -> SimdOperations.SimdConfig.builder().maxVectorWidth(100)); // Not multiple of 128

    System.out.println("✓ SIMD config builder validation works");
  }

  @Test
  void testSimdConfigEquality() {
    System.out.println("Testing SIMD config equality");

    final SimdOperations.SimdConfig config1 = SimdOperations.SimdConfig.builder().build();
    final SimdOperations.SimdConfig config2 = SimdOperations.SimdConfig.builder().build();
    final SimdOperations.SimdConfig config3 =
        SimdOperations.SimdConfig.builder().enableRelaxedOperations(true).build();

    assertEquals(config1, config2);
    assertNotEquals(config1, config3);
    assertEquals(config1.hashCode(), config2.hashCode());
    System.out.println("✓ SIMD config equality works");
  }

  // ===== SIMD OPERATIONS TESTS =====

  @Test
  void testSimdOperationsCreation() {
    System.out.println("Testing SIMD operations creation");

    assertNotNull(simdOps);
    assertEquals(config, simdOps.getConfig());
    assertEquals(mockRuntime, simdOps.getRuntime());
    System.out.println("✓ SIMD operations creation works");

    final SimdOperations defaultSimd = SimdOperations.create(mockRuntime);
    assertNotNull(defaultSimd);
    System.out.println("✓ Default SIMD operations creation works");
  }

  @Test
  void testSimdOperationsWithNullParameters() {
    System.out.println("Testing SIMD operations with null parameters");

    assertThrows(IllegalArgumentException.class, () -> new SimdOperations(null, mockRuntime));
    assertThrows(IllegalArgumentException.class, () -> new SimdOperations(config, null));
    assertThrows(IllegalArgumentException.class, () -> SimdOperations.create(null));
    System.out.println("✓ Null parameter validation works");
  }

  // ===== ARITHMETIC OPERATIONS TESTS =====

  @Test
  void testArithmeticOperations() throws WasmtimeException {
    System.out.println("Testing SIMD arithmetic operations");

    final SimdOperations.V128 a = SimdOperations.V128.fromInts(1, 2, 3, 4);
    final SimdOperations.V128 b = SimdOperations.V128.fromInts(5, 6, 7, 8);

    // Test addition
    mockRuntime.setExpectedSimdResult(SimdOperations.V128.fromInts(6, 8, 10, 12));
    final SimdOperations.V128 addResult = simdOps.add(a, b);
    assertArrayEquals(new int[] {6, 8, 10, 12}, addResult.getAsInts());
    System.out.println("✓ SIMD addition works");

    // Test subtraction
    mockRuntime.setExpectedSimdResult(SimdOperations.V128.fromInts(-4, -4, -4, -4));
    final SimdOperations.V128 subResult = simdOps.subtract(a, b);
    assertArrayEquals(new int[] {-4, -4, -4, -4}, subResult.getAsInts());
    System.out.println("✓ SIMD subtraction works");

    // Test multiplication
    mockRuntime.setExpectedSimdResult(SimdOperations.V128.fromInts(5, 12, 21, 32));
    final SimdOperations.V128 mulResult = simdOps.multiply(a, b);
    assertArrayEquals(new int[] {5, 12, 21, 32}, mulResult.getAsInts());
    System.out.println("✓ SIMD multiplication works");

    // Test division
    final SimdOperations.V128 divA = SimdOperations.V128.fromFloats(8.0f, 12.0f, 20.0f, 30.0f);
    final SimdOperations.V128 divB = SimdOperations.V128.fromFloats(2.0f, 3.0f, 4.0f, 5.0f);
    mockRuntime.setExpectedSimdResult(SimdOperations.V128.fromFloats(4.0f, 4.0f, 5.0f, 6.0f));
    final SimdOperations.V128 divResult = simdOps.divide(divA, divB);
    assertArrayEquals(new float[] {4.0f, 4.0f, 5.0f, 6.0f}, divResult.getAsFloats(), 0.001f);
    System.out.println("✓ SIMD division works");
  }

  @Test
  void testSaturatedAddition() throws WasmtimeException {
    System.out.println("Testing SIMD saturated addition");

    final SimdOperations.V128 a =
        SimdOperations.V128.fromInts(Integer.MAX_VALUE - 1, 100, 200, 300);
    final SimdOperations.V128 b = SimdOperations.V128.fromInts(5, 10, 20, 30);

    mockRuntime.setExpectedSimdResult(
        SimdOperations.V128.fromInts(Integer.MAX_VALUE, 110, 220, 330));
    final SimdOperations.V128 result = simdOps.addSaturated(a, b);
    final int[] resultInts = result.getAsInts();

    assertEquals(Integer.MAX_VALUE, resultInts[0]); // Saturated
    assertEquals(110, resultInts[1]);
    assertEquals(220, resultInts[2]);
    assertEquals(330, resultInts[3]);
    System.out.println("✓ SIMD saturated addition works");
  }

  // ===== LOGICAL OPERATIONS TESTS =====

  @Test
  void testLogicalOperations() throws WasmtimeException {
    System.out.println("Testing SIMD logical operations");

    final SimdOperations.V128 a =
        SimdOperations.V128.fromInts(0xFF00FF00, 0x00FF00FF, 0xF0F0F0F0, 0x0F0F0F0F);
    final SimdOperations.V128 b =
        SimdOperations.V128.fromInts(0xF0F0F0F0, 0x0F0F0F0F, 0xFF00FF00, 0x00FF00FF);

    // Test AND
    mockRuntime.setExpectedSimdResult(
        SimdOperations.V128.fromInts(0xF000F000, 0x000F000F, 0xF000F000, 0x000F000F));
    final SimdOperations.V128 andResult = simdOps.and(a, b);
    assertEquals(0xF000F000, andResult.getAsInts()[0]);
    System.out.println("✓ SIMD AND works");

    // Test OR
    mockRuntime.setExpectedSimdResult(
        SimdOperations.V128.fromInts(0xFFF0FFF0, 0x0FFF0FFF, 0xFFF0FFF0, 0x0FFF0FFF));
    final SimdOperations.V128 orResult = simdOps.or(a, b);
    assertEquals(0xFFF0FFF0, orResult.getAsInts()[0]);
    System.out.println("✓ SIMD OR works");

    // Test XOR
    mockRuntime.setExpectedSimdResult(
        SimdOperations.V128.fromInts(0x0FF00FF0, 0x0FF00FF0, 0x0FF00FF0, 0x0FF00FF0));
    final SimdOperations.V128 xorResult = simdOps.xor(a, b);
    assertEquals(0x0FF00FF0, xorResult.getAsInts()[0]);
    System.out.println("✓ SIMD XOR works");

    // Test NOT
    mockRuntime.setExpectedSimdResult(
        SimdOperations.V128.fromInts(~0xFF00FF00, ~0x00FF00FF, ~0xF0F0F0F0, ~0x0F0F0F0F));
    final SimdOperations.V128 notResult = simdOps.not(a);
    assertEquals(~0xFF00FF00, notResult.getAsInts()[0]);
    System.out.println("✓ SIMD NOT works");
  }

  // ===== COMPARISON OPERATIONS TESTS =====

  @Test
  void testComparisonOperations() throws WasmtimeException {
    System.out.println("Testing SIMD comparison operations");

    final SimdOperations.V128 a = SimdOperations.V128.fromInts(1, 5, 3, 7);
    final SimdOperations.V128 b = SimdOperations.V128.fromInts(1, 4, 5, 6);

    // Test equals
    mockRuntime.setExpectedSimdResult(SimdOperations.V128.fromInts(-1, 0, 0, 0));
    final SimdOperations.V128 eqResult = simdOps.equals(a, b);
    assertArrayEquals(
        new int[] {-1, 0, 0, 0}, eqResult.getAsInts()); // Only first elements are equal
    System.out.println("✓ SIMD equals works");

    // Test less than
    mockRuntime.setExpectedSimdResult(SimdOperations.V128.fromInts(0, 0, -1, 0));
    final SimdOperations.V128 ltResult = simdOps.lessThan(a, b);
    assertArrayEquals(new int[] {0, 0, -1, 0}, ltResult.getAsInts()); // Only third element: 3 < 5
    System.out.println("✓ SIMD less than works");

    // Test greater than
    mockRuntime.setExpectedSimdResult(SimdOperations.V128.fromInts(0, -1, 0, -1));
    final SimdOperations.V128 gtResult = simdOps.greaterThan(a, b);
    assertArrayEquals(
        new int[] {0, -1, 0, -1}, gtResult.getAsInts()); // Second and fourth: 5 > 4, 7 > 6
    System.out.println("✓ SIMD greater than works");
  }

  // ===== MEMORY OPERATIONS TESTS =====

  @Test
  void testMemoryOperations() throws WasmtimeException {
    System.out.println("Testing SIMD memory operations");

    final MockWasmMemory memory = new MockWasmMemory();
    final SimdOperations.V128 vector = SimdOperations.V128.fromInts(1, 2, 3, 4);

    // Test basic load/store
    mockRuntime.setExpectedSimdResult(vector);
    final SimdOperations.V128 loadResult = simdOps.load(memory, 0);
    assertEquals(vector, loadResult);
    System.out.println("✓ SIMD load works");

    simdOps.store(memory, 0, vector);
    System.out.println("✓ SIMD store works");

    // Test aligned load/store
    final SimdOperations.V128 alignedLoadResult = simdOps.loadAligned(memory, 16, 16);
    assertEquals(vector, alignedLoadResult);
    System.out.println("✓ SIMD aligned load works");

    simdOps.storeAligned(memory, 16, vector, 16);
    System.out.println("✓ SIMD aligned store works");
  }

  @Test
  void testMemoryOperationsValidation() {
    System.out.println("Testing SIMD memory operations validation");

    final MockWasmMemory memory = new MockWasmMemory();
    final SimdOperations.V128 vector = SimdOperations.V128.fromInts(1, 2, 3, 4);

    // Test null memory
    assertThrows(IllegalArgumentException.class, () -> simdOps.load(null, 0));
    assertThrows(IllegalArgumentException.class, () -> simdOps.store(null, 0, vector));

    // Test negative offset
    assertThrows(IllegalArgumentException.class, () -> simdOps.load(memory, -1));
    assertThrows(IllegalArgumentException.class, () -> simdOps.store(memory, -1, vector));

    // Test null vector for store
    assertThrows(IllegalArgumentException.class, () -> simdOps.store(memory, 0, null));

    // Test invalid alignment
    assertThrows(
        IllegalArgumentException.class, () -> simdOps.loadAligned(memory, 0, 3)); // Not power of 2
    assertThrows(
        IllegalArgumentException.class, () -> simdOps.loadAligned(memory, 0, 32)); // Too large

    // Test misaligned offset
    assertThrows(
        IllegalArgumentException.class, () -> simdOps.loadAligned(memory, 5, 4)); // 5 % 4 != 0

    System.out.println("✓ SIMD memory operations validation works");
  }

  // ===== CONVERSION OPERATIONS TESTS =====

  @Test
  void testConversionOperations() throws WasmtimeException {
    System.out.println("Testing SIMD conversion operations");

    // Test int to float conversion
    final SimdOperations.V128 intVec = SimdOperations.V128.fromInts(1, 2, 3, 4);
    mockRuntime.setExpectedSimdResult(SimdOperations.V128.fromFloats(1.0f, 2.0f, 3.0f, 4.0f));
    final SimdOperations.V128 floatResult = simdOps.convertI32ToF32(intVec);
    assertArrayEquals(new float[] {1.0f, 2.0f, 3.0f, 4.0f}, floatResult.getAsFloats(), 0.001f);
    System.out.println("✓ SIMD int to float conversion works");

    // Test float to int conversion
    final SimdOperations.V128 floatVec = SimdOperations.V128.fromFloats(1.5f, 2.7f, 3.1f, 4.9f);
    mockRuntime.setExpectedSimdResult(SimdOperations.V128.fromInts(1, 2, 3, 4));
    final SimdOperations.V128 intResult = simdOps.convertF32ToI32(floatVec);
    assertArrayEquals(new int[] {1, 2, 3, 4}, intResult.getAsInts()); // Truncation behavior
    System.out.println("✓ SIMD float to int conversion works");
  }

  // ===== LANE OPERATIONS TESTS =====

  @Test
  void testLaneOperations() throws WasmtimeException {
    System.out.println("Testing SIMD lane operations");

    final SimdOperations.V128 vec = SimdOperations.V128.fromInts(10, 20, 30, 40);

    // Test extract lane
    mockRuntime.setExpectedIntResult(10);
    assertEquals(10, simdOps.extractLaneI32(vec, 0));
    mockRuntime.setExpectedIntResult(30);
    assertEquals(30, simdOps.extractLaneI32(vec, 2));
    System.out.println("✓ SIMD extract lane works");

    // Test invalid lane index
    assertThrows(IllegalArgumentException.class, () -> simdOps.extractLaneI32(vec, 4));
    assertThrows(IllegalArgumentException.class, () -> simdOps.extractLaneI32(vec, -1));

    // Test replace lane
    mockRuntime.setExpectedSimdResult(SimdOperations.V128.fromInts(10, 99, 30, 40));
    final SimdOperations.V128 replaced = simdOps.replaceLaneI32(vec, 1, 99);
    assertArrayEquals(new int[] {10, 99, 30, 40}, replaced.getAsInts());
    System.out.println("✓ SIMD replace lane works");

    // Test invalid lane index for replace
    assertThrows(IllegalArgumentException.class, () -> simdOps.replaceLaneI32(vec, 5, 99));
    assertThrows(IllegalArgumentException.class, () -> simdOps.replaceLaneI32(vec, -1, 99));
  }

  @Test
  void testSplatOperations() throws WasmtimeException {
    System.out.println("Testing SIMD splat operations");

    // Test int splat
    mockRuntime.setExpectedSimdResult(SimdOperations.V128.fromInts(42, 42, 42, 42));
    final SimdOperations.V128 intSplat = simdOps.splatI32(42);
    assertArrayEquals(new int[] {42, 42, 42, 42}, intSplat.getAsInts());
    System.out.println("✓ SIMD int splat works");

    // Test float splat
    mockRuntime.setExpectedSimdResult(SimdOperations.V128.fromFloats(3.14f, 3.14f, 3.14f, 3.14f));
    final SimdOperations.V128 floatSplat = simdOps.splatF32(3.14f);
    assertArrayEquals(new float[] {3.14f, 3.14f, 3.14f, 3.14f}, floatSplat.getAsFloats(), 0.001f);
    System.out.println("✓ SIMD float splat works");
  }

  // ===== SHUFFLE OPERATIONS TESTS =====

  @Test
  void testShuffleOperations() throws WasmtimeException {
    System.out.println("Testing SIMD shuffle operations");

    final SimdOperations.V128 a =
        SimdOperations.V128.fromInts(0x01020304, 0x05060708, 0x090A0B0C, 0x0D0E0F10);
    final SimdOperations.V128 b =
        SimdOperations.V128.fromInts(0x11121314, 0x15161718, 0x191A1B1C, 0x1D1E1F20);

    // Reverse the bytes from vector a
    final byte[] indices = {3, 2, 1, 0, 7, 6, 5, 4, 11, 10, 9, 8, 15, 14, 13, 12};
    mockRuntime.setExpectedSimdResult(
        SimdOperations.V128.fromInts(0x04030201, 0x08070605, 0x0C0B0A09, 0x100F0E0D));
    final SimdOperations.V128 result = simdOps.shuffle(a, b, indices);

    final SimdOperations.V128 expected =
        SimdOperations.V128.fromInts(0x04030201, 0x08070605, 0x0C0B0A09, 0x100F0E0D);
    assertEquals(expected, result);
    System.out.println("✓ SIMD shuffle works");
  }

  @Test
  void testShuffleValidation() {
    System.out.println("Testing SIMD shuffle validation");

    final SimdOperations.V128 a = SimdOperations.V128.zero();
    final SimdOperations.V128 b = SimdOperations.V128.zero();

    // Test null indices
    assertThrows(IllegalArgumentException.class, () -> simdOps.shuffle(a, b, null));

    // Test wrong length indices
    assertThrows(IllegalArgumentException.class, () -> simdOps.shuffle(a, b, new byte[15]));

    // Test invalid indices
    final byte[] invalidIndices = new byte[16];
    Arrays.fill(invalidIndices, (byte) 32);
    assertThrows(IllegalArgumentException.class, () -> simdOps.shuffle(a, b, invalidIndices));

    final byte[] negativeIndices = new byte[16];
    negativeIndices[0] = -1;
    assertThrows(IllegalArgumentException.class, () -> simdOps.shuffle(a, b, negativeIndices));

    System.out.println("✓ SIMD shuffle validation works");
  }

  // ===== RELAXED OPERATIONS TESTS =====

  @Test
  void testRelaxedOperations() throws WasmtimeException {
    System.out.println("Testing SIMD relaxed operations");

    final SimdOperations.SimdConfig relaxedConfig =
        SimdOperations.SimdConfig.builder().enableRelaxedOperations(true).build();
    final SimdOperations relaxedSimd = new SimdOperations(relaxedConfig, mockRuntime);

    final SimdOperations.V128 a = SimdOperations.V128.fromFloats(1.0f, 2.0f, 3.0f, 4.0f);
    final SimdOperations.V128 b = SimdOperations.V128.fromFloats(0.5f, 1.5f, 2.5f, 3.5f);

    mockRuntime.setExpectedSimdResult(SimdOperations.V128.fromFloats(1.5f, 3.5f, 5.5f, 7.5f));
    final SimdOperations.V128 result = relaxedSimd.relaxedAdd(a, b);
    assertArrayEquals(new float[] {1.5f, 3.5f, 5.5f, 7.5f}, result.getAsFloats(), 0.001f);
    System.out.println("✓ SIMD relaxed addition works");
  }

  @Test
  void testRelaxedOperationsDisabled() {
    System.out.println("Testing SIMD relaxed operations when disabled");

    final SimdOperations.V128 a = SimdOperations.V128.fromFloats(1.0f, 2.0f, 3.0f, 4.0f);
    final SimdOperations.V128 b = SimdOperations.V128.fromFloats(0.5f, 1.5f, 2.5f, 3.5f);

    assertThrows(UnsupportedOperationException.class, () -> simdOps.relaxedAdd(a, b));
    System.out.println("✓ SIMD relaxed operations properly disabled");
  }

  // ===== VALIDATION TESTS =====

  @Test
  void testOperandValidation() {
    System.out.println("Testing SIMD operand validation");

    final SimdOperations.V128 vector = SimdOperations.V128.zero();

    // Test binary operations with null operands
    assertThrows(IllegalArgumentException.class, () -> simdOps.add(null, vector));
    assertThrows(IllegalArgumentException.class, () -> simdOps.add(vector, null));
    assertThrows(IllegalArgumentException.class, () -> simdOps.subtract(null, vector));
    assertThrows(IllegalArgumentException.class, () -> simdOps.multiply(vector, null));

    // Test unary operations with null operands
    assertThrows(IllegalArgumentException.class, () -> simdOps.not(null));
    assertThrows(IllegalArgumentException.class, () -> simdOps.convertI32ToF32(null));

    System.out.println("✓ SIMD operand validation works");
  }

  @Test
  void testValidationDisabled() throws WasmtimeException {
    System.out.println("Testing SIMD operations with validation disabled");

    final SimdOperations.SimdConfig noValidationConfig =
        SimdOperations.SimdConfig.builder().validateVectorOperands(false).build();
    final SimdOperations noValidationSimd = new SimdOperations(noValidationConfig, mockRuntime);

    final SimdOperations.V128 a = SimdOperations.V128.fromInts(1, 2, 3, 4);
    final SimdOperations.V128 b = SimdOperations.V128.fromInts(5, 6, 7, 8);

    mockRuntime.setExpectedSimdResult(SimdOperations.V128.fromInts(6, 8, 10, 12));

    // With validation disabled, operations should still work
    final SimdOperations.V128 result = noValidationSimd.add(a, b);
    assertNotNull(result);
    System.out.println("✓ SIMD operations work with validation disabled");
  }

  // ===== UTILITY TESTS =====

  @Test
  void testUtilityMethods() {
    System.out.println("Testing SIMD utility methods");

    assertTrue(simdOps.isSimdSupported());
    assertNotNull(simdOps.getSimdCapabilities());
    System.out.println("✓ SIMD utility methods work");
    System.out.println("  - SIMD supported: " + simdOps.isSimdSupported());
    System.out.println("  - SIMD capabilities: " + simdOps.getSimdCapabilities());
  }

  // ===== MOCK CLASSES =====

  /** Mock WebAssembly runtime for testing. */
  private static class MockWasmRuntime implements WasmRuntime {
    private SimdOperations.V128 expectedSimdResult = SimdOperations.V128.zero();
    private int expectedIntResult = 0;

    void setExpectedSimdResult(final SimdOperations.V128 result) {
      this.expectedSimdResult = result;
    }

    void setExpectedIntResult(final int result) {
      this.expectedIntResult = result;
    }

    @Override
    public boolean isSimdSupported() {
      return true;
    }

    @Override
    public String getSimdCapabilities() {
      return "Mock SIMD capabilities: SSE4.1, AVX, AVX2";
    }

    @Override
    public SimdOperations.V128 simdAdd(final SimdOperations.V128 a, final SimdOperations.V128 b) {
      return expectedSimdResult;
    }

    @Override
    public SimdOperations.V128 simdSubtract(
        final SimdOperations.V128 a, final SimdOperations.V128 b) {
      return expectedSimdResult;
    }

    @Override
    public SimdOperations.V128 simdMultiply(
        final SimdOperations.V128 a, final SimdOperations.V128 b) {
      return expectedSimdResult;
    }

    @Override
    public SimdOperations.V128 simdDivide(
        final SimdOperations.V128 a, final SimdOperations.V128 b) {
      return expectedSimdResult;
    }

    @Override
    public SimdOperations.V128 simdAddSaturated(
        final SimdOperations.V128 a, final SimdOperations.V128 b) {
      return expectedSimdResult;
    }

    @Override
    public SimdOperations.V128 simdAnd(final SimdOperations.V128 a, final SimdOperations.V128 b) {
      return expectedSimdResult;
    }

    @Override
    public SimdOperations.V128 simdOr(final SimdOperations.V128 a, final SimdOperations.V128 b) {
      return expectedSimdResult;
    }

    @Override
    public SimdOperations.V128 simdXor(final SimdOperations.V128 a, final SimdOperations.V128 b) {
      return expectedSimdResult;
    }

    @Override
    public SimdOperations.V128 simdNot(final SimdOperations.V128 a) {
      return expectedSimdResult;
    }

    @Override
    public SimdOperations.V128 simdEquals(
        final SimdOperations.V128 a, final SimdOperations.V128 b) {
      return expectedSimdResult;
    }

    @Override
    public SimdOperations.V128 simdLessThan(
        final SimdOperations.V128 a, final SimdOperations.V128 b) {
      return expectedSimdResult;
    }

    @Override
    public SimdOperations.V128 simdGreaterThan(
        final SimdOperations.V128 a, final SimdOperations.V128 b) {
      return expectedSimdResult;
    }

    @Override
    public SimdOperations.V128 simdLoad(final WasmMemory memory, final int offset) {
      return expectedSimdResult;
    }

    @Override
    public SimdOperations.V128 simdLoadAligned(
        final WasmMemory memory, final int offset, final int alignment) {
      return expectedSimdResult;
    }

    @Override
    public void simdStore(
        final WasmMemory memory, final int offset, final SimdOperations.V128 vector) {
      // Mock implementation
    }

    @Override
    public void simdStoreAligned(
        final WasmMemory memory,
        final int offset,
        final SimdOperations.V128 vector,
        final int alignment) {
      // Mock implementation
    }

    @Override
    public SimdOperations.V128 simdConvertI32ToF32(final SimdOperations.V128 vector) {
      return expectedSimdResult;
    }

    @Override
    public SimdOperations.V128 simdConvertF32ToI32(final SimdOperations.V128 vector) {
      return expectedSimdResult;
    }

    @Override
    public int simdExtractLaneI32(final SimdOperations.V128 vector, final int lane) {
      return expectedIntResult;
    }

    @Override
    public SimdOperations.V128 simdReplaceLaneI32(
        final SimdOperations.V128 vector, final int lane, final int value) {
      return expectedSimdResult;
    }

    @Override
    public SimdOperations.V128 simdSplatI32(final int value) {
      return expectedSimdResult;
    }

    @Override
    public SimdOperations.V128 simdSplatF32(final float value) {
      return expectedSimdResult;
    }

    @Override
    public SimdOperations.V128 simdShuffle(
        final SimdOperations.V128 a, final SimdOperations.V128 b, final byte[] indices) {
      return expectedSimdResult;
    }

    @Override
    public SimdOperations.V128 simdRelaxedAdd(
        final SimdOperations.V128 a, final SimdOperations.V128 b) {
      return expectedSimdResult;
    }

    // Implement other required methods with minimal stubs
    @Override
    public Engine createEngine() {
      return null;
    }

    @Override
    public Engine createEngine(final EngineConfig config) {
      return null;
    }

    @Override
    public Module compileModule(final Engine engine, final byte[] wasmBytes) {
      return null;
    }

    @Override
    public Module compileModule(final Engine engine, final String wasmText) {
      return null;
    }

    @Override
    public Instance instantiate(final Engine engine, final Module module) {
      return null;
    }

    @Override
    public Instance instantiate(final Engine engine, final Module module, final Store store) {
      return null;
    }

    @Override
    public Store createStore(final Engine engine) {
      return null;
    }

    @Override
    public ai.tegmentum.wasmtime4j.gc.GcRuntime createGcRuntime(final Engine engine) {
      return null;
    }

    @Override
    public RuntimeInfo getRuntimeInfo() {
      return null;
    }

    @Override
    public boolean isValid() {
      return true;
    }

    @Override
    public void close() {}
  }

  /** Mock WebAssembly memory for testing. */
  private static class MockWasmMemory implements WasmMemory {
    @Override
    public long size() {
      return 65536;
    }

    @Override
    public void grow(final long pages) {}

    @Override
    public byte readByte(final int offset) {
      return 0;
    }

    @Override
    public void writeByte(final int offset, final byte value) {}

    @Override
    public int readInt(final int offset) {
      return 0;
    }

    @Override
    public void writeInt(final int offset, final int value) {}

    @Override
    public long readLong(final int offset) {
      return 0;
    }

    @Override
    public void writeLong(final int offset, final long value) {}

    @Override
    public float readFloat(final int offset) {
      return 0.0f;
    }

    @Override
    public void writeFloat(final int offset, final float value) {}

    @Override
    public double readDouble(final int offset) {
      return 0.0;
    }

    @Override
    public void writeDouble(final int offset, final double value) {}

    @Override
    public void readBytes(
        final int offset, final byte[] buffer, final int bufferOffset, final int length) {}

    @Override
    public void writeBytes(
        final int offset, final byte[] buffer, final int bufferOffset, final int length) {}

    @Override
    public java.nio.ByteBuffer asByteBuffer() {
      return null;
    }

    @Override
    public void close() {}
  }
}
