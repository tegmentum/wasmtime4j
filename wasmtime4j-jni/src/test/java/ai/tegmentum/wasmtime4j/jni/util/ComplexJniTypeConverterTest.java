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

package ai.tegmentum.wasmtime4j.jni.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.MarshalingConfiguration;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link ComplexJniTypeConverter}.
 */
@DisplayName("ComplexJniTypeConverter Tests")
class ComplexJniTypeConverterTest {

  private ComplexJniTypeConverter converter;

  @BeforeEach
  void setUp() {
    converter = new ComplexJniTypeConverter();
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("ComplexJniTypeConverter should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(ComplexJniTypeConverter.class.getModifiers()),
          "ComplexJniTypeConverter should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Default constructor should create converter")
    void defaultConstructorShouldCreateConverter() {
      final ComplexJniTypeConverter conv = new ComplexJniTypeConverter();

      assertNotNull(conv, "Converter should be created");
    }

    @Test
    @DisplayName("Constructor with configuration should create converter")
    void constructorWithConfigurationShouldCreateConverter() {
      final MarshalingConfiguration config = MarshalingConfiguration.defaultConfiguration();
      final ComplexJniTypeConverter conv = new ComplexJniTypeConverter(config);

      assertNotNull(conv, "Converter should be created with config");
    }

    @Test
    @DisplayName("Constructor should throw on null configuration")
    void constructorShouldThrowOnNullConfiguration() {
      assertThrows(NullPointerException.class,
          () -> new ComplexJniTypeConverter(null),
          "Should throw on null configuration");
    }
  }

  @Nested
  @DisplayName("marshalMultiDimensionalArray Tests")
  class MarshalMultiDimensionalArrayTests {

    @Test
    @DisplayName("Should marshal 1D int array")
    void shouldMarshal1DIntArray() throws WasmException {
      final int[] array = {1, 2, 3, 4, 5};

      final WasmValue[] result = converter.marshalMultiDimensionalArray(array);

      assertNotNull(result, "Result should not be null");
      assertEquals(4, result.length, "Should have 4 WasmValue elements");
      assertEquals(1, result[0].asI32(), "Dimensions should be 1");
      assertEquals(5, result[1].asI32(), "Total elements should be 5");
    }

    @Test
    @DisplayName("Should marshal 2D int array")
    void shouldMarshal2DIntArray() throws WasmException {
      final int[][] array = {{1, 2, 3}, {4, 5, 6}};

      final WasmValue[] result = converter.marshalMultiDimensionalArray(array);

      assertNotNull(result, "Result should not be null");
      assertEquals(4, result.length, "Should have 4 WasmValue elements");
      assertEquals(2, result[0].asI32(), "Dimensions should be 2");
      assertEquals(6, result[1].asI32(), "Total elements should be 6");
    }

    @Test
    @DisplayName("Should marshal 3D int array")
    void shouldMarshal3DIntArray() throws WasmException {
      final int[][][] array = {{{1, 2}, {3, 4}}, {{5, 6}, {7, 8}}};

      final WasmValue[] result = converter.marshalMultiDimensionalArray(array);

      assertNotNull(result, "Result should not be null");
      assertEquals(4, result.length, "Should have 4 WasmValue elements");
      assertEquals(3, result[0].asI32(), "Dimensions should be 3");
      assertEquals(8, result[1].asI32(), "Total elements should be 8");
    }

    @Test
    @DisplayName("Should throw on null array")
    void shouldThrowOnNullArray() {
      assertThrows(NullPointerException.class,
          () -> converter.marshalMultiDimensionalArray(null),
          "Should throw on null array");
    }

    @Test
    @DisplayName("Should throw on non-array object")
    void shouldThrowOnNonArrayObject() {
      assertThrows(WasmException.class,
          () -> converter.marshalMultiDimensionalArray("not an array"),
          "Should throw on non-array object");
    }

    @Test
    @DisplayName("Should marshal empty array")
    void shouldMarshalEmptyArray() throws WasmException {
      final int[] array = {};

      final WasmValue[] result = converter.marshalMultiDimensionalArray(array);

      assertNotNull(result, "Result should not be null");
      assertEquals(1, result[0].asI32(), "Dimensions should be 1");
      assertEquals(0, result[1].asI32(), "Total elements should be 0");
    }

