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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link NnTensor} class.
 *
 * <p>Verifies factory methods (fromBytes, fromFloatArray, fromIntArray, fromLongArray,
 * fromByteArray), defensive copies, dimension validation, type conversions, and named tensor
 * support.
 */
@DisplayName("NnTensor Tests")
class NnTensorTest {

  @Nested
  @DisplayName("FromFloatArray Tests")
  class FromFloatArrayTests {

    @Test
    @DisplayName("should create FP32 tensor from float array")
    void shouldCreateFp32TensorFromFloatArray() {
      final int[] dims = {2, 3};
      final float[] data = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f};
      final NnTensor tensor = NnTensor.fromFloatArray(dims, data);

      assertEquals(NnTensorType.FP32, tensor.getType(), "Type should be FP32");
      assertArrayEquals(dims, tensor.getDimensions(), "Dimensions should match");
      assertEquals(6L, tensor.getElementCount(), "Element count should be 6");
      assertEquals(24, tensor.getByteSize(), "Byte size should be 24 (6 * 4)");
      assertFalse(tensor.isNamed(), "Tensor should not be named");
      assertNull(tensor.getName(), "Name should be null");
    }

    @Test
    @DisplayName("should create named FP32 tensor")
    void shouldCreateNamedFp32Tensor() {
      final int[] dims = {1, 3};
      final float[] data = {1.0f, 2.0f, 3.0f};
      final NnTensor tensor = NnTensor.fromFloatArray("input", dims, data);

      assertTrue(tensor.isNamed(), "Tensor should be named");
      assertEquals("input", tensor.getName(), "Name should be 'input'");
    }

    @Test
    @DisplayName("should round-trip float data correctly")
    void shouldRoundTripFloatData() {
      final int[] dims = {3};
      final float[] data = {1.5f, -2.5f, 3.14f};
      final NnTensor tensor = NnTensor.fromFloatArray(dims, data);
      final float[] retrieved = tensor.toFloatArray();
      assertArrayEquals(data, retrieved, 0.0001f, "Float data should round-trip correctly");
    }

    @Test
    @DisplayName("should throw for dimension mismatch")
    void shouldThrowForDimensionMismatch() {
      final int[] dims = {2, 3};
      final float[] data = {1.0f, 2.0f};
      assertThrows(
          IllegalArgumentException.class,
          () -> NnTensor.fromFloatArray(dims, data),
          "Should throw for data size mismatch with dimensions");
    }

    @Test
    @DisplayName("should throw for null dimensions")
    void shouldThrowForNullDimensions() {
      final float[] data = {1.0f};
      assertThrows(
          NullPointerException.class,
          () -> NnTensor.fromFloatArray(null, data),
          "Should throw for null dimensions");
    }

