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
package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasmValueMarshaller}.
 *
 * <p>Validates round-trip marshalling/unmarshalling of all Wasm value types through the native
 * tagged union layout (20 bytes per value: 4-byte tag + 16-byte union).
 */
@DisplayName("WasmValueMarshaller Tests")
class WasmValueMarshallerTest {

  private static final Logger LOGGER = Logger.getLogger(WasmValueMarshallerTest.class.getName());

  /** Tag constants matching WasmValueMarshaller's internal encoding. */
  private static final int TAG_I32 = 0;

  private static final int TAG_I64 = 1;
  private static final int TAG_F32 = 2;
  private static final int TAG_F64 = 3;
  private static final int TAG_V128 = 4;
  private static final int TAG_FUNCREF = 5;
  private static final int TAG_EXTERNREF = 6;
  private static final int TAG_CONTREF = 7;

  @Nested
  @DisplayName("I32 Round-Trip")
  class I32RoundTrip {

    @Test
    @DisplayName("Round-trip i32(42)")
    void testI32Basic() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment buffer = arena.allocate(WasmValueMarshaller.WASM_VALUE_SIZE);
        final WasmValue original = WasmValue.i32(42);

        WasmValueMarshaller.marshalWasmValue(original, buffer, 0, null);
        final WasmValue result = WasmValueMarshaller.unmarshalWasmValue(buffer, 0, null);