    @Test
    @DisplayName("Should marshal long array")
    void shouldMarshalLongArray() throws WasmException {
      final long[] array = {100L, 200L, 300L};

      final WasmValue[] result = converter.marshalMultiDimensionalArray(array);

      assertNotNull(result, "Result should not be null");
      assertEquals(1, result[0].asI32(), "Dimensions should be 1");
      assertEquals(3, result[1].asI32(), "Total elements should be 3");
    }

    @Test
    @DisplayName("Should marshal double array")
    void shouldMarshalDoubleArray() throws WasmException {
      final double[] array = {1.5, 2.5, 3.5};

      final WasmValue[] result = converter.marshalMultiDimensionalArray(array);

      assertNotNull(result, "Result should not be null");
      assertEquals(1, result[0].asI32(), "Dimensions should be 1");
      assertEquals(3, result[1].asI32(), "Total elements should be 3");
    }

    @Test
    @DisplayName("Should marshal float array")
    void shouldMarshalFloatArray() throws WasmException {
      final float[] array = {1.5f, 2.5f};

      final WasmValue[] result = converter.marshalMultiDimensionalArray(array);

      assertNotNull(result, "Result should not be null");
      assertEquals(1, result[0].asI32(), "Dimensions should be 1");
      assertEquals(2, result[1].asI32(), "Total elements should be 2");
    }

    @Test
    @DisplayName("Should marshal byte array")
    void shouldMarshalByteArray() throws WasmException {
      final byte[] array = {1, 2, 3, 4};

      final WasmValue[] result = converter.marshalMultiDimensionalArray(array);

      assertNotNull(result, "Result should not be null");
      assertEquals(1, result[0].asI32(), "Dimensions should be 1");
      assertEquals(4, result[1].asI32(), "Total elements should be 4");
    }

    @Test
    @DisplayName("Should marshal short array")
    void shouldMarshalShortArray() throws WasmException {
      final short[] array = {10, 20, 30};

      final WasmValue[] result = converter.marshalMultiDimensionalArray(array);

      assertNotNull(result, "Result should not be null");
      assertEquals(1, result[0].asI32(), "Dimensions should be 1");
      assertEquals(3, result[1].asI32(), "Total elements should be 3");
    }

    @Test
    @DisplayName("Should marshal char array")
    void shouldMarshalCharArray() throws WasmException {
      final char[] array = {'a', 'b', 'c'};

      final WasmValue[] result = converter.marshalMultiDimensionalArray(array);

      assertNotNull(result, "Result should not be null");
      assertEquals(1, result[0].asI32(), "Dimensions should be 1");
      assertEquals(3, result[1].asI32(), "Total elements should be 3");
    }

    @Test
    @DisplayName("Should marshal boolean array")
    void shouldMarshalBooleanArray() throws WasmException {
      final boolean[] array = {true, false, true};

      final WasmValue[] result = converter.marshalMultiDimensionalArray(array);

      assertNotNull(result, "Result should not be null");
      assertEquals(1, result[0].asI32(), "Dimensions should be 1");
      assertEquals(3, result[1].asI32(), "Total elements should be 3");
    }
  }

  @Nested
  @DisplayName("marshalCollection Tests")
  class MarshalCollectionTests {

    @Test
    @DisplayName("Should marshal List")
    void shouldMarshalList() throws WasmException {
      final List<String> list = new ArrayList<>();
      list.add("a");
      list.add("b");
      list.add("c");

      final WasmValue[] result = converter.marshalCollection(list);

      assertNotNull(result, "Result should not be null");
      assertEquals(2, result.length, "Should have 2 WasmValue elements");
      assertEquals(3, result[0].asI32(), "Size should be 3");
    }

    @Test
    @DisplayName("Should marshal Map")
    void shouldMarshalMap() throws WasmException {
      final Map<String, Integer> map = new HashMap<>();
      map.put("a", 1);
      map.put("b", 2);

      final WasmValue[] result = converter.marshalCollection(map);

      assertNotNull(result, "Result should not be null");
      assertEquals(3, result.length, "Should have 3 WasmValue elements");
      assertEquals(2, result[0].asI32(), "Size should be 2");
    }

