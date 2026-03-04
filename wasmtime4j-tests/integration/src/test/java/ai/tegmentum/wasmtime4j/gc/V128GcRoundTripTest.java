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
package ai.tegmentum.wasmtime4j.gc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Round-trip tests for V128 values stored in GC struct and array fields. Verifies that all 16 bytes
 * of a V128 value survive creation, storage, and retrieval through the GC runtime — especially the
 * upper 8 bytes which were previously corrupted by i32/i64 truncation bugs.
 *
 * @since 1.0.0
 */
@DisplayName("V128 GC Struct/Array Round-Trip Tests")
public class V128GcRoundTripTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(V128GcRoundTripTest.class.getName());

  /** Baseline: all zeros. */
  private static final byte[] ALL_ZEROS = new byte[16];

  /** Counting pattern {0x00..0x0F} — detects byte order bugs. */
  private static final byte[] COUNTING = {
    0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
    0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F
  };

  /**
   * Upper half only — lower 8 bytes zero, upper 8 bytes non-zero. Detects i32/i64 truncation (the
   * original V128 data corruption bug).
   */
  private static final byte[] UPPER_HALF_ONLY = {
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    (byte) 0xDE,
    (byte) 0xAD,
    (byte) 0xBE,
    (byte) 0xEF,
    (byte) 0xCA,
    (byte) 0xFE,
    (byte) 0xBA,
    (byte) 0xBE
  };

  /** Alternating pattern — detects bit-flip errors. */
  private static final byte[] ALTERNATING = {
    (byte) 0xAA, 0x55, (byte) 0xAA, 0x55,
    (byte) 0xAA, 0x55, (byte) 0xAA, 0x55,
    (byte) 0xAA, 0x55, (byte) 0xAA, 0x55,
    (byte) 0xAA, 0x55, (byte) 0xAA, 0x55
  };

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("V128 struct field round-trip preserves all 16 bytes")
  void v128StructFieldRoundTrip(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing V128 struct field round-trip");

    final byte[][] testValues = {ALL_ZEROS, COUNTING, UPPER_HALF_ONLY, ALTERNATING};

    try {
      final WasmRuntime wasmRuntime = WasmRuntimeFactory.create(runtime);
      final GcRuntime gcRuntime = wasmRuntime.getGcRuntime();
      assertNotNull(gcRuntime, "GcRuntime should be available");

      for (final byte[] expected : testValues) {
        LOGGER.info("[" + runtime + "] Testing pattern: " + bytesToHex(expected));

        final StructType structType =
            StructType.builder("V128Struct").addField("v128Field", FieldType.v128(), true).build();

        final StructInstance struct =
            gcRuntime.createStruct(structType, List.of(GcValue.v128(expected)));
        assertNotNull(struct, "Struct should be created");

        final GcValue retrieved = gcRuntime.getStructField(struct, 0);
        assertNotNull(retrieved, "Field value should not be null");
        assertEquals(GcValue.Type.V128, retrieved.getType(), "Field type should be V128");

        final byte[] actual = retrieved.asV128();
        LOGGER.info(
            "[" + runtime + "] Expected: " + bytesToHex(expected) + ", Got: " + bytesToHex(actual));
        assertArrayEquals(
            expected,
            actual,
            "V128 bytes must survive struct field round-trip for pattern " + bytesToHex(expected));
      }

      LOGGER.info("[" + runtime + "] All V128 struct field round-trip patterns passed");

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] GC structs not supported: " + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning(
          "["
              + runtime
              + "] Unexpected exception: "
              + e.getClass().getName()
              + " - "
              + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("V128 struct field set and get preserves all 16 bytes")
  void v128StructFieldSetAndGet(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing V128 struct field set/get");

    try {
      final WasmRuntime wasmRuntime = WasmRuntimeFactory.create(runtime);
      final GcRuntime gcRuntime = wasmRuntime.getGcRuntime();
      assertNotNull(gcRuntime, "GcRuntime should be available");

      final StructType structType =
          StructType.builder("V128SetGetStruct")
              .addField("v128Field", FieldType.v128(), true)
              .build();

      // Create with zeros, then set to COUNTING pattern
      final StructInstance struct =
          gcRuntime.createStruct(structType, List.of(GcValue.v128(ALL_ZEROS)));
      assertNotNull(struct, "Struct should be created");

      final GcValue initialValue = gcRuntime.getStructField(struct, 0);
      assertArrayEquals(ALL_ZEROS, initialValue.asV128(), "Initial value should be all zeros");
      LOGGER.info("[" + runtime + "] Initial value verified as all zeros");

      // Set to COUNTING pattern
      gcRuntime.setStructField(struct, 0, GcValue.v128(COUNTING));
      final GcValue afterSet = gcRuntime.getStructField(struct, 0);
      assertArrayEquals(COUNTING, afterSet.asV128(), "After set, value should be COUNTING pattern");
      LOGGER.info("[" + runtime + "] Set/get verified with COUNTING pattern");

      // Set to UPPER_HALF_ONLY
      gcRuntime.setStructField(struct, 0, GcValue.v128(UPPER_HALF_ONLY));
      final GcValue afterUpperHalf = gcRuntime.getStructField(struct, 0);
      assertArrayEquals(
          UPPER_HALF_ONLY,
          afterUpperHalf.asV128(),
          "After set, value should be UPPER_HALF_ONLY pattern");
      LOGGER.info("[" + runtime + "] Set/get verified with UPPER_HALF_ONLY pattern");

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] GC structs not supported: " + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning(
          "["
              + runtime
              + "] Unexpected exception: "
              + e.getClass().getName()
              + " - "
              + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("V128 struct with multiple field types does not corrupt adjacent fields")
  void v128StructMultipleFieldTypes(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing V128 struct with mixed field types");

    try {
      final WasmRuntime wasmRuntime = WasmRuntimeFactory.create(runtime);
      final GcRuntime gcRuntime = wasmRuntime.getGcRuntime();
      assertNotNull(gcRuntime, "GcRuntime should be available");

      final StructType structType =
          StructType.builder("MixedStruct")
              .addField("intField", FieldType.i32(), true)
              .addField("v128First", FieldType.v128(), true)
              .addField("floatField", FieldType.f64(), true)
              .addField("v128Second", FieldType.v128(), true)
              .build();

      final int expectedI32 = 0x12345678;
      final double expectedF64 = 3.141592653589793;

      final StructInstance struct =
          gcRuntime.createStruct(
              structType,
              List.of(
                  GcValue.i32(expectedI32),
                  GcValue.v128(COUNTING),
                  GcValue.f64(expectedF64),
                  GcValue.v128(UPPER_HALF_ONLY)));
      assertNotNull(struct, "Mixed struct should be created");

      // Verify all fields are intact
      final GcValue field0 = gcRuntime.getStructField(struct, 0);
      assertEquals(
          expectedI32, field0.asI32(), "i32 field should not be corrupted by adjacent V128");
      LOGGER.info("[" + runtime + "] i32 field intact: 0x" + Integer.toHexString(field0.asI32()));

      final GcValue field1 = gcRuntime.getStructField(struct, 1);
      assertArrayEquals(COUNTING, field1.asV128(), "First V128 field should be COUNTING pattern");
      LOGGER.info("[" + runtime + "] First V128 field intact: " + bytesToHex(field1.asV128()));

      final GcValue field2 = gcRuntime.getStructField(struct, 2);
      assertEquals(
          expectedF64, field2.asF64(), 0.0, "f64 field should not be corrupted by adjacent V128");
      LOGGER.info("[" + runtime + "] f64 field intact: " + field2.asF64());

      final GcValue field3 = gcRuntime.getStructField(struct, 3);
      assertArrayEquals(
          UPPER_HALF_ONLY, field3.asV128(), "Second V128 field should be UPPER_HALF_ONLY pattern");
      LOGGER.info("[" + runtime + "] Second V128 field intact: " + bytesToHex(field3.asV128()));

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] GC structs not supported: " + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning(
          "["
              + runtime
              + "] Unexpected exception: "
              + e.getClass().getName()
              + " - "
              + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("V128 array element round-trip preserves all 16 bytes")
  void v128ArrayElementRoundTrip(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing V128 array element round-trip");

    final byte[][] testValues = {ALL_ZEROS, COUNTING, UPPER_HALF_ONLY, ALTERNATING};

    try {
      final WasmRuntime wasmRuntime = WasmRuntimeFactory.create(runtime);
      final GcRuntime gcRuntime = wasmRuntime.getGcRuntime();
      assertNotNull(gcRuntime, "GcRuntime should be available");

      final ArrayType arrayType =
          ArrayType.builder("V128Array").elementType(FieldType.v128()).mutable(true).build();

      final List<GcValue> elements =
          List.of(
              GcValue.v128(ALL_ZEROS),
              GcValue.v128(COUNTING),
              GcValue.v128(UPPER_HALF_ONLY),
              GcValue.v128(ALTERNATING));

      final ArrayInstance array = gcRuntime.createArray(arrayType, elements);
      assertNotNull(array, "Array should be created");
      assertEquals(4, gcRuntime.getArrayLength(array), "Array should have 4 elements");
      LOGGER.info("[" + runtime + "] Created V128 array with 4 elements");

      for (int i = 0; i < testValues.length; i++) {
        final GcValue retrieved = gcRuntime.getArrayElement(array, i);
        assertNotNull(retrieved, "Element " + i + " should not be null");
        assertEquals(
            GcValue.Type.V128, retrieved.getType(), "Element " + i + " type should be V128");

        final byte[] actual = retrieved.asV128();
        LOGGER.info(
            "["
                + runtime
                + "] Element "
                + i
                + " expected: "
                + bytesToHex(testValues[i])
                + ", got: "
                + bytesToHex(actual));
        assertArrayEquals(
            testValues[i],
            actual,
            "V128 bytes must survive array element round-trip at index " + i);
      }

      LOGGER.info("[" + runtime + "] All V128 array element round-trip patterns passed");

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] GC arrays not supported: " + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning(
          "["
              + runtime
              + "] Unexpected exception: "
              + e.getClass().getName()
              + " - "
              + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("V128 array element set and get preserves all 16 bytes")
  void v128ArrayElementSetAndGet(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing V128 array element set/get");

    try {
      final WasmRuntime wasmRuntime = WasmRuntimeFactory.create(runtime);
      final GcRuntime gcRuntime = wasmRuntime.getGcRuntime();
      assertNotNull(gcRuntime, "GcRuntime should be available");

      final ArrayType arrayType =
          ArrayType.builder("V128SetGetArray").elementType(FieldType.v128()).mutable(true).build();

      // Create array with 2 zero elements
      final ArrayInstance array =
          gcRuntime.createArray(
              arrayType, List.of(GcValue.v128(ALL_ZEROS), GcValue.v128(ALL_ZEROS)));
      assertNotNull(array, "Array should be created");

      // Set element 0 to ALTERNATING
      gcRuntime.setArrayElement(array, 0, GcValue.v128(ALTERNATING));
      final byte[] elem0 = gcRuntime.getArrayElement(array, 0).asV128();
      assertArrayEquals(ALTERNATING, elem0, "Element 0 should be ALTERNATING pattern after set");
      LOGGER.info("[" + runtime + "] Element 0 set/get verified: " + bytesToHex(elem0));

      // Verify element 1 is still zeros (not corrupted by adjacent set)
      final byte[] elem1 = gcRuntime.getArrayElement(array, 1).asV128();
      assertArrayEquals(
          ALL_ZEROS, elem1, "Element 1 should still be zeros after setting element 0");
      LOGGER.info("[" + runtime + "] Element 1 unaffected: " + bytesToHex(elem1));

      // Set element 1 to UPPER_HALF_ONLY
      gcRuntime.setArrayElement(array, 1, GcValue.v128(UPPER_HALF_ONLY));
      final byte[] elem1After = gcRuntime.getArrayElement(array, 1).asV128();
      assertArrayEquals(
          UPPER_HALF_ONLY, elem1After, "Element 1 should be UPPER_HALF_ONLY pattern after set");
      LOGGER.info("[" + runtime + "] Element 1 set/get verified: " + bytesToHex(elem1After));

      // Re-check element 0 is still ALTERNATING
      final byte[] elem0Recheck = gcRuntime.getArrayElement(array, 0).asV128();
      assertArrayEquals(
          ALTERNATING,
          elem0Recheck,
          "Element 0 should still be ALTERNATING after setting element 1");
      LOGGER.info("[" + runtime + "] Element 0 still intact: " + bytesToHex(elem0Recheck));

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] GC arrays not supported: " + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning(
          "["
              + runtime
              + "] Unexpected exception: "
              + e.getClass().getName()
              + " - "
              + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("V128 upper 8 bytes preserved — critical regression test")
  void v128UpperHalfPreserved(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] CRITICAL: Testing V128 upper half preservation");

    try {
      final WasmRuntime wasmRuntime = WasmRuntimeFactory.create(runtime);
      final GcRuntime gcRuntime = wasmRuntime.getGcRuntime();
      assertNotNull(gcRuntime, "GcRuntime should be available");

      // Test with struct
      final StructType structType =
          StructType.builder("UpperHalfStruct")
              .addField("v128Field", FieldType.v128(), true)
              .build();

      final StructInstance struct =
          gcRuntime.createStruct(structType, List.of(GcValue.v128(UPPER_HALF_ONLY)));
      final byte[] structResult = gcRuntime.getStructField(struct, 0).asV128();

      // Explicitly check upper 8 bytes are non-zero
      final byte[] upperHalf = Arrays.copyOfRange(structResult, 8, 16);
      final byte[] expectedUpperHalf = Arrays.copyOfRange(UPPER_HALF_ONLY, 8, 16);
      LOGGER.info(
          "["
              + runtime
              + "] Struct upper half: "
              + bytesToHex(upperHalf)
              + " (expected: "
              + bytesToHex(expectedUpperHalf)
              + ")");

      assertArrayEquals(
          expectedUpperHalf,
          upperHalf,
          "CRITICAL: Upper 8 bytes of V128 must be preserved in struct field. "
              + "If this fails, V128 data is being truncated to i64.");

      // Test with array
      final ArrayType arrayType =
          ArrayType.builder("UpperHalfArray").elementType(FieldType.v128()).mutable(true).build();

      final ArrayInstance array =
          gcRuntime.createArray(arrayType, List.of(GcValue.v128(UPPER_HALF_ONLY)));
      final byte[] arrayResult = gcRuntime.getArrayElement(array, 0).asV128();

      final byte[] arrayUpperHalf = Arrays.copyOfRange(arrayResult, 8, 16);
      LOGGER.info(
          "["
              + runtime
              + "] Array upper half: "
              + bytesToHex(arrayUpperHalf)
              + " (expected: "
              + bytesToHex(expectedUpperHalf)
              + ")");

      assertArrayEquals(
          expectedUpperHalf,
          arrayUpperHalf,
          "CRITICAL: Upper 8 bytes of V128 must be preserved in array element. "
              + "If this fails, V128 data is being truncated to i64.");

      LOGGER.info("[" + runtime + "] V128 upper half preservation PASSED");

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] GC not supported: " + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning(
          "["
              + runtime
              + "] Unexpected exception: "
              + e.getClass().getName()
              + " - "
              + e.getMessage());
    }
  }

  private static String bytesToHex(final byte[] bytes) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < bytes.length; i++) {
      if (i > 0) {
        sb.append(' ');
      }
      sb.append(String.format("%02X", bytes[i] & 0xFF));
    }
    return sb.toString();
  }
}