        assertEquals(WasmValueType.I32, result.getType(), "Type should be I32");
        assertEquals(42, result.asInt(), "Value should be 42");
        LOGGER.info("I32 round-trip: 42 -> " + result.asInt());
      }
    }

    @Test
    @DisplayName("Round-trip i32(-1)")
    void testI32Negative() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment buffer = arena.allocate(WasmValueMarshaller.WASM_VALUE_SIZE);
        final WasmValue original = WasmValue.i32(-1);

        WasmValueMarshaller.marshalWasmValue(original, buffer, 0, null);
        final WasmValue result = WasmValueMarshaller.unmarshalWasmValue(buffer, 0, null);

        assertEquals(-1, result.asInt(), "Value should be -1");
      }
    }

    @Test
    @DisplayName("Round-trip i32(Integer.MIN_VALUE)")
    void testI32MinValue() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment buffer = arena.allocate(WasmValueMarshaller.WASM_VALUE_SIZE);
        final WasmValue original = WasmValue.i32(Integer.MIN_VALUE);

        WasmValueMarshaller.marshalWasmValue(original, buffer, 0, null);
        final WasmValue result = WasmValueMarshaller.unmarshalWasmValue(buffer, 0, null);

        assertEquals(Integer.MIN_VALUE, result.asInt(), "Value should be Integer.MIN_VALUE");
      }
    }

    @Test
    @DisplayName("Round-trip i32(Integer.MAX_VALUE)")
    void testI32MaxValue() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment buffer = arena.allocate(WasmValueMarshaller.WASM_VALUE_SIZE);
        final WasmValue original = WasmValue.i32(Integer.MAX_VALUE);

        WasmValueMarshaller.marshalWasmValue(original, buffer, 0, null);
        final WasmValue result = WasmValueMarshaller.unmarshalWasmValue(buffer, 0, null);

        assertEquals(Integer.MAX_VALUE, result.asInt(), "Value should be Integer.MAX_VALUE");
      }
    }
  }

  @Nested
  @DisplayName("I64 Round-Trip")
  class I64RoundTrip {

    @Test
    @DisplayName("Round-trip i64(Long.MAX_VALUE)")
    void testI64MaxValue() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment buffer = arena.allocate(WasmValueMarshaller.WASM_VALUE_SIZE);
        final WasmValue original = WasmValue.i64(Long.MAX_VALUE);

        WasmValueMarshaller.marshalWasmValue(original, buffer, 0, null);
        final WasmValue result = WasmValueMarshaller.unmarshalWasmValue(buffer, 0, null);

        assertEquals(WasmValueType.I64, result.getType(), "Type should be I64");
        assertEquals(Long.MAX_VALUE, result.asLong(), "Value should be Long.MAX_VALUE");
        LOGGER.info("I64 round-trip: MAX_VALUE -> " + result.asLong());
      }
    }

    @Test
    @DisplayName("Round-trip i64(-123456789L)")
    void testI64Negative() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment buffer = arena.allocate(WasmValueMarshaller.WASM_VALUE_SIZE);
        final WasmValue original = WasmValue.i64(-123456789L);

        WasmValueMarshaller.marshalWasmValue(original, buffer, 0, null);
        final WasmValue result = WasmValueMarshaller.unmarshalWasmValue(buffer, 0, null);

        assertEquals(-123456789L, result.asLong(), "Value should be -123456789");
      }
    }

    @Test
    @DisplayName("Round-trip i64(0)")
    void testI64Zero() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment buffer = arena.allocate(WasmValueMarshaller.WASM_VALUE_SIZE);
        final WasmValue original = WasmValue.i64(0L);

        WasmValueMarshaller.marshalWasmValue(original, buffer, 0, null);
        final WasmValue result = WasmValueMarshaller.unmarshalWasmValue(buffer, 0, null);

        assertEquals(0L, result.asLong(), "Value should be 0");
      }
    }
  }

  @Nested
  @DisplayName("F32 Round-Trip")
  class F32RoundTrip {

    @Test
    @DisplayName("Round-trip f32(3.14f)")
    void testF32Basic() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment buffer = arena.allocate(WasmValueMarshaller.WASM_VALUE_SIZE);
        final WasmValue original = WasmValue.f32(3.14f);

        WasmValueMarshaller.marshalWasmValue(original, buffer, 0, null);
        final WasmValue result = WasmValueMarshaller.unmarshalWasmValue(buffer, 0, null);

        assertEquals(WasmValueType.F32, result.getType(), "Type should be F32");
        assertEquals(3.14f, result.asFloat(), 0.001f, "Value should be approximately 3.14");
        LOGGER.info("F32 round-trip: 3.14 -> " + result.asFloat());
      }
    }

    @Test
    @DisplayName("Round-trip f32(Float.NaN)")
    void testF32Nan() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment buffer = arena.allocate(WasmValueMarshaller.WASM_VALUE_SIZE);
        final WasmValue original = WasmValue.f32(Float.NaN);

        WasmValueMarshaller.marshalWasmValue(original, buffer, 0, null);
        final WasmValue result = WasmValueMarshaller.unmarshalWasmValue(buffer, 0, null);

        assertTrue(Float.isNaN(result.asFloat()), "Value should be NaN");
      }
    }

    @Test
    @DisplayName("Round-trip f32(Float.POSITIVE_INFINITY)")
    void testF32Infinity() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment buffer = arena.allocate(WasmValueMarshaller.WASM_VALUE_SIZE);
        final WasmValue original = WasmValue.f32(Float.POSITIVE_INFINITY);

        WasmValueMarshaller.marshalWasmValue(original, buffer, 0, null);
        final WasmValue result = WasmValueMarshaller.unmarshalWasmValue(buffer, 0, null);

        assertEquals(
            Float.POSITIVE_INFINITY, result.asFloat(), "Value should be POSITIVE_INFINITY");
      }
    }

    @Test
    @DisplayName("Round-trip f32(-0.0f)")
    void testF32NegativeZero() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment buffer = arena.allocate(WasmValueMarshaller.WASM_VALUE_SIZE);
        final WasmValue original = WasmValue.f32(-0.0f);

        WasmValueMarshaller.marshalWasmValue(original, buffer, 0, null);
        final WasmValue result = WasmValueMarshaller.unmarshalWasmValue(buffer, 0, null);

        assertEquals(
            Float.floatToRawIntBits(-0.0f),
            Float.floatToRawIntBits(result.asFloat()),
            "Should preserve negative zero bit pattern");
      }
    }
  }

  @Nested
  @DisplayName("F64 Round-Trip")
  class F64RoundTrip {

    @Test
    @DisplayName("Round-trip f64(Math.PI)")
    void testF64Pi() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment buffer = arena.allocate(WasmValueMarshaller.WASM_VALUE_SIZE);
        final WasmValue original = WasmValue.f64(Math.PI);

        WasmValueMarshaller.marshalWasmValue(original, buffer, 0, null);
        final WasmValue result = WasmValueMarshaller.unmarshalWasmValue(buffer, 0, null);

        assertEquals(WasmValueType.F64, result.getType(), "Type should be F64");
        assertEquals(Math.PI, result.asDouble(), 0.0, "Value should be exactly Math.PI");
        LOGGER.info("F64 round-trip: PI -> " + result.asDouble());
      }
    }

    @Test
    @DisplayName("Round-trip f64(Double.NaN)")
    void testF64Nan() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment buffer = arena.allocate(WasmValueMarshaller.WASM_VALUE_SIZE);
        final WasmValue original = WasmValue.f64(Double.NaN);

        WasmValueMarshaller.marshalWasmValue(original, buffer, 0, null);
        final WasmValue result = WasmValueMarshaller.unmarshalWasmValue(buffer, 0, null);

        assertTrue(Double.isNaN(result.asDouble()), "Value should be NaN");
      }
    }

    @Test
    @DisplayName("Round-trip f64(Double.MAX_VALUE)")
    void testF64MaxValue() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment buffer = arena.allocate(WasmValueMarshaller.WASM_VALUE_SIZE);
        final WasmValue original = WasmValue.f64(Double.MAX_VALUE);

        WasmValueMarshaller.marshalWasmValue(original, buffer, 0, null);
        final WasmValue result = WasmValueMarshaller.unmarshalWasmValue(buffer, 0, null);

        assertEquals(Double.MAX_VALUE, result.asDouble(), 0.0, "Value should be MAX_VALUE");
      }
    }
  }

  @Nested
  @DisplayName("V128 Round-Trip")
  class V128RoundTrip {

    @Test
    @DisplayName("Round-trip v128 with known byte pattern")
    void testV128KnownPattern() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment buffer = arena.allocate(WasmValueMarshaller.WASM_VALUE_SIZE);
        final byte[] pattern = new byte[16];
        for (int i = 0; i < 16; i++) {
          pattern[i] = (byte) (i * 17); // 0x00, 0x11, 0x22, ..., 0xFF
        }
        final WasmValue original = WasmValue.v128(pattern);

        WasmValueMarshaller.marshalWasmValue(original, buffer, 0, null);
        final WasmValue result = WasmValueMarshaller.unmarshalWasmValue(buffer, 0, null);

        assertEquals(WasmValueType.V128, result.getType(), "Type should be V128");
        final byte[] resultBytes = result.asV128();
        assertNotNull(resultBytes, "V128 bytes should not be null");
        assertEquals(16, resultBytes.length, "V128 should be 16 bytes");
        assertArrayEquals(pattern, resultBytes, "V128 bytes should match original pattern");
        LOGGER.info("V128 round-trip verified with 16-byte pattern");
      }
    }
  }

  @Nested
  @DisplayName("Reference Type Round-Trip")
  class RefTypeRoundTrip {

    @Test
    @DisplayName("Round-trip null funcref")
    void testNullFuncref() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment buffer = arena.allocate(WasmValueMarshaller.WASM_VALUE_SIZE);
        final WasmValue original = WasmValue.nullFuncref();

        WasmValueMarshaller.marshalWasmValue(original, buffer, 0, null);
        final WasmValue result = WasmValueMarshaller.unmarshalWasmValue(buffer, 0, null);

        assertEquals(WasmValueType.FUNCREF, result.getType(), "Type should be FUNCREF");
        assertTrue(result.getValue() == null, "Null funcref should be null");
        LOGGER.info("Null funcref round-trip verified");
      }
    }

    @Test
    @DisplayName("Round-trip null externref")
    void testNullExternref() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment buffer = arena.allocate(WasmValueMarshaller.WASM_VALUE_SIZE);
        final WasmValue original = WasmValue.nullExternref();

        WasmValueMarshaller.marshalWasmValue(original, buffer, 0, null);
        final WasmValue result = WasmValueMarshaller.unmarshalWasmValue(buffer, 0, null);

        assertEquals(WasmValueType.EXTERNREF, result.getType(), "Type should be EXTERNREF");
        assertTrue(result.getValue() == null, "Null externref should be null");
        LOGGER.info("Null externref round-trip verified");
      }
    }
  }

  @Nested
  @DisplayName("Multiple Values Sequential")
  class MultipleValues {

    @Test
    @DisplayName("Marshal 4 values at sequential indices and unmarshal all")
    void testMultipleValuesSequential() {
      try (Arena arena = Arena.ofConfined()) {
        final int count = 4;
        final MemorySegment buffer =
            arena.allocate((long) WasmValueMarshaller.WASM_VALUE_SIZE * count);

        final WasmValue[] originals = {
          WasmValue.i32(100), WasmValue.i64(200L), WasmValue.f32(3.0f), WasmValue.f64(4.0)
        };

        // Marshal all
        for (int i = 0; i < count; i++) {
          WasmValueMarshaller.marshalWasmValue(originals[i], buffer, i, null);
        }

        // Unmarshal and verify each
        final WasmValue r0 = WasmValueMarshaller.unmarshalWasmValue(buffer, 0, null);
        assertEquals(WasmValueType.I32, r0.getType(), "Index 0 type should be I32");
        assertEquals(100, r0.asInt(), "Index 0 value should be 100");

        final WasmValue r1 = WasmValueMarshaller.unmarshalWasmValue(buffer, 1, null);
        assertEquals(WasmValueType.I64, r1.getType(), "Index 1 type should be I64");
        assertEquals(200L, r1.asLong(), "Index 1 value should be 200");

        final WasmValue r2 = WasmValueMarshaller.unmarshalWasmValue(buffer, 2, null);
        assertEquals(WasmValueType.F32, r2.getType(), "Index 2 type should be F32");
        assertEquals(3.0f, r2.asFloat(), 0.001f, "Index 2 value should be 3.0");

        final WasmValue r3 = WasmValueMarshaller.unmarshalWasmValue(buffer, 3, null);
        assertEquals(WasmValueType.F64, r3.getType(), "Index 3 type should be F64");
        assertEquals(4.0, r3.asDouble(), 0.0, "Index 3 value should be 4.0");

        LOGGER.info("Multiple values sequential round-trip verified: 4 values at 4 indices");
      }
    }
  }

  @Nested
  @DisplayName("Tag Correctness")
  class TagCorrectness {

    @Test
    @DisplayName("I32 tag byte is correct")
    void testI32Tag() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment buffer = arena.allocate(WasmValueMarshaller.WASM_VALUE_SIZE);
        WasmValueMarshaller.marshalWasmValue(WasmValue.i32(0), buffer, 0, null);

        final int tag = buffer.get(ValueLayout.JAVA_INT, 0);
        assertEquals(TAG_I32, tag, "I32 tag should be " + TAG_I32);
      }
    }

    @Test
    @DisplayName("I64 tag byte is correct")
    void testI64Tag() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment buffer = arena.allocate(WasmValueMarshaller.WASM_VALUE_SIZE);
        WasmValueMarshaller.marshalWasmValue(WasmValue.i64(0L), buffer, 0, null);

        final int tag = buffer.get(ValueLayout.JAVA_INT, 0);
        assertEquals(TAG_I64, tag, "I64 tag should be " + TAG_I64);
      }
    }

    @Test
    @DisplayName("F32 tag byte is correct")
    void testF32Tag() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment buffer = arena.allocate(WasmValueMarshaller.WASM_VALUE_SIZE);
        WasmValueMarshaller.marshalWasmValue(WasmValue.f32(0.0f), buffer, 0, null);

        final int tag = buffer.get(ValueLayout.JAVA_INT, 0);
        assertEquals(TAG_F32, tag, "F32 tag should be " + TAG_F32);
      }
    }

    @Test
    @DisplayName("F64 tag byte is correct")
    void testF64Tag() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment buffer = arena.allocate(WasmValueMarshaller.WASM_VALUE_SIZE);
        WasmValueMarshaller.marshalWasmValue(WasmValue.f64(0.0), buffer, 0, null);

        final int tag = buffer.get(ValueLayout.JAVA_INT, 0);
        assertEquals(TAG_F64, tag, "F64 tag should be " + TAG_F64);
      }
    }

    @Test
    @DisplayName("V128 tag byte is correct")
    void testV128Tag() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment buffer = arena.allocate(WasmValueMarshaller.WASM_VALUE_SIZE);
        WasmValueMarshaller.marshalWasmValue(WasmValue.v128(new byte[16]), buffer, 0, null);

        final int tag = buffer.get(ValueLayout.JAVA_INT, 0);
        assertEquals(TAG_V128, tag, "V128 tag should be " + TAG_V128);
      }
    }

    @Test
    @DisplayName("FUNCREF tag byte is correct")
    void testFuncrefTag() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment buffer = arena.allocate(WasmValueMarshaller.WASM_VALUE_SIZE);
        WasmValueMarshaller.marshalWasmValue(WasmValue.nullFuncref(), buffer, 0, null);

        final int tag = buffer.get(ValueLayout.JAVA_INT, 0);
        assertEquals(TAG_FUNCREF, tag, "FUNCREF tag should be " + TAG_FUNCREF);
      }
    }

    @Test
    @DisplayName("EXTERNREF tag byte is correct")
    void testExternrefTag() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment buffer = arena.allocate(WasmValueMarshaller.WASM_VALUE_SIZE);
        WasmValueMarshaller.marshalWasmValue(WasmValue.nullExternref(), buffer, 0, null);

        final int tag = buffer.get(ValueLayout.JAVA_INT, 0);
        assertEquals(TAG_EXTERNREF, tag, "EXTERNREF tag should be " + TAG_EXTERNREF);
      }
    }
  }

  @Nested
  @DisplayName("Invalid Tag Handling")
  class InvalidTag {

    @Test
    @DisplayName("Invalid tag value causes exception on unmarshal")
    void testInvalidTagThrows() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment buffer = arena.allocate(WasmValueMarshaller.WASM_VALUE_SIZE);
        // Write an invalid tag value (255 is not a valid tag)
        buffer.set(ValueLayout.JAVA_INT, 0, 255);

        assertThrows(
            Exception.class,
            () -> WasmValueMarshaller.unmarshalWasmValue(buffer, 0, null),
            "Invalid tag should cause an exception during unmarshal");

        LOGGER.info("Invalid tag correctly causes exception");
      }
    }
  }
}