    @Test
    @DisplayName("Should throw on null collection")
    void shouldThrowOnNullCollection() {
      assertThrows(NullPointerException.class,
          () -> converter.marshalCollection(null),
          "Should throw on null collection");
    }

    @Test
    @DisplayName("Should throw on unsupported collection type")
    void shouldThrowOnUnsupportedCollectionType() {
      assertThrows(WasmException.class,
          () -> converter.marshalCollection("not a collection"),
          "Should throw on unsupported collection type");
    }

    @Test
    @DisplayName("Should marshal empty List")
    void shouldMarshalEmptyList() throws WasmException {
      final List<String> list = new ArrayList<>();

      final WasmValue[] result = converter.marshalCollection(list);

      assertNotNull(result, "Result should not be null");
      assertEquals(0, result[0].asI32(), "Size should be 0");
    }

    @Test
    @DisplayName("Should marshal empty Map")
    void shouldMarshalEmptyMap() throws WasmException {
      final Map<String, Integer> map = new HashMap<>();

      final WasmValue[] result = converter.marshalCollection(map);

      assertNotNull(result, "Result should not be null");
      assertEquals(0, result[0].asI32(), "Size should be 0");
    }
  }

  @Nested
  @DisplayName("estimateMarshalingOverhead Tests")
  class EstimateMarshalingOverheadTests {

    @Test
    @DisplayName("Should estimate overhead for int array")
    void shouldEstimateOverheadForIntArray() {
      final int[] array = {1, 2, 3, 4, 5};

      final long overhead = converter.estimateMarshalingOverhead(array);

      assertTrue(overhead >= 0, "Overhead should be non-negative");
    }

    @Test
    @DisplayName("Should estimate overhead for List")
    void shouldEstimateOverheadForList() {
      final List<String> list = new ArrayList<>();
      list.add("test");

      final long overhead = converter.estimateMarshalingOverhead(list);

      assertTrue(overhead >= 0, "Overhead should be non-negative");
    }

    @Test
    @DisplayName("Should throw on null object")
    void shouldThrowOnNullObject() {
      assertThrows(NullPointerException.class,
          () -> converter.estimateMarshalingOverhead(null),
          "Should throw on null object");
    }
  }

  @Nested
  @DisplayName("validateMarshalableObject Tests")
  class ValidateMarshalableObjectTests {

    @Test
    @DisplayName("Should validate array")
    void shouldValidateArray() throws WasmException {
      final int[] array = {1, 2, 3};

      // Should not throw
      converter.validateMarshalableObject(array);
    }

    @Test
    @DisplayName("Should validate List")
    void shouldValidateList() throws WasmException {
      final List<String> list = new ArrayList<>();
      list.add("test");

      // Should not throw
      converter.validateMarshalableObject(list);
    }

    @Test
    @DisplayName("Should throw on null object")
    void shouldThrowOnNullObject() {
      assertThrows(NullPointerException.class,
          () -> converter.validateMarshalableObject(null),
          "Should throw on null object");
    }

    @Test
    @DisplayName("Should throw on enum type")
    void shouldThrowOnEnumType() {
      assertThrows(WasmException.class,
          () -> converter.validateMarshalableObject(TestEnum.VALUE1),
          "Should throw on enum type");
    }
  }

  @Nested
  @DisplayName("unmarshalMultiDimensionalArray Tests")
  class UnmarshalMultiDimensionalArrayTests {

    @Test
    @DisplayName("Should throw on null WasmValues")
    void shouldThrowOnNullWasmValues() {
      assertThrows(IllegalArgumentException.class,
          () -> converter.unmarshalMultiDimensionalArray(null, int[].class),
          "Should throw on null WasmValues");
    }

    @Test
    @DisplayName("Should throw on null array type")
    void shouldThrowOnNullArrayType() {
      final WasmValue[] wasmValues = new WasmValue[4];

      assertThrows(IllegalArgumentException.class,
          () -> converter.unmarshalMultiDimensionalArray(wasmValues, null),
          "Should throw on null array type");
    }

    @Test
    @DisplayName("Should throw on invalid array format")
    void shouldThrowOnInvalidArrayFormat() {
      final WasmValue[] wasmValues = new WasmValue[2]; // Should be 4

      assertThrows(WasmException.class,
          () -> converter.unmarshalMultiDimensionalArray(wasmValues, int[].class),
          "Should throw on invalid format");
    }
  }