    @Test
    @DisplayName("should throw for null data")
    void shouldThrowForNullData() {
      final int[] dims = {1};
      assertThrows(
          NullPointerException.class,
          () -> NnTensor.fromFloatArray(dims, (float[]) null),
          "Should throw for null data");
    }
  }

  @Nested
  @DisplayName("FromByteArray Tests")
  class FromByteArrayTests {

    @Test
    @DisplayName("should create U8 tensor from byte array")
    void shouldCreateU8TensorFromByteArray() {
      final int[] dims = {2, 2};
      final byte[] data = {1, 2, 3, 4};
      final NnTensor tensor = NnTensor.fromByteArray(dims, data);

      assertEquals(NnTensorType.U8, tensor.getType(), "Type should be U8");
      assertEquals(4L, tensor.getElementCount(), "Element count should be 4");
      assertEquals(4, tensor.getByteSize(), "Byte size should be 4");
    }

    @Test
    @DisplayName("should create named U8 tensor")
    void shouldCreateNamedU8Tensor() {
      final int[] dims = {3};
      final byte[] data = {10, 20, 30};
      final NnTensor tensor = NnTensor.fromByteArray("pixels", dims, data);

      assertTrue(tensor.isNamed(), "Tensor should be named");
      assertEquals("pixels", tensor.getName(), "Name should be 'pixels'");
    }

    @Test
    @DisplayName("should round-trip byte data correctly")
    void shouldRoundTripByteData() {
      final int[] dims = {4};
      final byte[] data = {10, 20, 30, 40};
      final NnTensor tensor = NnTensor.fromByteArray(dims, data);
      final byte[] retrieved = tensor.toByteArray();
      assertArrayEquals(data, retrieved, "Byte data should round-trip correctly");
    }
  }

  @Nested
  @DisplayName("FromIntArray Tests")
  class FromIntArrayTests {

    @Test
    @DisplayName("should create I32 tensor from int array")
    void shouldCreateI32TensorFromIntArray() {
      final int[] dims = {3};
      final int[] data = {100, 200, 300};
      final NnTensor tensor = NnTensor.fromIntArray(dims, data);

      assertEquals(NnTensorType.I32, tensor.getType(), "Type should be I32");
      assertEquals(3L, tensor.getElementCount(), "Element count should be 3");
      assertEquals(12, tensor.getByteSize(), "Byte size should be 12 (3 * 4)");
    }

    @Test
    @DisplayName("should round-trip int data correctly")
    void shouldRoundTripIntData() {
      final int[] dims = {3};
      final int[] data = {-1, 0, Integer.MAX_VALUE};
      final NnTensor tensor = NnTensor.fromIntArray(dims, data);
      final int[] retrieved = tensor.toIntArray();
      assertArrayEquals(data, retrieved, "Int data should round-trip correctly");
    }

    @Test
    @DisplayName("should create named I32 tensor")
    void shouldCreateNamedI32Tensor() {
      final int[] dims = {2};
      final int[] data = {1, 2};
      final NnTensor tensor = NnTensor.fromIntArray("indices", dims, data);
      assertEquals("indices", tensor.getName(), "Name should be 'indices'");
    }
  }

  @Nested
  @DisplayName("FromLongArray Tests")
  class FromLongArrayTests {

    @Test
    @DisplayName("should create I64 tensor from long array")
    void shouldCreateI64TensorFromLongArray() {
      final int[] dims = {2};
      final long[] data = {100L, 200L};
      final NnTensor tensor = NnTensor.fromLongArray(dims, data);

      assertEquals(NnTensorType.I64, tensor.getType(), "Type should be I64");
      assertEquals(2L, tensor.getElementCount(), "Element count should be 2");
      assertEquals(16, tensor.getByteSize(), "Byte size should be 16 (2 * 8)");
    }

    @Test
    @DisplayName("should round-trip long data correctly")
    void shouldRoundTripLongData() {
      final int[] dims = {3};
      final long[] data = {-1L, 0L, Long.MAX_VALUE};
      final NnTensor tensor = NnTensor.fromLongArray(dims, data);
      final long[] retrieved = tensor.toLongArray();
      assertArrayEquals(data, retrieved, "Long data should round-trip correctly");
    }

    @Test
    @DisplayName("should create named I64 tensor")
    void shouldCreateNamedI64Tensor() {
      final int[] dims = {1};
      final long[] data = {42L};
      final NnTensor tensor = NnTensor.fromLongArray("timestamps", dims, data);
      assertEquals("timestamps", tensor.getName(), "Name should be 'timestamps'");
    }
  }

  @Nested
  @DisplayName("FromBytes Tests")
  class FromBytesTests {

    @Test
    @DisplayName("should create tensor from raw bytes")
    void shouldCreateTensorFromRawBytes() {
      final int[] dims = {2};
      final byte[] data = {0, 0, (byte) 0x80, 0x3F, 0, 0, 0, 0x40};
      final NnTensor tensor = NnTensor.fromBytes(dims, NnTensorType.FP32, data);

      assertEquals(NnTensorType.FP32, tensor.getType(), "Type should be FP32");
      assertEquals(2L, tensor.getElementCount(), "Element count should be 2");
      assertEquals(8, tensor.getByteSize(), "Byte size should be 8");
    }

    @Test
    @DisplayName("should throw for data size mismatch with type")
    void shouldThrowForDataSizeMismatch() {
      final int[] dims = {2};
      final byte[] data = {1, 2, 3};
      assertThrows(
          IllegalArgumentException.class,
          () -> NnTensor.fromBytes(dims, NnTensorType.FP32, data),
          "Should throw when data size doesn't match dimensions * type byte size");
    }

    @Test
    @DisplayName("should create named tensor from raw bytes")
    void shouldCreateNamedTensorFromRawBytes() {
      final int[] dims = {1};
      final byte[] data = {42};
      final NnTensor tensor = NnTensor.fromBytes("raw", dims, NnTensorType.U8, data);
      assertEquals("raw", tensor.getName(), "Name should be 'raw'");
    }
  }

  @Nested
  @DisplayName("Dimension Validation Tests")
  class DimensionValidationTests {

    @Test
    @DisplayName("should throw for empty dimensions")
    void shouldThrowForEmptyDimensions() {
      final int[] dims = {};
      final float[] data = {};
      assertThrows(
          IllegalArgumentException.class,
          () -> NnTensor.fromFloatArray(dims, data),
          "Should throw for empty dimensions");
    }

    @Test
    @DisplayName("should throw for negative dimension")
    void shouldThrowForNegativeDimension() {
      final int[] dims = {2, -1};
      final float[] data = {1.0f, 2.0f};
      assertThrows(
          IllegalArgumentException.class,
          () -> NnTensor.fromFloatArray(dims, data),
          "Should throw for negative dimension");
    }

    @Test
    @DisplayName("getRank should return number of dimensions")
    void getRankShouldReturnDimensionCount() {
      final int[] dims = {2, 3, 4};
      final float[] data = new float[24];
      final NnTensor tensor = NnTensor.fromFloatArray(dims, data);
      assertEquals(3, tensor.getRank(), "Rank should be 3 for 3D tensor");
    }
  }

  @Nested
  @DisplayName("Defensive Copy Tests")
  class DefensiveCopyTests {

    @Test
    @DisplayName("should defensively copy dimensions on construction")
    void shouldDefensivelyCopyDimensionsOnConstruction() {
      final int[] dims = {3};
      final float[] data = {1.0f, 2.0f, 3.0f};
      final NnTensor tensor = NnTensor.fromFloatArray(dims, data);

      dims[0] = 99;
      assertArrayEquals(
          new int[] {3},
          tensor.getDimensions(),
          "Modifying original dimensions should not affect tensor");
    }

    @Test
    @DisplayName("should defensively copy dimensions on retrieval")
    void shouldDefensivelyCopyDimensionsOnRetrieval() {
      final int[] dims = {3};
      final float[] data = {1.0f, 2.0f, 3.0f};
      final NnTensor tensor = NnTensor.fromFloatArray(dims, data);

      final int[] retrieved = tensor.getDimensions();
      retrieved[0] = 99;
      assertArrayEquals(
          new int[] {3},
          tensor.getDimensions(),
          "Modifying retrieved dimensions should not affect tensor");
    }

    @Test
    @DisplayName("should defensively copy data on construction")
    void shouldDefensivelyCopyDataOnConstruction() {
      final int[] dims = {3};
      final byte[] data = {1, 2, 3};
      final NnTensor tensor = NnTensor.fromByteArray(dims, data);

      data[0] = 99;
      assertEquals(1, tensor.getData()[0], "Modifying original data should not affect tensor");
    }

    @Test
    @DisplayName("should defensively copy data on retrieval")
    void shouldDefensivelyCopyDataOnRetrieval() {
      final int[] dims = {3};
      final byte[] data = {1, 2, 3};
      final NnTensor tensor = NnTensor.fromByteArray(dims, data);

      final byte[] retrieved = tensor.getData();
      retrieved[0] = 99;
      assertEquals(1, tensor.getData()[0], "Modifying retrieved data should not affect tensor");
    }
  }

  @Nested
  @DisplayName("Type Conversion Error Tests")
  class TypeConversionErrorTests {

    @Test
    @DisplayName("toFloatArray should throw for non-FP32 tensor")
    void toFloatArrayShouldThrowForNonFp32() {
      final int[] dims = {2};
      final byte[] data = {1, 2};
      final NnTensor tensor = NnTensor.fromByteArray(dims, data);
      assertThrows(
          IllegalStateException.class,
          tensor::toFloatArray,
          "Should throw when converting U8 tensor to float array");
    }

    @Test
    @DisplayName("toByteArray should throw for non-U8 tensor")
    void toByteArrayShouldThrowForNonU8() {
      final int[] dims = {1};
      final float[] data = {1.0f};
      final NnTensor tensor = NnTensor.fromFloatArray(dims, data);
      assertThrows(
          IllegalStateException.class,
          tensor::toByteArray,
          "Should throw when converting FP32 tensor to byte array");
    }

    @Test
    @DisplayName("toIntArray should throw for non-I32 tensor")
    void toIntArrayShouldThrowForNonI32() {
      final int[] dims = {1};
      final float[] data = {1.0f};
      final NnTensor tensor = NnTensor.fromFloatArray(dims, data);
      assertThrows(
          IllegalStateException.class,
          tensor::toIntArray,
          "Should throw when converting FP32 tensor to int array");
    }

    @Test
    @DisplayName("toLongArray should throw for non-I64 tensor")
    void toLongArrayShouldThrowForNonI64() {
      final int[] dims = {1};
      final float[] data = {1.0f};
      final NnTensor tensor = NnTensor.fromFloatArray(dims, data);
      assertThrows(
          IllegalStateException.class,
          tensor::toLongArray,
          "Should throw when converting FP32 tensor to long array");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equal tensors should be equal")
    void equalTensorsShouldBeEqual() {
      final int[] dims = {2};
      final float[] data = {1.0f, 2.0f};
      final NnTensor t1 = NnTensor.fromFloatArray(dims, data);
      final NnTensor t2 = NnTensor.fromFloatArray(dims, data);
      assertEquals(t1, t2, "Tensors with same values should be equal");
      assertEquals(
          t1.hashCode(), t2.hashCode(), "Tensors with same values should have same hashCode");
    }

    @Test
    @DisplayName("tensors with different data should not be equal")
    void differentDataShouldNotBeEqual() {
      final int[] dims = {2};
      final NnTensor t1 = NnTensor.fromFloatArray(dims, new float[] {1.0f, 2.0f});
      final NnTensor t2 = NnTensor.fromFloatArray(dims, new float[] {3.0f, 4.0f});
      assertNotEquals(t1, t2, "Tensors with different data should not be equal");
    }

    @Test
    @DisplayName("tensors with different names should not be equal")
    void differentNamesShouldNotBeEqual() {
      final int[] dims = {1};
      final float[] data = {1.0f};
      final NnTensor t1 = NnTensor.fromFloatArray("a", dims, data);
      final NnTensor t2 = NnTensor.fromFloatArray("b", dims, data);
      assertNotEquals(t1, t2, "Tensors with different names should not be equal");
    }

    @Test
    @DisplayName("should not equal null")
    void shouldNotEqualNull() {
      final NnTensor tensor = NnTensor.fromFloatArray(new int[] {1}, new float[] {1.0f});
      assertNotEquals(null, tensor, "Tensor should not equal null");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain type and dimensions")
    void toStringShouldContainTypeAndDimensions() {
      final int[] dims = {2, 3};
      final float[] data = new float[6];
      final NnTensor tensor = NnTensor.fromFloatArray(dims, data);
      final String result = tensor.toString();
      assertTrue(result.contains("FP32"), "toString should contain type: " + result);
      assertTrue(result.contains("[2, 3]"), "toString should contain dimensions: " + result);
    }

    @Test
    @DisplayName("toString should contain name when named")
    void toStringShouldContainNameWhenNamed() {
      final NnTensor tensor =
          NnTensor.fromFloatArray("myTensor", new int[] {1}, new float[] {1.0f});
      final String result = tensor.toString();
      assertTrue(result.contains("myTensor"), "toString should contain name: " + result);
    }

    @Test
    @DisplayName("toString should contain NnTensor")
    void toStringShouldContainClassName() {
      final NnTensor tensor = NnTensor.fromFloatArray(new int[] {1}, new float[] {1.0f});
      final String result = tensor.toString();
      assertNotNull(result, "toString should not be null");
      assertTrue(result.contains("NnTensor"), "toString should contain class name: " + result);
    }
  }
}
