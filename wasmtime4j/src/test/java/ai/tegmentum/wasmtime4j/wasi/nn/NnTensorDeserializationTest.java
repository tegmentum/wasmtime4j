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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NnTensor#deserializeFromNative(String, byte[])}.
 *
 * <p>Verifies that the native FFI serialization format {@code
 * [num_dims:i32LE][dim0:i32LE]...[dimN:i32LE][tensor_type:i32LE][data bytes]} is correctly parsed
 * for all tensor types, edge cases, and error conditions.
 */
@DisplayName("NnTensor Deserialization Tests")
class NnTensorDeserializationTest {

  // Native codes match ordinals: FP16=0, FP32=1, FP64=2, BF16=3, U8=4, I32=5, I64=6
  private static final int FP32_CODE = NnTensorType.FP32.getNativeCode();
  private static final int U8_CODE = NnTensorType.U8.getNativeCode();
  private static final int I32_CODE = NnTensorType.I32.getNativeCode();
  private static final int I64_CODE = NnTensorType.I64.getNativeCode();
  private static final int FP16_CODE = NnTensorType.FP16.getNativeCode();
  private static final int FP64_CODE = NnTensorType.FP64.getNativeCode();
  private static final int BF16_CODE = NnTensorType.BF16.getNativeCode();

  /**
   * Builds the native serialization format from components.
   *
   * @param dims tensor dimensions
   * @param typeCode native tensor type code
   * @param data raw tensor data bytes
   * @return serialized byte array
   */
  private static byte[] buildSerialized(final int[] dims, final int typeCode, final byte[] data) {
    // Header: 4 (numDims) + dims.length*4 (each dim) + 4 (type code)
    final int headerSize = 4 + (dims.length * 4) + 4;
    final ByteBuffer buf = ByteBuffer.allocate(headerSize + data.length);
    buf.order(ByteOrder.LITTLE_ENDIAN);
    buf.putInt(dims.length);
    for (final int dim : dims) {
      buf.putInt(dim);
    }
    buf.putInt(typeCode);
    buf.put(data);
    return buf.array();
  }

  /**
   * Converts float array to little-endian bytes.
   *
   * @param values the float values
   * @return the byte representation
   */
  private static byte[] floatsToBytes(final float... values) {
    final ByteBuffer buf = ByteBuffer.allocate(values.length * 4).order(ByteOrder.LITTLE_ENDIAN);
    for (final float v : values) {
      buf.putFloat(v);
    }
    return buf.array();
  }

  /**
   * Converts int array to little-endian bytes.
   *
   * @param values the int values
   * @return the byte representation
   */
  private static byte[] intsToBytes(final int... values) {
    final ByteBuffer buf = ByteBuffer.allocate(values.length * 4).order(ByteOrder.LITTLE_ENDIAN);
    for (final int v : values) {
      buf.putInt(v);
    }
    return buf.array();
  }

  /**
   * Converts long array to little-endian bytes.
   *
   * @param values the long values
   * @return the byte representation
   */
  private static byte[] longsToBytes(final long... values) {
    final ByteBuffer buf = ByteBuffer.allocate(values.length * 8).order(ByteOrder.LITTLE_ENDIAN);
    for (final long v : values) {
      buf.putLong(v);
    }
    return buf.array();
  }

  @Nested
  @DisplayName("Valid FP32 Deserialization")
  class ValidFp32Tests {

