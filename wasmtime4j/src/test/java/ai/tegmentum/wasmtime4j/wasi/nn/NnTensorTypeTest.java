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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NnTensorType} enum.
 *
 * <p>NnTensorType represents data types supported for tensor elements in WASI-NN per the WASI-NN
 * specification.
 */
@DisplayName("NnTensorType Tests")
class NnTensorTypeTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(NnTensorType.class.isEnum(), "NnTensorType should be an enum");
    }

    @Test
    @DisplayName("should have exactly 7 values")
    void shouldHaveExactlySevenValues() {
      final NnTensorType[] values = NnTensorType.values();
      assertEquals(7, values.length, "Should have exactly 7 tensor types");
    }

    @Test
    @DisplayName("should have FP16 value")
    void shouldHaveFp16Value() {
      assertNotNull(NnTensorType.valueOf("FP16"), "Should have FP16");
    }

    @Test
    @DisplayName("should have FP32 value")
    void shouldHaveFp32Value() {
      assertNotNull(NnTensorType.valueOf("FP32"), "Should have FP32");
    }

    @Test
    @DisplayName("should have FP64 value")
    void shouldHaveFp64Value() {
      assertNotNull(NnTensorType.valueOf("FP64"), "Should have FP64");
    }

    @Test
    @DisplayName("should have BF16 value")
    void shouldHaveBf16Value() {
      assertNotNull(NnTensorType.valueOf("BF16"), "Should have BF16");
    }

    @Test
    @DisplayName("should have U8 value")
    void shouldHaveU8Value() {
      assertNotNull(NnTensorType.valueOf("U8"), "Should have U8");
    }

    @Test
    @DisplayName("should have I32 value")
    void shouldHaveI32Value() {
      assertNotNull(NnTensorType.valueOf("I32"), "Should have I32");
    }

    @Test
    @DisplayName("should have I64 value")
    void shouldHaveI64Value() {
      assertNotNull(NnTensorType.valueOf("I64"), "Should have I64");
    }
  }

  @Nested
  @DisplayName("getByteSize Method Tests")
  class GetByteSizeTests {

    @Test
    @DisplayName("should return 2 for FP16")
    void shouldReturn2ForFp16() {
      assertEquals(2, NnTensorType.FP16.getByteSize(), "FP16 should be 2 bytes");
    }

    @Test
    @DisplayName("should return 4 for FP32")
    void shouldReturn4ForFp32() {
      assertEquals(4, NnTensorType.FP32.getByteSize(), "FP32 should be 4 bytes");
    }

    @Test
    @DisplayName("should return 8 for FP64")
    void shouldReturn8ForFp64() {
      assertEquals(8, NnTensorType.FP64.getByteSize(), "FP64 should be 8 bytes");
    }

    @Test
    @DisplayName("should return 2 for BF16")
    void shouldReturn2ForBf16() {
      assertEquals(2, NnTensorType.BF16.getByteSize(), "BF16 should be 2 bytes");
    }

    @Test
    @DisplayName("should return 1 for U8")
    void shouldReturn1ForU8() {
      assertEquals(1, NnTensorType.U8.getByteSize(), "U8 should be 1 byte");
    }

    @Test
    @DisplayName("should return 4 for I32")
    void shouldReturn4ForI32() {
      assertEquals(4, NnTensorType.I32.getByteSize(), "I32 should be 4 bytes");
    }

    @Test
    @DisplayName("should return 8 for I64")
    void shouldReturn8ForI64() {
      assertEquals(8, NnTensorType.I64.getByteSize(), "I64 should be 8 bytes");
    }
  }

  @Nested
  @DisplayName("getWasiName Method Tests")
  class GetWasiNameTests {

    @Test
    @DisplayName("should return fp16 for FP16")
    void shouldReturnFp16ForFp16() {
      assertEquals("fp16", NnTensorType.FP16.getWasiName(), "FP16 WASI name should be fp16");
    }

    @Test
    @DisplayName("should return fp32 for FP32")
    void shouldReturnFp32ForFp32() {
      assertEquals("fp32", NnTensorType.FP32.getWasiName(), "FP32 WASI name should be fp32");
    }

    @Test
    @DisplayName("should return fp64 for FP64")
    void shouldReturnFp64ForFp64() {
      assertEquals("fp64", NnTensorType.FP64.getWasiName(), "FP64 WASI name should be fp64");
    }

    @Test
    @DisplayName("should return bf16 for BF16")
    void shouldReturnBf16ForBf16() {
      assertEquals("bf16", NnTensorType.BF16.getWasiName(), "BF16 WASI name should be bf16");
    }

    @Test
    @DisplayName("should return u8 for U8")
    void shouldReturnU8ForU8() {
      assertEquals("u8", NnTensorType.U8.getWasiName(), "U8 WASI name should be u8");
    }

    @Test
    @DisplayName("should return i32 for I32")
    void shouldReturnI32ForI32() {
      assertEquals("i32", NnTensorType.I32.getWasiName(), "I32 WASI name should be i32");
    }

    @Test
    @DisplayName("should return i64 for I64")
    void shouldReturnI64ForI64() {
      assertEquals("i64", NnTensorType.I64.getWasiName(), "I64 WASI name should be i64");
    }
  }

  @Nested
  @DisplayName("calculateByteSize Method Tests")
  class CalculateByteSizeTests {

    @Test
    @DisplayName("should return 0 for null dimensions")
    void shouldReturn0ForNullDimensions() {
      assertEquals(0, NnTensorType.FP32.calculateByteSize(null), "Null dimensions should return 0");
    }

    @Test
    @DisplayName("should return 0 for empty dimensions")
    void shouldReturn0ForEmptyDimensions() {
      assertEquals(
          0, NnTensorType.FP32.calculateByteSize(new int[0]), "Empty dimensions should return 0");
    }

    @Test
    @DisplayName("should calculate 1D tensor size correctly")
    void shouldCalculate1DTensorSizeCorrectly() {
      // 10 elements * 4 bytes per FP32 = 40 bytes
      assertEquals(40, NnTensorType.FP32.calculateByteSize(new int[] {10}), "1D tensor size");
    }

    @Test
    @DisplayName("should calculate 2D tensor size correctly")
    void shouldCalculate2DTensorSizeCorrectly() {
      // 3 * 4 = 12 elements * 4 bytes per FP32 = 48 bytes
      assertEquals(48, NnTensorType.FP32.calculateByteSize(new int[] {3, 4}), "2D tensor size");
    }

    @Test
    @DisplayName("should calculate 3D tensor size correctly")
    void shouldCalculate3DTensorSizeCorrectly() {
      // 2 * 3 * 4 = 24 elements * 4 bytes per FP32 = 96 bytes
      assertEquals(96, NnTensorType.FP32.calculateByteSize(new int[] {2, 3, 4}), "3D tensor size");
    }

    @Test
    @DisplayName("should calculate 4D tensor size correctly for U8")
    void shouldCalculate4DTensorSizeCorrectlyForU8() {
      // Typical image batch: 32 * 224 * 224 * 3 = 4,816,896 elements * 1 byte per U8
      assertEquals(
          4816896L,
          NnTensorType.U8.calculateByteSize(new int[] {32, 224, 224, 3}),
          "4D image batch tensor size");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for negative dimension")
    void shouldThrowIllegalArgumentExceptionForNegativeDimension() {
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> NnTensorType.FP32.calculateByteSize(new int[] {10, -1, 5}));

      assertTrue(ex.getMessage().contains("negative"), "Exception should mention negative");
    }

    @Test
    @DisplayName("should handle dimension with zero value")
    void shouldHandleDimensionWithZeroValue() {
      // 10 * 0 * 5 = 0 elements
      assertEquals(
          0, NnTensorType.FP32.calculateByteSize(new int[] {10, 0, 5}), "Zero dimension = 0 size");
    }

    @Test
    @DisplayName("should calculate correctly for different tensor types")
    void shouldCalculateCorrectlyForDifferentTensorTypes() {
      final int[] dims = {100};

      assertEquals(100, NnTensorType.U8.calculateByteSize(dims), "U8: 100 bytes");
      assertEquals(200, NnTensorType.FP16.calculateByteSize(dims), "FP16: 200 bytes");
      assertEquals(200, NnTensorType.BF16.calculateByteSize(dims), "BF16: 200 bytes");
      assertEquals(400, NnTensorType.FP32.calculateByteSize(dims), "FP32: 400 bytes");
      assertEquals(400, NnTensorType.I32.calculateByteSize(dims), "I32: 400 bytes");
      assertEquals(800, NnTensorType.FP64.calculateByteSize(dims), "FP64: 800 bytes");
      assertEquals(800, NnTensorType.I64.calculateByteSize(dims), "I64: 800 bytes");
    }
  }

  @Nested
  @DisplayName("fromWasiName Method Tests")
  class FromWasiNameTests {

    @Test
    @DisplayName("should parse fp16")
    void shouldParseFp16() {
      assertEquals(NnTensorType.FP16, NnTensorType.fromWasiName("fp16"), "Should parse fp16");
    }

    @Test
    @DisplayName("should parse fp32")
    void shouldParseFp32() {
      assertEquals(NnTensorType.FP32, NnTensorType.fromWasiName("fp32"), "Should parse fp32");
    }

    @Test
    @DisplayName("should parse fp64")
    void shouldParseFp64() {
      assertEquals(NnTensorType.FP64, NnTensorType.fromWasiName("fp64"), "Should parse fp64");
    }

    @Test
    @DisplayName("should parse bf16")
    void shouldParseBf16() {
      assertEquals(NnTensorType.BF16, NnTensorType.fromWasiName("bf16"), "Should parse bf16");
    }

    @Test
    @DisplayName("should parse u8")
    void shouldParseU8() {
      assertEquals(NnTensorType.U8, NnTensorType.fromWasiName("u8"), "Should parse u8");
    }

    @Test
    @DisplayName("should parse i32")
    void shouldParseI32() {
      assertEquals(NnTensorType.I32, NnTensorType.fromWasiName("i32"), "Should parse i32");
    }

    @Test
    @DisplayName("should parse i64")
    void shouldParseI64() {
      assertEquals(NnTensorType.I64, NnTensorType.fromWasiName("i64"), "Should parse i64");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for unknown name")
    void shouldThrowIllegalArgumentExceptionForUnknownName() {
      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> NnTensorType.fromWasiName("float32"));

      assertTrue(ex.getMessage().contains("Unknown"), "Exception should mention Unknown");
    }

    @Test
    @DisplayName("should be case sensitive")
    void shouldBeCaseSensitive() {
      assertThrows(
          IllegalArgumentException.class,
          () -> NnTensorType.fromWasiName("FP32"),
          "Should be case sensitive");
    }
  }

  @Nested
  @DisplayName("getNativeCode Method Tests")
  class GetNativeCodeTests {

    @Test
    @DisplayName("should return ordinal as native code")
    void shouldReturnOrdinalAsNativeCode() {
      for (final NnTensorType type : NnTensorType.values()) {
        assertEquals(
            type.ordinal(), type.getNativeCode(), type.name() + " native code should be ordinal");
      }
    }

    @Test
    @DisplayName("should have unique native codes")
    void shouldHaveUniqueNativeCodes() {
      final Set<Integer> codes = new HashSet<>();
      for (final NnTensorType type : NnTensorType.values()) {
        assertTrue(codes.add(type.getNativeCode()), "Native code should be unique: " + type);
      }
    }
  }

  @Nested
  @DisplayName("fromNativeCode Method Tests")
  class FromNativeCodeTests {

    @Test
    @DisplayName("should parse all valid codes")
    void shouldParseAllValidCodes() {
      for (final NnTensorType expected : NnTensorType.values()) {
        final NnTensorType actual = NnTensorType.fromNativeCode(expected.ordinal());
        assertEquals(expected, actual, "Should parse code " + expected.ordinal());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for negative code")
    void shouldThrowIllegalArgumentExceptionForNegativeCode() {
      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> NnTensorType.fromNativeCode(-1));

      assertTrue(ex.getMessage().contains("Invalid"), "Exception should mention Invalid");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for code out of range")
    void shouldThrowIllegalArgumentExceptionForCodeOutOfRange() {
      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> NnTensorType.fromNativeCode(100));

      assertTrue(ex.getMessage().contains("Invalid"), "Exception should mention Invalid");
    }

    @Test
    @DisplayName("should round trip from type to code and back")
    void shouldRoundTripFromTypeToCodeAndBack() {
      for (final NnTensorType original : NnTensorType.values()) {
        final int code = original.getNativeCode();
        final NnTensorType roundTripped = NnTensorType.fromNativeCode(code);
        assertEquals(original, roundTripped, "Should round trip: " + original);
      }
    }
  }

  @Nested
  @DisplayName("WASI-NN Specification Compliance Tests")
  class WasiNnSpecificationComplianceTests {

    @Test
    @DisplayName("should cover all WASI-NN tensor types")
    void shouldCoverAllWasiNnTensorTypes() {
      // Per WASI-NN specification: tensor_type enum
      final String[] expectedTypes = {"fp16", "fp32", "fp64", "bf16", "u8", "i32", "i64"};

      for (final String expectedName : expectedTypes) {
        assertNotNull(NnTensorType.fromWasiName(expectedName), "Should have type: " + expectedName);
      }

      assertEquals(
          expectedTypes.length, NnTensorType.values().length, "Should have exact count of types");
    }

    @Test
    @DisplayName("should have correct byte sizes per ML conventions")
    void shouldHaveCorrectByteSizesPerMlConventions() {
      // Standard ML framework conventions for data type sizes
      assertEquals(2, NnTensorType.FP16.getByteSize(), "FP16 is 2 bytes (IEEE half)");
      assertEquals(4, NnTensorType.FP32.getByteSize(), "FP32 is 4 bytes (IEEE single)");
      assertEquals(8, NnTensorType.FP64.getByteSize(), "FP64 is 8 bytes (IEEE double)");
      assertEquals(2, NnTensorType.BF16.getByteSize(), "BF16 is 2 bytes (bfloat16)");
      assertEquals(1, NnTensorType.U8.getByteSize(), "U8 is 1 byte");
      assertEquals(4, NnTensorType.I32.getByteSize(), "I32 is 4 bytes");
      assertEquals(8, NnTensorType.I64.getByteSize(), "I64 is 8 bytes");
    }
  }

  @Nested
  @DisplayName("Usage Pattern Tests")
  class UsagePatternTests {

    @Test
    @DisplayName("should support tensor memory allocation calculation")
    void shouldSupportTensorMemoryAllocationCalculation() {
      // Typical BERT input: batch=8, sequence=512, hidden=768
      final int[] bertDimensions = {8, 512, 768};
      final long fp32Size = NnTensorType.FP32.calculateByteSize(bertDimensions);
      final long fp16Size = NnTensorType.FP16.calculateByteSize(bertDimensions);

      assertEquals(12582912, fp32Size, "BERT FP32 size");
      assertEquals(6291456, fp16Size, "BERT FP16 size (half of FP32)");
      assertEquals(fp32Size, fp16Size * 2, "FP32 should be double FP16");
    }

    @Test
    @DisplayName("should support quantization comparison")
    void shouldSupportQuantizationComparison() {
      final int[] dims = {1, 1000};

      final long fp32Size = NnTensorType.FP32.calculateByteSize(dims);
      final long u8Size = NnTensorType.U8.calculateByteSize(dims);

      assertEquals(4000, fp32Size, "FP32 model size");
      assertEquals(1000, u8Size, "Quantized U8 model size");
      assertEquals(4, fp32Size / u8Size, "Quantization saves 4x memory");
    }
  }
}
