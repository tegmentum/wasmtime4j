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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasmComplexValue}.
 *
 * <p>Verifies factory methods, type-safe accessors, null handling, equality, hashCode, toString,
 * and validation.
 */
@DisplayName("WasmComplexValue Tests")
class WasmComplexValueTest {

  @Nested
  @DisplayName("ComplexType Enum Tests")
  class ComplexTypeEnumTests {

    @Test
    @DisplayName("should have exactly 9 complex types")
    void shouldHaveExactlyNineComplexTypes() {
      assertEquals(
          9, WasmComplexValue.ComplexType.values().length, "ComplexType should have 9 values");
    }

    @Test
    @DisplayName("should contain all expected complex types")
    void shouldContainAllExpectedTypes() {
      assertNotNull(WasmComplexValue.ComplexType.MULTI_ARRAY, "MULTI_ARRAY should exist");
      assertNotNull(WasmComplexValue.ComplexType.LIST, "LIST should exist");
      assertNotNull(WasmComplexValue.ComplexType.MAP, "MAP should exist");
      assertNotNull(WasmComplexValue.ComplexType.OBJECT, "OBJECT should exist");
      assertNotNull(WasmComplexValue.ComplexType.STRUCT, "STRUCT should exist");
      assertNotNull(WasmComplexValue.ComplexType.UNION, "UNION should exist");
      assertNotNull(WasmComplexValue.ComplexType.BINARY_BLOB, "BINARY_BLOB should exist");
      assertNotNull(WasmComplexValue.ComplexType.STRING_DATA, "STRING_DATA should exist");
      assertNotNull(WasmComplexValue.ComplexType.NULL_REF, "NULL_REF should exist");
    }
  }

  @Nested
  @DisplayName("MultiArray Factory Tests")
  class MultiArrayFactoryTests {

    @Test
    @DisplayName("should create multi-dimensional array value")
    void shouldCreateMultiArrayValue() {
      final int[][] array = {{1, 2}, {3, 4}};
      final WasmComplexValue val = WasmComplexValue.multiArray(array);
      assertEquals(
          WasmComplexValue.ComplexType.MULTI_ARRAY,
          val.getComplexType(),
          "Type should be MULTI_ARRAY");
      assertFalse(val.isNull(), "Should not be null");
    }

    @Test
    @DisplayName("should throw for null array")
    void shouldThrowForNullArray() {
      assertThrows(
          NullPointerException.class,
          () -> WasmComplexValue.multiArray(null),
          "Should throw for null array");
    }

