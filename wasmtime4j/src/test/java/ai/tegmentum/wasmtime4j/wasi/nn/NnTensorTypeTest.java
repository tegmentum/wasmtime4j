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

package ai.tegmentum.wasmtime4j.wasi.nn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link NnTensorType} enum.
 *
 * <p>Verifies WASI-NN tensor type values, byte sizes, name mappings, native codes, and byte size
 * calculations.
 */
@DisplayName("NnTensorType Tests")
class NnTensorTypeTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("NnTensorType should be an enum")
    void shouldBeAnEnum() {
      assertTrue(NnTensorType.class.isEnum(), "NnTensorType should be an enum");
    }

    @Test
    @DisplayName("NnTensorType should have exactly 7 values")
    void shouldHaveExactlySevenValues() {
      assertEquals(
          7, NnTensorType.values().length, "Should have exactly 7 tensor type values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have FP16 value")
    void shouldHaveFp16Value() {
      assertNotNull(NnTensorType.FP16, "FP16 should exist");
      assertEquals("FP16", NnTensorType.FP16.name(), "Name should be FP16");
    }

    @Test
    @DisplayName("should have FP32 value")
    void shouldHaveFp32Value() {
      assertNotNull(NnTensorType.FP32, "FP32 should exist");
      assertEquals("FP32", NnTensorType.FP32.name(), "Name should be FP32");
    }

    @Test
    @DisplayName("should have FP64 value")
    void shouldHaveFp64Value() {
      assertNotNull(NnTensorType.FP64, "FP64 should exist");
      assertEquals("FP64", NnTensorType.FP64.name(), "Name should be FP64");
    }

    @Test
    @DisplayName("should have BF16 value")
    void shouldHaveBf16Value() {
      assertNotNull(NnTensorType.BF16, "BF16 should exist");
      assertEquals("BF16", NnTensorType.BF16.name(), "Name should be BF16");
    }

    @Test
    @DisplayName("should have U8 value")
    void shouldHaveU8Value() {
      assertNotNull(NnTensorType.U8, "U8 should exist");
      assertEquals("U8", NnTensorType.U8.name(), "Name should be U8");
    }

    @Test
    @DisplayName("should have I32 value")
    void shouldHaveI32Value() {
      assertNotNull(NnTensorType.I32, "I32 should exist");
      assertEquals("I32", NnTensorType.I32.name(), "Name should be I32");
    }

    @Test
    @DisplayName("should have I64 value")
    void shouldHaveI64Value() {
      assertNotNull(NnTensorType.I64, "I64 should exist");
      assertEquals("I64", NnTensorType.I64.name(), "Name should be I64");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("ordinals should be unique")
    void ordinalsShouldBeUnique() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final NnTensorType type : NnTensorType.values()) {
        assertTrue(ordinals.add(type.ordinal()), "Ordinal should be unique: " + type.ordinal());
      }
    }

    @Test
    @DisplayName("ordinals should be sequential starting at 0")
    void ordinalsShouldBeSequential() {
      final NnTensorType[] values = NnTensorType.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal(), "Ordinal should match index for " + values[i]);
      }
    }
  }

  @Nested
  @DisplayName("valueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("valueOf should return correct constant for each name")
    void valueOfShouldReturnCorrectConstant() {
      assertEquals(NnTensorType.FP16, NnTensorType.valueOf("FP16"), "Should return FP16");
      assertEquals(NnTensorType.FP32, NnTensorType.valueOf("FP32"), "Should return FP32");
      assertEquals(NnTensorType.FP64, NnTensorType.valueOf("FP64"), "Should return FP64");
      assertEquals(NnTensorType.BF16, NnTensorType.valueOf("BF16"), "Should return BF16");
      assertEquals(NnTensorType.U8, NnTensorType.valueOf("U8"), "Should return U8");
      assertEquals(NnTensorType.I32, NnTensorType.valueOf("I32"), "Should return I32");
      assertEquals(NnTensorType.I64, NnTensorType.valueOf("I64"), "Should return I64");
    }

    @Test
    @DisplayName("valueOf should throw for invalid name")
    void valueOfShouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> NnTensorType.valueOf("INVALID"),
          "Should throw for invalid enum name");
    }
  }

  @Nested
  @DisplayName("values() Tests")
  class ValuesTests {

    @Test
    @DisplayName("values() should return all enum constants")
    void valuesShouldReturnAllEnumConstants() {
      final NnTensorType[] values = NnTensorType.values();
      final Set<NnTensorType> valueSet = new HashSet<>(Arrays.asList(values));

      assertTrue(valueSet.contains(NnTensorType.FP16), "Should contain FP16");
      assertTrue(valueSet.contains(NnTensorType.FP32), "Should contain FP32");
      assertTrue(valueSet.contains(NnTensorType.FP64), "Should contain FP64");
      assertTrue(valueSet.contains(NnTensorType.BF16), "Should contain BF16");
      assertTrue(valueSet.contains(NnTensorType.U8), "Should contain U8");
      assertTrue(valueSet.contains(NnTensorType.I32), "Should contain I32");
      assertTrue(valueSet.contains(NnTensorType.I64), "Should contain I64");
    }

    @Test
    @DisplayName("values() should return new array each time")
    void valuesShouldReturnNewArrayEachTime() {
      final NnTensorType[] first = NnTensorType.values();
      final NnTensorType[] second = NnTensorType.values();

      assertTrue(first != second, "Should return new array each time");
      assertEquals(first.length, second.length, "Arrays should have same length");
    }
  }

  @Nested
  @DisplayName("GetByteSize Tests")
  class GetByteSizeTests {

    @Test
    @DisplayName("FP16 should have byte size 2")
    void fp16ShouldHaveByteSizeTwo() {
      assertEquals(2, NnTensorType.FP16.getByteSize(), "FP16 byte size should be 2");
    }

    @Test
    @DisplayName("FP32 should have byte size 4")
    void fp32ShouldHaveByteSizeFour() {
      assertEquals(4, NnTensorType.FP32.getByteSize(), "FP32 byte size should be 4");
    }

    @Test
    @DisplayName("FP64 should have byte size 8")
    void fp64ShouldHaveByteSizeEight() {
      assertEquals(8, NnTensorType.FP64.getByteSize(), "FP64 byte size should be 8");
    }

    @Test
    @DisplayName("BF16 should have byte size 2")
    void bf16ShouldHaveByteSizeTwo() {
      assertEquals(2, NnTensorType.BF16.getByteSize(), "BF16 byte size should be 2");
    }

    @Test
    @DisplayName("U8 should have byte size 1")
    void u8ShouldHaveByteSizeOne() {
      assertEquals(1, NnTensorType.U8.getByteSize(), "U8 byte size should be 1");
    }

    @Test
    @DisplayName("I32 should have byte size 4")
    void i32ShouldHaveByteSizeFour() {
      assertEquals(4, NnTensorType.I32.getByteSize(), "I32 byte size should be 4");
    }

    @Test
    @DisplayName("I64 should have byte size 8")
    void i64ShouldHaveByteSizeEight() {
      assertEquals(8, NnTensorType.I64.getByteSize(), "I64 byte size should be 8");
    }
  }

  @Nested
  @DisplayName("GetWasiName Tests")
  class GetWasiNameTests {

    @Test
    @DisplayName("FP16 should have wasi name 'fp16'")
    void fp16ShouldHaveCorrectWasiName() {
      assertEquals("fp16", NnTensorType.FP16.getWasiName(), "FP16 wasi name should be 'fp16'");
    }

    @Test
    @DisplayName("FP32 should have wasi name 'fp32'")
    void fp32ShouldHaveCorrectWasiName() {
      assertEquals("fp32", NnTensorType.FP32.getWasiName(), "FP32 wasi name should be 'fp32'");
    }

    @Test
    @DisplayName("FP64 should have wasi name 'fp64'")
    void fp64ShouldHaveCorrectWasiName() {
      assertEquals("fp64", NnTensorType.FP64.getWasiName(), "FP64 wasi name should be 'fp64'");
    }

    @Test
    @DisplayName("BF16 should have wasi name 'bf16'")
    void bf16ShouldHaveCorrectWasiName() {
      assertEquals("bf16", NnTensorType.BF16.getWasiName(), "BF16 wasi name should be 'bf16'");
    }

    @Test
    @DisplayName("U8 should have wasi name 'u8'")
    void u8ShouldHaveCorrectWasiName() {
      assertEquals("u8", NnTensorType.U8.getWasiName(), "U8 wasi name should be 'u8'");
    }

    @Test
    @DisplayName("I32 should have wasi name 'i32'")
    void i32ShouldHaveCorrectWasiName() {
      assertEquals("i32", NnTensorType.I32.getWasiName(), "I32 wasi name should be 'i32'");
    }

    @Test
    @DisplayName("I64 should have wasi name 'i64'")
    void i64ShouldHaveCorrectWasiName() {
      assertEquals("i64", NnTensorType.I64.getWasiName(), "I64 wasi name should be 'i64'");
    }
  }

  @Nested
  @DisplayName("CalculateByteSize Tests")
  class CalculateByteSizeTests {

    @Test
    @DisplayName("should calculate correct byte size for normal dimensions {2,3}")
    void shouldCalculateCorrectByteSizeForNormalDimensions() {
      assertEquals(
          12L,
          NnTensorType.FP16.calculateByteSize(new int[] {2, 3}),
          "FP16 {2,3} should be 2*2*3=12");
      assertEquals(
          24L,
          NnTensorType.FP32.calculateByteSize(new int[] {2, 3}),
          "FP32 {2,3} should be 4*2*3=24");
      assertEquals(
          48L,
          NnTensorType.FP64.calculateByteSize(new int[] {2, 3}),
          "FP64 {2,3} should be 8*2*3=48");
      assertEquals(
          12L,
          NnTensorType.BF16.calculateByteSize(new int[] {2, 3}),
          "BF16 {2,3} should be 2*2*3=12");
      assertEquals(
          6L,
          NnTensorType.U8.calculateByteSize(new int[] {2, 3}),
          "U8 {2,3} should be 1*2*3=6");
      assertEquals(
          24L,
          NnTensorType.I32.calculateByteSize(new int[] {2, 3}),
          "I32 {2,3} should be 4*2*3=24");
      assertEquals(
          48L,
          NnTensorType.I64.calculateByteSize(new int[] {2, 3}),
          "I64 {2,3} should be 8*2*3=48");
    }

    @Test
    @DisplayName("should return 0 for null dimensions")
    void shouldReturnZeroForNullDimensions() {
      assertEquals(
          0L,
          NnTensorType.FP32.calculateByteSize(null),
          "Should return 0 for null dimensions");
    }

    @Test
    @DisplayName("should return 0 for empty dimensions")
    void shouldReturnZeroForEmptyDimensions() {
      assertEquals(
          0L,
          NnTensorType.FP32.calculateByteSize(new int[] {}),
          "Should return 0 for empty dimensions");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for negative dimension")
    void shouldThrowForNegativeDimension() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> NnTensorType.FP32.calculateByteSize(new int[] {2, -1}),
              "Should throw for negative dimension");
      assertTrue(
          exception.getMessage().contains("-1"),
          "Exception message should mention the negative dimension: " + exception.getMessage());
    }
  }

  @Nested
  @DisplayName("FromWasiName Tests")
  class FromWasiNameTests {

    @Test
    @DisplayName("should resolve valid wasi names to correct constants")
    void shouldResolveValidWasiNames() {
      assertEquals(
          NnTensorType.FP16, NnTensorType.fromWasiName("fp16"), "Should resolve 'fp16'");
      assertEquals(
          NnTensorType.FP32, NnTensorType.fromWasiName("fp32"), "Should resolve 'fp32'");
      assertEquals(
          NnTensorType.FP64, NnTensorType.fromWasiName("fp64"), "Should resolve 'fp64'");
      assertEquals(
          NnTensorType.BF16, NnTensorType.fromWasiName("bf16"), "Should resolve 'bf16'");
      assertEquals(NnTensorType.U8, NnTensorType.fromWasiName("u8"), "Should resolve 'u8'");
      assertEquals(
          NnTensorType.I32, NnTensorType.fromWasiName("i32"), "Should resolve 'i32'");
      assertEquals(
          NnTensorType.I64, NnTensorType.fromWasiName("i64"), "Should resolve 'i64'");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid wasi name")
    void shouldThrowForInvalidWasiName() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> NnTensorType.fromWasiName("f128"),
              "Should throw for invalid wasi name");
      assertTrue(
          exception.getMessage().contains("f128"),
          "Exception message should mention the invalid name: " + exception.getMessage());
    }
  }

  @Nested
  @DisplayName("FromNativeCode Tests")
  class FromNativeCodeTests {

    @Test
    @DisplayName("should resolve valid native codes to correct constants")
    void shouldResolveValidNativeCodes() {
      for (final NnTensorType type : NnTensorType.values()) {
        assertSame(
            type,
            NnTensorType.fromNativeCode(type.getNativeCode()),
            "Should resolve native code " + type.getNativeCode() + " to " + type);
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid native code")
    void shouldThrowForInvalidNativeCode() {
      assertThrows(
          IllegalArgumentException.class,
          () -> NnTensorType.fromNativeCode(-1),
          "Should throw for negative native code");
      assertThrows(
          IllegalArgumentException.class,
          () -> NnTensorType.fromNativeCode(7),
          "Should throw for out-of-range native code 7");
    }
  }
}