    @Test
    @DisplayName("should deserialize 1D FP32 tensor")
    void shouldDeserialize1dFp32Tensor() throws NnException {
      final float[] expected = {1.0f, 2.0f, 3.0f};
      final byte[] serialized = buildSerialized(new int[] {3}, FP32_CODE, floatsToBytes(expected));

      final NnTensor tensor = NnTensor.deserializeFromNative(null, serialized);

      assertNotNull(tensor, "Deserialized tensor should not be null");
      assertEquals(NnTensorType.FP32, tensor.getType(), "Type should be FP32");
      assertArrayEquals(new int[] {3}, tensor.getDimensions(), "Dimensions should be [3]");
      assertEquals(1, tensor.getRank(), "Rank should be 1");
      assertEquals(3L, tensor.getElementCount(), "Element count should be 3");
      assertArrayEquals(expected, tensor.toFloatArray(), 0.0001f, "Float data should match");
      assertFalse(tensor.isNamed(), "Tensor should not be named");
      assertNull(tensor.getName(), "Name should be null");
    }

    @Test
    @DisplayName("should deserialize 2D FP32 tensor")
    void shouldDeserialize2dFp32Tensor() throws NnException {
      final int[] dims = {2, 3};
      final float[] expected = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f};
      final byte[] serialized = buildSerialized(dims, FP32_CODE, floatsToBytes(expected));

      final NnTensor tensor = NnTensor.deserializeFromNative(null, serialized);

      assertEquals(NnTensorType.FP32, tensor.getType(), "Type should be FP32");
      assertArrayEquals(dims, tensor.getDimensions(), "Dimensions should be [2, 3]");
      assertEquals(2, tensor.getRank(), "Rank should be 2");
      assertEquals(6L, tensor.getElementCount(), "Element count should be 6");
      assertArrayEquals(expected, tensor.toFloatArray(), 0.0001f, "Float data should match");
    }

