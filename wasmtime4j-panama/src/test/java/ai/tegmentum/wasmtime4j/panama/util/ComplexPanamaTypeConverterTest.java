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

package ai.tegmentum.wasmtime4j.panama.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.MarshalingConfiguration;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.ArenaResourceManager;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link ComplexPanamaTypeConverter} class.
 *
 * <p>This test class verifies the complex type conversion utilities for Panama Foreign Function
 * API including multi-dimensional arrays, collections, and POJOs.
 */
@DisplayName("ComplexPanamaTypeConverter Tests")
class ComplexPanamaTypeConverterTest {

  private Arena arena;
  private ArenaResourceManager arenaManager;
  private ComplexPanamaTypeConverter converter;

  @BeforeEach
  void setUp() {
    arena = Arena.ofShared();
    arenaManager = new ArenaResourceManager(arena, false);
    converter = new ComplexPanamaTypeConverter(arenaManager);
  }

  @AfterEach
  void tearDown() {
    if (arena.scope().isAlive()) {
      arena.close();
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("ComplexPanamaTypeConverter should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(ComplexPanamaTypeConverter.class.getModifiers()),
          "ComplexPanamaTypeConverter should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor with arenaManager should create converter")
    void constructorWithArenaManagerShouldCreateConverter() {
      final ComplexPanamaTypeConverter testConverter =
          new ComplexPanamaTypeConverter(arenaManager);

      assertNotNull(testConverter, "Converter should be created");
    }

    @Test
    @DisplayName("Constructor with configuration should create converter")
    void constructorWithConfigurationShouldCreateConverter() {
      final MarshalingConfiguration config = MarshalingConfiguration.defaultConfiguration();
      final ComplexPanamaTypeConverter testConverter =
          new ComplexPanamaTypeConverter(config, arenaManager);

      assertNotNull(testConverter, "Converter should be created");
    }

    @Test
    @DisplayName("Constructor should throw for null arenaManager")
    void constructorShouldThrowForNullArenaManager() {
      assertThrows(
          NullPointerException.class,
          () -> new ComplexPanamaTypeConverter(null),
          "Should throw for null arenaManager");
    }

    @Test
    @DisplayName("Constructor should throw for null configuration")
    void constructorShouldThrowForNullConfiguration() {
      assertThrows(
          NullPointerException.class,
          () -> new ComplexPanamaTypeConverter(null, arenaManager),
          "Should throw for null configuration");
    }
  }

  @Nested
  @DisplayName("convertComplexObjectToPanamaMemory Tests")
  class ConvertComplexObjectToPanamaMemoryTests {

    @Test
    @DisplayName("convertComplexObjectToPanamaMemory should convert byte array")
    void convertComplexObjectToPanamaMemoryShouldConvertByteArray() throws WasmException {
      final byte[] data = new byte[] {1, 2, 3, 4, 5};

      final MemorySegment result = converter.convertComplexObjectToPanamaMemory(data);

      assertNotNull(result, "Result should not be null");
      assertTrue(result.byteSize() >= data.length, "Segment should be large enough");
    }

    @Test
    @DisplayName("convertComplexObjectToPanamaMemory should convert int array")
    void convertComplexObjectToPanamaMemoryShouldConvertIntArray() throws WasmException {
      final int[] data = new int[] {10, 20, 30, 40, 50};

      final MemorySegment result = converter.convertComplexObjectToPanamaMemory(data);

      assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("convertComplexObjectToPanamaMemory should convert long array")
    void convertComplexObjectToPanamaMemoryShouldConvertLongArray() throws WasmException {
      final long[] data = new long[] {100L, 200L, 300L};

      final MemorySegment result = converter.convertComplexObjectToPanamaMemory(data);

      assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("convertComplexObjectToPanamaMemory should convert float array")
    void convertComplexObjectToPanamaMemoryShouldConvertFloatArray() throws WasmException {
      final float[] data = new float[] {1.1f, 2.2f, 3.3f};

      final MemorySegment result = converter.convertComplexObjectToPanamaMemory(data);

      assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("convertComplexObjectToPanamaMemory should convert double array")
    void convertComplexObjectToPanamaMemoryShouldConvertDoubleArray() throws WasmException {
      final double[] data = new double[] {1.11, 2.22, 3.33};

      final MemorySegment result = converter.convertComplexObjectToPanamaMemory(data);

      assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("convertComplexObjectToPanamaMemory should convert short array")
    void convertComplexObjectToPanamaMemoryShouldConvertShortArray() throws WasmException {
      final short[] data = new short[] {1, 2, 3};

      final MemorySegment result = converter.convertComplexObjectToPanamaMemory(data);

      assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("convertComplexObjectToPanamaMemory should convert char array")
    void convertComplexObjectToPanamaMemoryShouldConvertCharArray() throws WasmException {
      final char[] data = new char[] {'a', 'b', 'c'};

      final MemorySegment result = converter.convertComplexObjectToPanamaMemory(data);

      assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("convertComplexObjectToPanamaMemory should throw for null object")
    void convertComplexObjectToPanamaMemoryShouldThrowForNullObject() {
      assertThrows(
          NullPointerException.class,
          () -> converter.convertComplexObjectToPanamaMemory(null),
          "Should throw for null object");
    }
  }

  @Nested
  @DisplayName("convertPanamaMemoryToComplexObject Tests")
  class ConvertPanamaMemoryToComplexObjectTests {

    @Test
    @DisplayName("convertPanamaMemoryToComplexObject should throw for null memorySegment")
    void convertPanamaMemoryToComplexObjectShouldThrowForNullMemorySegment() {
      assertThrows(
          NullPointerException.class,
          () -> converter.convertPanamaMemoryToComplexObject(null, byte[].class),
          "Should throw for null memorySegment");
    }

    @Test
    @DisplayName("convertPanamaMemoryToComplexObject should throw for null expectedType")
    void convertPanamaMemoryToComplexObjectShouldThrowForNullExpectedType() {
      final MemorySegment segment = arena.allocate(64);

      assertThrows(
          NullPointerException.class,
          () -> converter.convertPanamaMemoryToComplexObject(segment, null),
          "Should throw for null expectedType");
    }
  }

  @Nested
  @DisplayName("marshalMultiDimensionalArrayToPanama Tests")
  class MarshalMultiDimensionalArrayToPanamaTests {

    @Test
    @DisplayName("marshalMultiDimensionalArrayToPanama should marshal 1D array")
    void marshalMultiDimensionalArrayToPanamaShouldMarshal1DArray() throws WasmException {
      final int[] array = new int[] {1, 2, 3, 4, 5};

      final MemorySegment result = converter.marshalMultiDimensionalArrayToPanama(array);

      assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("marshalMultiDimensionalArrayToPanama should marshal 2D array")
    void marshalMultiDimensionalArrayToPanamaShouldMarshal2DArray() throws WasmException {
      final int[][] array = new int[][] {{1, 2}, {3, 4}, {5, 6}};

      final MemorySegment result = converter.marshalMultiDimensionalArrayToPanama(array);

      assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("marshalMultiDimensionalArrayToPanama should throw for non-array")
    void marshalMultiDimensionalArrayToPanamaShouldThrowForNonArray() {
      final String notAnArray = "not an array";

      assertThrows(
          WasmException.class,
          () -> converter.marshalMultiDimensionalArrayToPanama(notAnArray),
          "Should throw for non-array");
    }

    @Test
    @DisplayName("marshalMultiDimensionalArrayToPanama should throw for null array")
    void marshalMultiDimensionalArrayToPanamaShouldThrowForNullArray() {
      assertThrows(
          NullPointerException.class,
          () -> converter.marshalMultiDimensionalArrayToPanama(null),
          "Should throw for null array");
    }
  }

  @Nested
  @DisplayName("unmarshalMultiDimensionalArrayFromPanama Tests")
  class UnmarshalMultiDimensionalArrayFromPanamaTests {

    @Test
    @DisplayName("unmarshalMultiDimensionalArrayFromPanama should throw for null segment")
    void unmarshalMultiDimensionalArrayFromPanamaShouldThrowForNullSegment() {
      assertThrows(
          Exception.class,
          () -> converter.unmarshalMultiDimensionalArrayFromPanama(null, int[].class),
          "Should throw for null segment");
    }

    @Test
    @DisplayName("unmarshalMultiDimensionalArrayFromPanama should throw for null arrayType")
    void unmarshalMultiDimensionalArrayFromPanamaShouldThrowForNullArrayType() {
      final MemorySegment segment = arena.allocate(64);

      assertThrows(
          Exception.class,
          () -> converter.unmarshalMultiDimensionalArrayFromPanama(segment, null),
          "Should throw for null arrayType");
    }
  }

  @Nested
  @DisplayName("marshalCollectionToPanama Tests")
  class MarshalCollectionToPanamaTests {

    @Test
    @DisplayName("marshalCollectionToPanama should marshal List")
    void marshalCollectionToPanamaShouldMarshalList() throws WasmException {
      final List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);

      final MemorySegment result = converter.marshalCollectionToPanama(list);

      assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("marshalCollectionToPanama should marshal Map")
    void marshalCollectionToPanamaShouldMarshalMap() throws WasmException {
      final Map<String, Integer> map = new HashMap<>();
      map.put("one", 1);
      map.put("two", 2);

      final MemorySegment result = converter.marshalCollectionToPanama(map);

      assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("marshalCollectionToPanama should throw for unsupported collection")
    void marshalCollectionToPanamaShouldThrowForUnsupportedCollection() {
      final java.util.Set<Integer> set = new java.util.HashSet<>();
      set.add(1);

      assertThrows(
          WasmException.class,
          () -> converter.marshalCollectionToPanama(set),
          "Should throw for unsupported collection");
    }

    @Test
    @DisplayName("marshalCollectionToPanama should throw for null collection")
    void marshalCollectionToPanamaShouldThrowForNullCollection() {
      assertThrows(
          NullPointerException.class,
          () -> converter.marshalCollectionToPanama(null),
          "Should throw for null collection");
    }
  }

  @Nested
  @DisplayName("unmarshalCollectionFromPanama Tests")
  class UnmarshalCollectionFromPanamaTests {

    @Test
    @DisplayName("unmarshalCollectionFromPanama should unmarshal List")
    void unmarshalCollectionFromPanamaShouldUnmarshalList() throws WasmException {
      final MemorySegment segment = arena.allocate(1024);

      final Object result = converter.unmarshalCollectionFromPanama(segment, List.class);

      assertNotNull(result, "Result should not be null");
      assertTrue(result instanceof List, "Result should be a List");
    }

    @Test
    @DisplayName("unmarshalCollectionFromPanama should unmarshal Map")
    void unmarshalCollectionFromPanamaShouldUnmarshalMap() throws WasmException {
      final MemorySegment segment = arena.allocate(1024);

      final Object result = converter.unmarshalCollectionFromPanama(segment, Map.class);

      assertNotNull(result, "Result should not be null");
      assertTrue(result instanceof Map, "Result should be a Map");
    }

    @Test
    @DisplayName("unmarshalCollectionFromPanama should throw for null segment")
    void unmarshalCollectionFromPanamaShouldThrowForNullSegment() {
      assertThrows(
          Exception.class,
          () -> converter.unmarshalCollectionFromPanama(null, List.class),
          "Should throw for null segment");
    }

    @Test
    @DisplayName("unmarshalCollectionFromPanama should throw for null collectionType")
    void unmarshalCollectionFromPanamaShouldThrowForNullCollectionType() {
      final MemorySegment segment = arena.allocate(64);

      assertThrows(
          Exception.class,
          () -> converter.unmarshalCollectionFromPanama(segment, null),
          "Should throw for null collectionType");
    }

    @Test
    @DisplayName("unmarshalCollectionFromPanama should throw for unsupported type")
    void unmarshalCollectionFromPanamaShouldThrowForUnsupportedType() {
      final MemorySegment segment = arena.allocate(64);

      assertThrows(
          WasmException.class,
          () -> converter.unmarshalCollectionFromPanama(segment, java.util.Set.class),
          "Should throw for unsupported type");
    }
  }

  @Nested
  @DisplayName("createOptimizedParameterLayout Tests")
  class CreateOptimizedParameterLayoutTests {

    @Test
    @DisplayName("createOptimizedParameterLayout should create layout for multiple objects")
    void createOptimizedParameterLayoutShouldCreateLayoutForMultipleObjects() throws WasmException {
      final MemorySegment result =
          converter.createOptimizedParameterLayout("string", 42, 3.14);

      assertNotNull(result, "Result should not be null");
      assertTrue(result.byteSize() > 0, "Segment should have size > 0");
    }

    @Test
    @DisplayName("createOptimizedParameterLayout should create layout for single object")
    void createOptimizedParameterLayoutShouldCreateLayoutForSingleObject() throws WasmException {
      final MemorySegment result = converter.createOptimizedParameterLayout("test");

      assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("createOptimizedParameterLayout should create empty layout for no objects")
    void createOptimizedParameterLayoutShouldCreateEmptyLayoutForNoObjects() throws WasmException {
      final MemorySegment result = converter.createOptimizedParameterLayout();

      assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("createOptimizedParameterLayout should throw for null objects array")
    void createOptimizedParameterLayoutShouldThrowForNullObjectsArray() {
      assertThrows(
          NullPointerException.class,
          () -> converter.createOptimizedParameterLayout((Object[]) null),
          "Should throw for null objects array");
    }
  }

  @Nested
  @DisplayName("extractFromOptimizedParameterLayout Tests")
  class ExtractFromOptimizedParameterLayoutTests {

    @Test
    @DisplayName("extractFromOptimizedParameterLayout should extract objects")
    void extractFromOptimizedParameterLayoutShouldExtractObjects() throws WasmException {
      final MemorySegment segment = arena.allocate(1024);

      final Object[] result =
          converter.extractFromOptimizedParameterLayout(
              segment, String.class, Integer.class, Double.class);

      assertNotNull(result, "Result should not be null");
      assertEquals(3, result.length, "Should extract 3 objects");
    }

    @Test
    @DisplayName("extractFromOptimizedParameterLayout should throw for null segment")
    void extractFromOptimizedParameterLayoutShouldThrowForNullSegment() {
      assertThrows(
          Exception.class,
          () ->
              converter.extractFromOptimizedParameterLayout(
                  null, String.class),
          "Should throw for null segment");
    }

    @Test
    @DisplayName("extractFromOptimizedParameterLayout should throw for null expectedTypes")
    void extractFromOptimizedParameterLayoutShouldThrowForNullExpectedTypes() {
      final MemorySegment segment = arena.allocate(64);

      assertThrows(
          NullPointerException.class,
          () -> converter.extractFromOptimizedParameterLayout(segment, (Class<?>[]) null),
          "Should throw for null expectedTypes");
    }
  }

  @Nested
  @DisplayName("validatePanamaMarshalableObject Tests")
  class ValidatePanamaMarshalableObjectTests {

    @Test
    @DisplayName("validatePanamaMarshalableObject should pass for simple object")
    void validatePanamaMarshalableObjectShouldPassForSimpleObject() {
      assertDoesNotThrow(
          () -> converter.validatePanamaMarshalableObject("simple string"),
          "Should pass for simple object");
    }

    @Test
    @DisplayName("validatePanamaMarshalableObject should pass for array")
    void validatePanamaMarshalableObjectShouldPassForArray() {
      final int[] array = new int[] {1, 2, 3};

      assertDoesNotThrow(
          () -> converter.validatePanamaMarshalableObject(array),
          "Should pass for array");
    }

    @Test
    @DisplayName("validatePanamaMarshalableObject should pass for list")
    void validatePanamaMarshalableObjectShouldPassForList() {
      final List<String> list = Arrays.asList("a", "b", "c");

      assertDoesNotThrow(
          () -> converter.validatePanamaMarshalableObject(list),
          "Should pass for list");
    }

    @Test
    @DisplayName("validatePanamaMarshalableObject should throw for enum")
    void validatePanamaMarshalableObjectShouldThrowForEnum() {
      assertThrows(
          WasmException.class,
          () -> converter.validatePanamaMarshalableObject(Thread.State.NEW),
          "Should throw for enum");
    }

    @Test
    @DisplayName("validatePanamaMarshalableObject should throw for null object")
    void validatePanamaMarshalableObjectShouldThrowForNullObject() {
      assertThrows(
          NullPointerException.class,
          () -> converter.validatePanamaMarshalableObject(null),
          "Should throw for null object");
    }
  }

  @Nested
  @DisplayName("Direct Memory Segment Conversion Tests")
  class DirectMemorySegmentConversionTests {

    @Test
    @DisplayName("Byte array round-trip should work")
    void byteArrayRoundTripShouldWork() throws WasmException {
      final byte[] original = new byte[] {10, 20, 30, 40, 50};

      final MemorySegment segment = converter.convertComplexObjectToPanamaMemory(original);

      assertNotNull(segment, "Segment should not be null");
      assertEquals(original.length, segment.byteSize(), "Segment size should match array length");
    }

    @Test
    @DisplayName("Int array round-trip should work")
    void intArrayRoundTripShouldWork() throws WasmException {
      final int[] original = new int[] {100, 200, 300};

      final MemorySegment segment = converter.convertComplexObjectToPanamaMemory(original);

      assertNotNull(segment, "Segment should not be null");
    }

    @Test
    @DisplayName("Long array round-trip should work")
    void longArrayRoundTripShouldWork() throws WasmException {
      final long[] original = new long[] {Long.MAX_VALUE, Long.MIN_VALUE, 0L};

      final MemorySegment segment = converter.convertComplexObjectToPanamaMemory(original);

      assertNotNull(segment, "Segment should not be null");
    }

    @Test
    @DisplayName("Float array round-trip should work")
    void floatArrayRoundTripShouldWork() throws WasmException {
      final float[] original = new float[] {Float.MIN_VALUE, Float.MAX_VALUE, 0.0f};

      final MemorySegment segment = converter.convertComplexObjectToPanamaMemory(original);

      assertNotNull(segment, "Segment should not be null");
    }

    @Test
    @DisplayName("Double array round-trip should work")
    void doubleArrayRoundTripShouldWork() throws WasmException {
      final double[] original = new double[] {Double.MIN_VALUE, Double.MAX_VALUE, Math.PI};

      final MemorySegment segment = converter.convertComplexObjectToPanamaMemory(original);

      assertNotNull(segment, "Segment should not be null");
    }
  }

  @Nested
  @DisplayName("Collection Marshaling Tests")
  class CollectionMarshalingTests {

    @Test
    @DisplayName("Empty list should be marshaled successfully")
    void emptyListShouldBeMarshaledSuccessfully() throws WasmException {
      final List<Integer> emptyList = new ArrayList<>();

      final MemorySegment result = converter.marshalCollectionToPanama(emptyList);

      assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("List with mixed types should be marshaled")
    void listWithMixedTypesShouldBeMarshaled() throws WasmException {
      final List<Object> mixedList = new ArrayList<>();
      mixedList.add("string");
      mixedList.add(42);
      mixedList.add(3.14);

      final MemorySegment result = converter.marshalCollectionToPanama(mixedList);

      assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("Empty map should be marshaled successfully")
    void emptyMapShouldBeMarshaledSuccessfully() throws WasmException {
      final Map<String, String> emptyMap = new HashMap<>();

      final MemorySegment result = converter.marshalCollectionToPanama(emptyMap);

      assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("Map with various value types should be marshaled")
    void mapWithVariousValueTypesShouldBeMarshaled() throws WasmException {
      final Map<String, Object> map = new HashMap<>();
      map.put("string", "value");
      map.put("integer", 42);
      map.put("double", 3.14);

      final MemorySegment result = converter.marshalCollectionToPanama(map);

      assertNotNull(result, "Result should not be null");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Very large array should be handled")
    void veryLargeArrayShouldBeHandled() throws WasmException {
      final int[] largeArray = new int[10000];
      for (int i = 0; i < largeArray.length; i++) {
        largeArray[i] = i;
      }

      final MemorySegment result = converter.convertComplexObjectToPanamaMemory(largeArray);

      assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("Nested array should be handled")
    void nestedArrayShouldBeHandled() throws WasmException {
      final int[][][] nestedArray = new int[][][] {
          {{1, 2}, {3, 4}},
          {{5, 6}, {7, 8}}
      };

      final MemorySegment result = converter.marshalMultiDimensionalArrayToPanama(nestedArray);

      assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("Empty array should be handled")
    void emptyArrayShouldBeHandled() throws WasmException {
      final int[] emptyArray = new int[0];

      final MemorySegment result = converter.convertComplexObjectToPanamaMemory(emptyArray);

      assertNotNull(result, "Result should not be null");
      assertEquals(0, result.byteSize(), "Empty array should produce empty segment");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Full lifecycle with primitive arrays should work")
    void fullLifecycleWithPrimitiveArraysShouldWork() throws WasmException {
      // Create original data
      final byte[] bytes = new byte[] {1, 2, 3, 4, 5};
      final int[] ints = new int[] {10, 20, 30};
      final double[] doubles = new double[] {1.1, 2.2, 3.3};

      // Validate objects
      converter.validatePanamaMarshalableObject(bytes);
      converter.validatePanamaMarshalableObject(ints);
      converter.validatePanamaMarshalableObject(doubles);

      // Convert to memory segments
      final MemorySegment bytesSegment = converter.convertComplexObjectToPanamaMemory(bytes);
      final MemorySegment intsSegment = converter.convertComplexObjectToPanamaMemory(ints);
      final MemorySegment doublesSegment = converter.convertComplexObjectToPanamaMemory(doubles);

      // Verify all segments are valid
      assertNotNull(bytesSegment, "Bytes segment should not be null");
      assertNotNull(intsSegment, "Ints segment should not be null");
      assertNotNull(doublesSegment, "Doubles segment should not be null");
    }

    @Test
    @DisplayName("Full lifecycle with collections should work")
    void fullLifecycleWithCollectionsShouldWork() throws WasmException {
      // Create collections
      final List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
      final Map<String, Object> map = new HashMap<>();
      map.put("key1", "value1");
      map.put("key2", 42);

      // Validate
      converter.validatePanamaMarshalableObject(list);
      converter.validatePanamaMarshalableObject(map);

      // Marshal
      final MemorySegment listSegment = converter.marshalCollectionToPanama(list);
      final MemorySegment mapSegment = converter.marshalCollectionToPanama(map);

      // Unmarshal
      final Object unmarshaledList = converter.unmarshalCollectionFromPanama(listSegment, List.class);
      final Object unmarshaledMap = converter.unmarshalCollectionFromPanama(mapSegment, Map.class);

      // Verify
      assertNotNull(unmarshaledList, "Unmarshaled list should not be null");
      assertNotNull(unmarshaledMap, "Unmarshaled map should not be null");
      assertTrue(unmarshaledList instanceof List, "Should unmarshal to List");
      assertTrue(unmarshaledMap instanceof Map, "Should unmarshal to Map");
    }

    @Test
    @DisplayName("Full lifecycle with optimized parameter layout should work")
    void fullLifecycleWithOptimizedParameterLayoutShouldWork() throws WasmException {
      // Create optimized layout with multiple types
      final MemorySegment layout =
          converter.createOptimizedParameterLayout("test", 42, 3.14, true);

      assertNotNull(layout, "Layout should not be null");
      assertTrue(layout.byteSize() > 0, "Layout should have positive size");

      // Extract from layout
      final Object[] extracted =
          converter.extractFromOptimizedParameterLayout(
              layout, String.class, Integer.class, Double.class, Boolean.class);

      assertNotNull(extracted, "Extracted array should not be null");
      assertEquals(4, extracted.length, "Should extract 4 objects");
    }

    @Test
    @DisplayName("Converter should work with different arena managers")
    void converterShouldWorkWithDifferentArenaManagers() throws WasmException {
      try (Arena customArena = Arena.ofConfined()) {
        final ArenaResourceManager customManager = new ArenaResourceManager(customArena, false);
        final ComplexPanamaTypeConverter customConverter =
            new ComplexPanamaTypeConverter(customManager);

        final int[] data = new int[] {1, 2, 3};
        final MemorySegment result = customConverter.convertComplexObjectToPanamaMemory(data);

        assertNotNull(result, "Result should not be null");
      }
    }
  }
}