    @Test
    @DisplayName("should throw for non-array argument")
    void shouldThrowForNonArray() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasmComplexValue.multiArray("not an array"),
          "Should throw for non-array");
    }

    @Test
    @DisplayName("should throw for single-dimensional array")
    void shouldThrowForSingleDimensionalArray() {
      final int[] array = {1, 2, 3};
      assertThrows(
          IllegalArgumentException.class,
          () -> WasmComplexValue.multiArray(array),
          "Should throw for 1D array");
    }

    @Test
    @DisplayName("asMultiArray should return the array")
    void asMultiArrayShouldReturnArray() {
      final int[][] array = {{1, 2}, {3, 4}};
      final WasmComplexValue val = WasmComplexValue.multiArray(array);
      final int[][] result = val.asMultiArray();
      assertArrayEquals(array, result, "Should return same array");
    }

    @Test
    @DisplayName("asMultiArray should throw for wrong type")
    void asMultiArrayShouldThrowForWrongType() {
      final WasmComplexValue val = WasmComplexValue.string("test");
      assertThrows(
          ClassCastException.class, () -> val.asMultiArray(), "Should throw for wrong type");
    }
  }

  @Nested
  @DisplayName("List Factory Tests")
  class ListFactoryTests {

    @Test
    @DisplayName("should create list value")
    void shouldCreateListValue() {
      final List<String> list = Arrays.asList("a", "b", "c");
      final WasmComplexValue val = WasmComplexValue.list(list, String.class);
      assertEquals(WasmComplexValue.ComplexType.LIST, val.getComplexType(), "Type should be LIST");
    }

    @Test
    @DisplayName("asList should return the list")
    void asListShouldReturnList() {
      final List<Integer> list = Arrays.asList(1, 2, 3);
      final WasmComplexValue val = WasmComplexValue.list(list, Integer.class);
      final List<Integer> result = val.asList();
      assertEquals(list, result, "Should return same list");
    }

    @Test
    @DisplayName("should throw for null list")
    void shouldThrowForNullList() {
      assertThrows(
          NullPointerException.class,
          () -> WasmComplexValue.list(null, String.class),
          "Should throw for null list");
    }

    @Test
    @DisplayName("should throw for null element type")
    void shouldThrowForNullElementType() {
      assertThrows(
          NullPointerException.class,
          () -> WasmComplexValue.list(Arrays.asList("a"), null),
          "Should throw for null element type");
    }
  }

  @Nested
  @DisplayName("Map Factory Tests")
  class MapFactoryTests {

    @Test
    @DisplayName("should create map value")
    void shouldCreateMapValue() {
      final Map<String, Integer> map = new HashMap<>();
      map.put("key", 42);
      final WasmComplexValue val = WasmComplexValue.map(map, String.class, Integer.class);
      assertEquals(WasmComplexValue.ComplexType.MAP, val.getComplexType(), "Type should be MAP");
    }

    @Test
    @DisplayName("asMap should return the map")
    void asMapShouldReturnMap() {
      final Map<String, Integer> map = new HashMap<>();
      map.put("key", 42);
      final WasmComplexValue val = WasmComplexValue.map(map, String.class, Integer.class);
      final Map<String, Integer> result = val.asMap();
      assertEquals(map, result, "Should return same map");
    }

    @Test
    @DisplayName("should throw for null map")
    void shouldThrowForNullMap() {
      assertThrows(
          NullPointerException.class,
          () -> WasmComplexValue.map(null, String.class, Integer.class),
          "Should throw for null map");
    }
  }

  @Nested
  @DisplayName("Object Factory Tests")
  class ObjectFactoryTests {

    @Test
    @DisplayName("should create object value")
    void shouldCreateObjectValue() {
      final WasmComplexValue val = WasmComplexValue.object("test object");
      assertEquals(
          WasmComplexValue.ComplexType.OBJECT, val.getComplexType(), "Type should be OBJECT");
    }

    @Test
    @DisplayName("asObject should return the object")
    void asObjectShouldReturnObject() {
      final String obj = "test object";
      final WasmComplexValue val = WasmComplexValue.object(obj);
      final String result = val.asObject();
      assertEquals(obj, result, "Should return same object");
    }

    @Test
    @DisplayName("should throw for null object")
    void shouldThrowForNullObject() {
      assertThrows(
          NullPointerException.class,
          () -> WasmComplexValue.object(null),
          "Should throw for null object");
    }
  }

  @Nested
  @DisplayName("BinaryBlob Factory Tests")
  class BinaryBlobFactoryTests {

    @Test
    @DisplayName("should create binary blob value")
    void shouldCreateBinaryBlobValue() {
      final byte[] data = {1, 2, 3, 4};
      final WasmComplexValue val = WasmComplexValue.binaryBlob(data);
      assertEquals(
          WasmComplexValue.ComplexType.BINARY_BLOB,
          val.getComplexType(),
          "Type should be BINARY_BLOB");
    }

    @Test
    @DisplayName("asBinaryBlob should return a copy of the data")
    void asBinaryBlobShouldReturnCopy() {
      final byte[] data = {1, 2, 3, 4};
      final WasmComplexValue val = WasmComplexValue.binaryBlob(data);
      final byte[] result = val.asBinaryBlob();
      assertArrayEquals(data, result, "Should return equal data");
      assertTrue(data != result, "Should return a defensive copy");
    }

    @Test
    @DisplayName("should throw for null data")
    void shouldThrowForNullData() {
      assertThrows(
          NullPointerException.class,
          () -> WasmComplexValue.binaryBlob(null),
          "Should throw for null data");
    }

    @Test
    @DisplayName("modifying original array should not affect stored value")
    void modifyingOriginalShouldNotAffectStored() {
      final byte[] data = {1, 2, 3};
      final WasmComplexValue val = WasmComplexValue.binaryBlob(data);
      data[0] = 99;
      final byte[] result = val.asBinaryBlob();
      assertEquals(1, result[0], "Stored value should not be affected by original modification");
    }
  }

  @Nested
  @DisplayName("String Factory Tests")
  class StringFactoryTests {

    @Test
    @DisplayName("should create string value")
    void shouldCreateStringValue() {
      final WasmComplexValue val = WasmComplexValue.string("hello");
      assertEquals(
          WasmComplexValue.ComplexType.STRING_DATA,
          val.getComplexType(),
          "Type should be STRING_DATA");
    }

    @Test
    @DisplayName("asString should return the string")
    void asStringShouldReturnString() {
      final WasmComplexValue val = WasmComplexValue.string("hello");
      assertEquals("hello", val.asString(), "Should return hello");
    }

    @Test
    @DisplayName("should throw for null string")
    void shouldThrowForNullString() {
      assertThrows(
          NullPointerException.class,
          () -> WasmComplexValue.string(null),
          "Should throw for null string");
    }

    @Test
    @DisplayName("asString should throw for wrong type")
    void asStringShouldThrowForWrongType() {
      final WasmComplexValue val = WasmComplexValue.object("test");
      assertThrows(ClassCastException.class, () -> val.asString(), "Should throw for wrong type");
    }
  }

  @Nested
  @DisplayName("NullRef Factory Tests")
  class NullRefFactoryTests {

    @Test
    @DisplayName("should create null reference value")
    void shouldCreateNullRefValue() {
      final WasmComplexValue val = WasmComplexValue.nullRef(String.class);
      assertEquals(
          WasmComplexValue.ComplexType.NULL_REF, val.getComplexType(), "Type should be NULL_REF");
      assertTrue(val.isNull(), "Should be null");
    }

    @Test
    @DisplayName("getValue should return null for null ref")
    void getValueShouldReturnNull() {
      final WasmComplexValue val = WasmComplexValue.nullRef(String.class);
      assertNull(val.getValue(), "getValue should return null");
    }

    @Test
    @DisplayName("should throw for null expected type")
    void shouldThrowForNullExpectedType() {
      assertThrows(
          NullPointerException.class,
          () -> WasmComplexValue.nullRef(null),
          "Should throw for null expected type");
    }
  }

  @Nested
  @DisplayName("Accessor Tests")
  class AccessorTests {

    @Test
    @DisplayName("getJavaType should return correct type for list")
    void getJavaTypeShouldReturnCorrectTypeForList() {
      final WasmComplexValue val = WasmComplexValue.list(Arrays.asList("a"), String.class);
      assertEquals(List.class, val.getJavaType(), "Java type should be List");
    }

    @Test
    @DisplayName("getJavaType should return correct type for map")
    void getJavaTypeShouldReturnCorrectTypeForMap() {
      final WasmComplexValue val =
          WasmComplexValue.map(new HashMap<>(), String.class, Integer.class);
      assertEquals(Map.class, val.getJavaType(), "Java type should be Map");
    }

    @Test
    @DisplayName("getMetadata should not return null")
    void getMetadataShouldNotReturnNull() {
      final WasmComplexValue val = WasmComplexValue.string("test");
      assertNotNull(val.getMetadata(), "Metadata should not be null");
    }
  }

  @Nested
  @DisplayName("IsNull Tests")
  class IsNullTests {

    @Test
    @DisplayName("non-null value should not be null")
    void nonNullValueShouldNotBeNull() {
      final WasmComplexValue val = WasmComplexValue.string("test");
      assertFalse(val.isNull(), "String value should not be null");
    }

    @Test
    @DisplayName("null ref should be null")
    void nullRefShouldBeNull() {
      final WasmComplexValue val = WasmComplexValue.nullRef(Object.class);
      assertTrue(val.isNull(), "Null ref should be null");
    }
  }

  @Nested
  @DisplayName("ValidateCompatibility Tests")
  class ValidateCompatibilityTests {

    @Test
    @DisplayName("should pass for compatible types")
    void shouldPassForCompatibleTypes() throws WasmException {
      final WasmComplexValue val = WasmComplexValue.string("test");
      val.validateCompatibility(String.class);
      val.validateCompatibility(Object.class);
    }

    @Test
    @DisplayName("should pass for null ref with any type")
    void shouldPassForNullRefWithAnyType() throws WasmException {
      final WasmComplexValue val = WasmComplexValue.nullRef(String.class);
      val.validateCompatibility(Integer.class);
    }

    @Test
    @DisplayName("should throw for null expected type")
    void shouldThrowForNullExpectedType() {
      final WasmComplexValue val = WasmComplexValue.string("test");
      assertThrows(
          NullPointerException.class,
          () -> val.validateCompatibility(null),
          "Should throw for null expected type");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("equal string values should be equal")
    void equalStringValuesShouldBeEqual() {
      final WasmComplexValue val1 = WasmComplexValue.string("hello");
      final WasmComplexValue val2 = WasmComplexValue.string("hello");
      assertEquals(val1, val2, "Same string values should be equal");
    }

    @Test
    @DisplayName("different string values should not be equal")
    void differentStringValuesShouldNotBeEqual() {
      final WasmComplexValue val1 = WasmComplexValue.string("hello");
      final WasmComplexValue val2 = WasmComplexValue.string("world");
      assertNotEquals(val1, val2, "Different string values should not be equal");
    }

    @Test
    @DisplayName("equal binary blobs should be equal")
    void equalBinaryBlobsShouldBeEqual() {
      final byte[] data = {1, 2, 3};
      final WasmComplexValue val1 = WasmComplexValue.binaryBlob(data);
      final WasmComplexValue val2 = WasmComplexValue.binaryBlob(data);
      assertEquals(val1, val2, "Same binary blobs should be equal");
    }

    @Test
    @DisplayName("different types should not be equal")
    void differentTypesShouldNotBeEqual() {
      final WasmComplexValue val1 = WasmComplexValue.string("test");
      final WasmComplexValue val2 = WasmComplexValue.object("test");
      assertNotEquals(val1, val2, "Different complex types should not be equal");
    }

    @Test
    @DisplayName("should not equal null")
    void shouldNotEqualNull() {
      final WasmComplexValue val = WasmComplexValue.string("test");
      assertNotEquals(null, val, "Should not equal null");
    }

    @Test
    @DisplayName("should be reflexive")
    void shouldBeReflexive() {
      final WasmComplexValue val = WasmComplexValue.string("test");
      assertEquals(val, val, "Equality should be reflexive");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("equal objects should have same hashCode")
    void equalObjectsShouldHaveSameHashCode() {
      final WasmComplexValue val1 = WasmComplexValue.string("hello");
      final WasmComplexValue val2 = WasmComplexValue.string("hello");
      assertEquals(val1.hashCode(), val2.hashCode(), "Equal objects should have same hashCode");
    }

    @Test
    @DisplayName("hashCode should be consistent")
    void hashCodeShouldBeConsistent() {
      final WasmComplexValue val = WasmComplexValue.string("test");
      final int hash1 = val.hashCode();
      final int hash2 = val.hashCode();
      assertEquals(hash1, hash2, "hashCode should be consistent");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain class name and type for string")
    void toStringShouldContainInfoForString() {
      final WasmComplexValue val = WasmComplexValue.string("hello");
      final String str = val.toString();
      assertNotNull(str, "toString should not be null");
      assertTrue(str.contains("WasmComplexValue"), "Should contain class name");
      assertTrue(str.contains("STRING_DATA"), "Should contain type");
    }

    @Test
    @DisplayName("toString should indicate null for null ref")
    void toStringShouldIndicateNullForNullRef() {
      final WasmComplexValue val = WasmComplexValue.nullRef(String.class);
      final String str = val.toString();
      assertTrue(str.contains("null"), "Should indicate null in toString");
    }

    @Test
    @DisplayName("toString should show byte length for binary blob")
    void toStringShouldShowByteLengthForBinaryBlob() {
      final byte[] data = {1, 2, 3, 4, 5};
      final WasmComplexValue val = WasmComplexValue.binaryBlob(data);
      final String str = val.toString();
      assertTrue(str.contains("byte[5]"), "Should show byte array length");
    }
  }
}