    @Test
    @DisplayName("should deserialize 3D FP32 tensor")
    void shouldDeserialize3dFp32Tensor() throws NnException {
      final int[] dims = {2, 3, 4};
      final float[] expected = new float[24];
      for (int i = 0; i < expected.length; i++) {
        expected[i] = i * 0.5f;
      }
      final byte[] serialized = buildSerialized(dims, FP32_CODE, floatsToBytes(expected));

      final NnTensor tensor = NnTensor.deserializeFromNative(null, serialized);

      assertArrayEquals(dims, tensor.getDimensions(), "Dimensions should be [2, 3, 4]");
      assertEquals(3, tensor.getRank(), "Rank should be 3");
      assertEquals(24L, tensor.getElementCount(), "Element count should be 24");
      assertArrayEquals(expected, tensor.toFloatArray(), 0.0001f, "Float data should match");
    }
  }

  @Nested
  @DisplayName("Valid U8 Deserialization")
  class ValidU8Tests {

    @Test
    @DisplayName("should deserialize U8 tensor")
    void shouldDeserializeU8Tensor() throws NnException {
      final byte[] expected = {0, 1, 2, 3, 127, (byte) 255};
      final byte[] serialized = buildSerialized(new int[] {6}, U8_CODE, expected);

      final NnTensor tensor = NnTensor.deserializeFromNative(null, serialized);

      assertEquals(NnTensorType.U8, tensor.getType(), "Type should be U8");
      assertArrayEquals(new int[] {6}, tensor.getDimensions(), "Dimensions should be [6]");
      assertArrayEquals(expected, tensor.toByteArray(), "Byte data should match");
    }

    @Test
    @DisplayName("should deserialize 2D U8 tensor")
    void shouldDeserialize2dU8Tensor() throws NnException {
      final int[] dims = {2, 3};
      final byte[] expected = {10, 20, 30, 40, 50, 60};
      final byte[] serialized = buildSerialized(dims, U8_CODE, expected);

      final NnTensor tensor = NnTensor.deserializeFromNative(null, serialized);

      assertEquals(NnTensorType.U8, tensor.getType(), "Type should be U8");
      assertArrayEquals(dims, tensor.getDimensions(), "Dimensions should match");
      assertEquals(6L, tensor.getElementCount(), "Element count should be 6");
      assertArrayEquals(expected, tensor.toByteArray(), "Byte data should match");
    }
  }

  @Nested
  @DisplayName("Valid I32 Deserialization")
  class ValidI32Tests {

    @Test
    @DisplayName("should deserialize I32 tensor")
    void shouldDeserializeI32Tensor() throws NnException {
      final int[] expected = {-100, 0, 42, Integer.MAX_VALUE};
      final byte[] serialized = buildSerialized(new int[] {4}, I32_CODE, intsToBytes(expected));

      final NnTensor tensor = NnTensor.deserializeFromNative(null, serialized);

      assertEquals(NnTensorType.I32, tensor.getType(), "Type should be I32");
      assertArrayEquals(new int[] {4}, tensor.getDimensions(), "Dimensions should be [4]");
      assertArrayEquals(expected, tensor.toIntArray(), "Int data should match");
    }
  }

  @Nested
  @DisplayName("Valid I64 Deserialization")
  class ValidI64Tests {

    @Test
    @DisplayName("should deserialize I64 tensor")
    void shouldDeserializeI64Tensor() throws NnException {
      final long[] expected = {-1L, 0L, Long.MAX_VALUE};
      final byte[] serialized = buildSerialized(new int[] {3}, I64_CODE, longsToBytes(expected));

      final NnTensor tensor = NnTensor.deserializeFromNative(null, serialized);

      assertEquals(NnTensorType.I64, tensor.getType(), "Type should be I64");
      assertArrayEquals(new int[] {3}, tensor.getDimensions(), "Dimensions should be [3]");
      assertArrayEquals(expected, tensor.toLongArray(), "Long data should match");
    }
  }

  @Nested
  @DisplayName("Other Type Deserialization")
  class OtherTypeTests {

    @Test
    @DisplayName("should deserialize FP16 tensor")
    void shouldDeserializeFp16Tensor() throws NnException {
      // FP16: 2 bytes per element
      final byte[] data = new byte[6]; // 3 elements * 2 bytes
      final byte[] serialized = buildSerialized(new int[] {3}, FP16_CODE, data);

      final NnTensor tensor = NnTensor.deserializeFromNative(null, serialized);

      assertEquals(NnTensorType.FP16, tensor.getType(), "Type should be FP16");
      assertEquals(3L, tensor.getElementCount(), "Element count should be 3");
      assertEquals(6, tensor.getByteSize(), "Byte size should be 6");
    }

    @Test
    @DisplayName("should deserialize FP64 tensor")
    void shouldDeserializeFp64Tensor() throws NnException {
      // FP64: 8 bytes per element
      final byte[] data = new byte[16]; // 2 elements * 8 bytes
      final byte[] serialized = buildSerialized(new int[] {2}, FP64_CODE, data);

      final NnTensor tensor = NnTensor.deserializeFromNative(null, serialized);

      assertEquals(NnTensorType.FP64, tensor.getType(), "Type should be FP64");
      assertEquals(2L, tensor.getElementCount(), "Element count should be 2");
      assertEquals(16, tensor.getByteSize(), "Byte size should be 16");
    }

    @Test
    @DisplayName("should deserialize BF16 tensor")
    void shouldDeserializeBf16Tensor() throws NnException {
      // BF16: 2 bytes per element
      final byte[] data = new byte[8]; // 4 elements * 2 bytes
      final byte[] serialized = buildSerialized(new int[] {4}, BF16_CODE, data);

      final NnTensor tensor = NnTensor.deserializeFromNative(null, serialized);

      assertEquals(NnTensorType.BF16, tensor.getType(), "Type should be BF16");
      assertEquals(4L, tensor.getElementCount(), "Element count should be 4");
      assertEquals(8, tensor.getByteSize(), "Byte size should be 8");
    }
  }

  @Nested
  @DisplayName("Named Tensor Deserialization")
  class NamedTensorTests {

    @Test
    @DisplayName("should deserialize with name")
    void shouldDeserializeWithName() throws NnException {
      final byte[] serialized = buildSerialized(new int[] {2}, U8_CODE, new byte[] {1, 2});

      final NnTensor tensor = NnTensor.deserializeFromNative("output_0", serialized);

      assertTrue(tensor.isNamed(), "Tensor should be named");
      assertEquals("output_0", tensor.getName(), "Name should be 'output_0'");
    }

    @Test
    @DisplayName("should deserialize with null name")
    void shouldDeserializeWithNullName() throws NnException {
      final byte[] serialized = buildSerialized(new int[] {2}, U8_CODE, new byte[] {1, 2});

      final NnTensor tensor = NnTensor.deserializeFromNative(null, serialized);

      assertFalse(tensor.isNamed(), "Tensor should not be named");
      assertNull(tensor.getName(), "Name should be null");
    }
  }

  @Nested
  @DisplayName("Round-Trip Tests")
  class RoundTripTests {

    @Test
    @DisplayName("FP32 round-trip: create -> serialize manually -> deserialize -> verify")
    void shouldRoundTripFp32() throws NnException {
      final int[] dims = {1, 3, 224, 224};
      final float[] data = new float[1 * 3 * 224 * 224];
      for (int i = 0; i < data.length; i++) {
        data[i] = (float) (Math.random() * 2 - 1);
      }
      final NnTensor original = NnTensor.fromFloatArray("input", dims, data);
      final byte[] serialized =
          buildSerialized(dims, FP32_CODE, floatsToBytes(original.toFloatArray()));

      final NnTensor deserialized = NnTensor.deserializeFromNative("input", serialized);

      assertEquals(original.getType(), deserialized.getType(), "Type should match");
      assertArrayEquals(
          original.getDimensions(), deserialized.getDimensions(), "Dimensions should match");
      assertEquals(original.getName(), deserialized.getName(), "Name should match");
      assertArrayEquals(
          original.toFloatArray(), deserialized.toFloatArray(), 0.0001f, "Float data should match");
      assertEquals(original, deserialized, "Tensors should be equal");
    }

    @Test
    @DisplayName("I32 round-trip: create -> serialize manually -> deserialize -> verify")
    void shouldRoundTripI32() throws NnException {
      final int[] dims = {3, 5};
      final int[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
      final NnTensor original = NnTensor.fromIntArray(dims, data);
      final byte[] serialized = buildSerialized(dims, I32_CODE, intsToBytes(original.toIntArray()));

      final NnTensor deserialized = NnTensor.deserializeFromNative(null, serialized);

      assertEquals(original, deserialized, "Tensors should be equal after round-trip");
    }

    @Test
    @DisplayName("I64 round-trip: create -> serialize manually -> deserialize -> verify")
    void shouldRoundTripI64() throws NnException {
      final int[] dims = {2, 2};
      final long[] data = {Long.MIN_VALUE, -1L, 0L, Long.MAX_VALUE};
      final NnTensor original = NnTensor.fromLongArray(dims, data);
      final byte[] serialized =
          buildSerialized(dims, I64_CODE, longsToBytes(original.toLongArray()));

      final NnTensor deserialized = NnTensor.deserializeFromNative(null, serialized);

      assertEquals(original, deserialized, "Tensors should be equal after round-trip");
    }

    @Test
    @DisplayName("U8 round-trip: create -> serialize manually -> deserialize -> verify")
    void shouldRoundTripU8() throws NnException {
      final int[] dims = {4, 4};
      final byte[] data = new byte[16];
      for (int i = 0; i < data.length; i++) {
        data[i] = (byte) (i * 17);
      }
      final NnTensor original = NnTensor.fromByteArray(dims, data);
      final byte[] serialized = buildSerialized(dims, U8_CODE, original.toByteArray());

      final NnTensor deserialized = NnTensor.deserializeFromNative(null, serialized);

      assertEquals(original, deserialized, "Tensors should be equal after round-trip");
    }
  }

  @Nested
  @DisplayName("Error Handling")
  class ErrorHandlingTests {

    @Test
    @DisplayName("should throw on null input")
    void shouldThrowOnNullInput() {
      final NnException ex =
          assertThrows(
              NnException.class,
              () -> NnTensor.deserializeFromNative(null, null),
              "Null input should throw NnException");
      assertTrue(
          ex.getMessage().contains("too short"), "Message should mention data being too short");
    }

    @Test
    @DisplayName("should throw on empty input")
    void shouldThrowOnEmptyInput() {
      assertThrows(
          NnException.class,
          () -> NnTensor.deserializeFromNative(null, new byte[0]),
          "Empty input should throw NnException");
    }

    @Test
    @DisplayName("should throw on input shorter than minimum header (< 8 bytes)")
    void shouldThrowOnInputShorterThanMinHeader() {
      assertThrows(
          NnException.class,
          () -> NnTensor.deserializeFromNative(null, new byte[7]),
          "7-byte input should throw NnException (need at least 8 for numDims + typeCode)");
    }

    @Test
    @DisplayName("should throw on negative numDims")
    void shouldThrowOnNegativeNumDims() {
      final ByteBuffer buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
      buf.putInt(-1); // numDims = -1
      buf.putInt(FP32_CODE); // typeCode

      final NnException ex =
          assertThrows(
              NnException.class,
              () -> NnTensor.deserializeFromNative(null, buf.array()),
              "Negative numDims should throw NnException");
      assertTrue(
          ex.getMessage().contains("Invalid number of dimensions"),
          "Message should mention invalid dimensions, got: " + ex.getMessage());
    }

    @Test
    @DisplayName("should throw on numDims > 16")
    void shouldThrowOnExcessiveNumDims() {
      final ByteBuffer buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
      buf.putInt(17); // numDims = 17 (exceeds limit)
      buf.putInt(FP32_CODE);

      final NnException ex =
          assertThrows(
              NnException.class,
              () -> NnTensor.deserializeFromNative(null, buf.array()),
              "numDims > 16 should throw NnException");
      assertTrue(
          ex.getMessage().contains("Invalid number of dimensions"),
          "Message should mention invalid dimensions, got: " + ex.getMessage());
    }

    @Test
    @DisplayName("should throw when header is too short for declared numDims")
    void shouldThrowWhenHeaderTooShortForDims() {
      // Claim 3 dimensions but only provide enough bytes for 1
      final ByteBuffer buf = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
      buf.putInt(3); // numDims = 3 (needs 4 + 3*4 + 4 = 20 bytes total header)
      buf.putInt(2); // dim0
      // Missing dim1, dim2, and typeCode

      assertThrows(
          NnException.class,
          () -> NnTensor.deserializeFromNative(null, buf.array()),
          "Header too short for declared dimensions should throw NnException");
    }

    @Test
    @DisplayName("should throw on invalid tensor type code")
    void shouldThrowOnInvalidTypeCode() {
      final ByteBuffer buf = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
      buf.putInt(1); // numDims = 1
      buf.putInt(2); // dim0 = 2
      buf.putInt(99); // invalid type code

      final NnException ex =
          assertThrows(
              NnException.class,
              () -> NnTensor.deserializeFromNative(null, buf.array()),
              "Invalid type code should throw NnException");
      assertTrue(
          ex.getMessage().contains("Invalid tensor type code"),
          "Message should mention invalid type code, got: " + ex.getMessage());
    }

    @Test
    @DisplayName("should throw when data size doesn't match dimensions and type")
    void shouldThrowOnDataSizeMismatch() {
      // Declare [2,3] FP32 = 24 bytes expected, but provide only 8 bytes of data
      final byte[] tooShortData = new byte[8];
      final byte[] serialized = buildSerialized(new int[] {2, 3}, FP32_CODE, tooShortData);

      // fromBytes validates size, so this should throw via IllegalArgumentException wrapped in
      // NnException or directly as IllegalArgumentException
      assertThrows(
          Exception.class,
          () -> NnTensor.deserializeFromNative(null, serialized),
          "Data size mismatch should throw");
    }

    @Test
    @DisplayName("should accept exactly 8 bytes for minimum valid tensor (0 dims, type, no data)")
    void shouldAcceptMinimumValidTensor() throws NnException {
      // 0 dimensions, U8 type, 0 bytes of data
      // This is actually an edge case: fromBytes will be called with dims=[], which is invalid
      // The method should throw because dimensions cannot be empty
      final ByteBuffer buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
      buf.putInt(0); // numDims = 0
      buf.putInt(U8_CODE); // type

      // 0 dimensions is invalid for NnTensor.fromBytes (empty dimensions)
      assertThrows(
          Exception.class,
          () -> NnTensor.deserializeFromNative(null, buf.array()),
          "Zero dimensions should throw (dimensions array cannot be empty)");
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCaseTests {

    @Test
    @DisplayName("should deserialize single-element tensor")
    void shouldDeserializeSingleElementTensor() throws NnException {
      final byte[] serialized = buildSerialized(new int[] {1}, FP32_CODE, floatsToBytes(42.0f));

      final NnTensor tensor = NnTensor.deserializeFromNative(null, serialized);

      assertEquals(1L, tensor.getElementCount(), "Element count should be 1");
      assertArrayEquals(
          new float[] {42.0f}, tensor.toFloatArray(), 0.0001f, "Should contain single float");
    }

    @Test
    @DisplayName("should deserialize tensor with special float values")
    void shouldDeserializeTensorWithSpecialFloats() throws NnException {
      final float[] data = {
        Float.NaN,
        Float.POSITIVE_INFINITY,
        Float.NEGATIVE_INFINITY,
        0.0f,
        -0.0f,
        Float.MIN_VALUE,
        Float.MAX_VALUE
      };
      final byte[] serialized = buildSerialized(new int[] {7}, FP32_CODE, floatsToBytes(data));

      final NnTensor tensor = NnTensor.deserializeFromNative(null, serialized);

      final float[] result = tensor.toFloatArray();
      assertTrue(Float.isNaN(result[0]), "First element should be NaN");
      assertEquals(
          Float.POSITIVE_INFINITY, result[1], "Second element should be positive infinity");
      assertEquals(Float.NEGATIVE_INFINITY, result[2], "Third element should be negative infinity");
      assertEquals(0.0f, result[3], "Fourth element should be zero");
      assertEquals(Float.MIN_VALUE, result[5], "Sixth element should be Float.MIN_VALUE");
      assertEquals(Float.MAX_VALUE, result[6], "Seventh element should be Float.MAX_VALUE");
    }

    @Test
    @DisplayName("should handle maximum allowed dimensions (16)")
    void shouldHandle16Dimensions() throws NnException {
      // 16 dimensions, each size 1, U8 type, 1 byte of data
      final int[] dims = new int[16];
      for (int i = 0; i < 16; i++) {
        dims[i] = 1;
      }
      final byte[] serialized = buildSerialized(dims, U8_CODE, new byte[] {42});

      final NnTensor tensor = NnTensor.deserializeFromNative(null, serialized);

      assertEquals(16, tensor.getRank(), "Rank should be 16");
      assertEquals(1L, tensor.getElementCount(), "Element count should be 1");
    }

    @Test
    @DisplayName("should preserve data exactly through deserialization")
    void shouldPreserveDataExactly() throws NnException {
      // Use known byte patterns to verify no byte-swapping issues
      final byte[] data = new byte[256];
      for (int i = 0; i < 256; i++) {
        data[i] = (byte) i;
      }
      final byte[] serialized = buildSerialized(new int[] {256}, U8_CODE, data);

      final NnTensor tensor = NnTensor.deserializeFromNative(null, serialized);

      assertArrayEquals(data, tensor.toByteArray(), "All 256 byte values should be preserved");
    }
  }
}