  @Nested
  @DisplayName("unmarshalCollection Tests")
  class UnmarshalCollectionTests {

    @Test
    @DisplayName("Should throw on null WasmValues")
    void shouldThrowOnNullWasmValues() {
      assertThrows(IllegalArgumentException.class,
          () -> converter.unmarshalCollection(null, List.class),
          "Should throw on null WasmValues");
    }

    @Test
    @DisplayName("Should throw on null collection type")
    void shouldThrowOnNullCollectionType() {
      final WasmValue[] wasmValues = new WasmValue[2];

      assertThrows(IllegalArgumentException.class,
          () -> converter.unmarshalCollection(wasmValues, null),
          "Should throw on null collection type");
    }

    @Test
    @DisplayName("Should throw on unsupported collection type")
    void shouldThrowOnUnsupportedCollectionType() {
      final WasmValue[] wasmValues = new WasmValue[] {
          WasmValue.i32(0)
      };

      assertThrows(WasmException.class,
          () -> converter.unmarshalCollection(wasmValues, String.class),
          "Should throw on unsupported collection type");
    }
  }

  @Nested
  @DisplayName("convertComplexObjectToWasm Tests")
  class ConvertComplexObjectToWasmTests {

    @Test
    @DisplayName("Should throw on null object")
    void shouldThrowOnNullObject() {
      assertThrows(NullPointerException.class,
          () -> converter.convertComplexObjectToWasm(null),
          "Should throw on null object");
    }

    @Test
    @DisplayName("Should convert List to Wasm")
    void shouldConvertListToWasm() throws WasmException {
      final List<Integer> list = new ArrayList<>();
      list.add(1);
      list.add(2);
      list.add(3);

      final WasmValue[] result = converter.convertComplexObjectToWasm(list);

      assertNotNull(result, "Result should not be null");
    }
  }

  @Nested
  @DisplayName("convertWasmToComplexObject Tests")
  class ConvertWasmToComplexObjectTests {

    @Test
    @DisplayName("Should throw on null WasmValues")
    void shouldThrowOnNullWasmValues() {
      assertThrows(NullPointerException.class,
          () -> converter.convertWasmToComplexObject(null, String.class),
          "Should throw on null WasmValues");
    }

    @Test
    @DisplayName("Should throw on null expected type")
    void shouldThrowOnNullExpectedType() {
      final WasmValue[] wasmValues = new WasmValue[] {WasmValue.i32(0)};

      assertThrows(NullPointerException.class,
          () -> converter.convertWasmToComplexObject(wasmValues, null),
          "Should throw on null expected type");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle large array")
    void shouldHandleLargeArray() throws WasmException {
      final int[] largeArray = new int[10000];
      for (int i = 0; i < largeArray.length; i++) {
        largeArray[i] = i;
      }

      final WasmValue[] result = converter.marshalMultiDimensionalArray(largeArray);

      assertNotNull(result, "Result should not be null");
      assertEquals(10000, result[1].asI32(), "Total elements should be 10000");
    }

    @Test
    @DisplayName("Should handle deeply nested array")
    void shouldHandleDeeplyNestedArray() throws WasmException {
      // 4D array
      final int[][][][] array = new int[2][2][2][2];
      int value = 0;
      for (int i = 0; i < 2; i++) {
        for (int j = 0; j < 2; j++) {
          for (int k = 0; k < 2; k++) {
            for (int l = 0; l < 2; l++) {
              array[i][j][k][l] = value++;
            }
          }
        }
      }

      final WasmValue[] result = converter.marshalMultiDimensionalArray(array);

      assertNotNull(result, "Result should not be null");
      assertEquals(4, result[0].asI32(), "Dimensions should be 4");
      assertEquals(16, result[1].asI32(), "Total elements should be 16");
    }

    @Test
    @DisplayName("Should handle single element array")
    void shouldHandleSingleElementArray() throws WasmException {
      final int[] array = {42};

      final WasmValue[] result = converter.marshalMultiDimensionalArray(array);

      assertNotNull(result, "Result should not be null");
      assertEquals(1, result[1].asI32(), "Total elements should be 1");
    }
  }

  /** Test enum for validation tests. */
  private enum TestEnum {
    VALUE1,
    VALUE2
  }
}
