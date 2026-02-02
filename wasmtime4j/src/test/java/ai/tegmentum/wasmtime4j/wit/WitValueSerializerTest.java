/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.wit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WitValueException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Comprehensive unit tests for WitValueSerializer. */
@DisplayName("WitValueSerializer Tests")
final class WitValueSerializerTest {

  @Test
  @DisplayName("Serialize bool true to binary format")
  void testSerializeBoolTrue() throws WitValueException {
    final WitBool value = WitBool.of(true);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(1, result.length, "Bool should serialize to 1 byte");
    assertEquals((byte) 1, result[0], "True should serialize to byte value 1");
  }

  @Test
  @DisplayName("Serialize bool false to binary format")
  void testSerializeBoolFalse() throws WitValueException {
    final WitBool value = WitBool.of(false);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(1, result.length, "Bool should serialize to 1 byte");
    assertEquals((byte) 0, result[0], "False should serialize to byte value 0");
  }

  @Test
  @DisplayName("Serialize s32 positive value to little-endian binary")
  void testSerializeS32Positive() throws WitValueException {
    final WitS32 value = WitS32.of(42);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(4, result.length, "S32 should serialize to 4 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(42, buffer.getInt(), "Should deserialize to original value");
  }

  @Test
  @DisplayName("Serialize s32 negative value to little-endian binary")
  void testSerializeS32Negative() throws WitValueException {
    final WitS32 value = WitS32.of(-999);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(4, result.length, "S32 should serialize to 4 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(-999, buffer.getInt(), "Should deserialize to original value");
  }

  @Test
  @DisplayName("Serialize s32 max value")
  void testSerializeS32Max() throws WitValueException {
    final WitS32 value = WitS32.of(Integer.MAX_VALUE);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(4, result.length, "S32 should serialize to 4 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(Integer.MAX_VALUE, buffer.getInt(), "Should preserve Integer.MAX_VALUE");
  }

  @Test
  @DisplayName("Serialize s32 min value")
  void testSerializeS32Min() throws WitValueException {
    final WitS32 value = WitS32.of(Integer.MIN_VALUE);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(4, result.length, "S32 should serialize to 4 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(Integer.MIN_VALUE, buffer.getInt(), "Should preserve Integer.MIN_VALUE");
  }

  @Test
  @DisplayName("Serialize s64 positive value to little-endian binary")
  void testSerializeS64Positive() throws WitValueException {
    final WitS64 value = WitS64.of(1_000_000_000_000L);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(8, result.length, "S64 should serialize to 8 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(1_000_000_000_000L, buffer.getLong(), "Should deserialize to original value");
  }

  @Test
  @DisplayName("Serialize s64 negative value to little-endian binary")
  void testSerializeS64Negative() throws WitValueException {
    final WitS64 value = WitS64.of(-9_999_999_999L);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(8, result.length, "S64 should serialize to 8 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(-9_999_999_999L, buffer.getLong(), "Should deserialize to original value");
  }

  @Test
  @DisplayName("Serialize s64 max value")
  void testSerializeS64Max() throws WitValueException {
    final WitS64 value = WitS64.of(Long.MAX_VALUE);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(8, result.length, "S64 should serialize to 8 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(Long.MAX_VALUE, buffer.getLong(), "Should preserve Long.MAX_VALUE");
  }

  @Test
  @DisplayName("Serialize s64 min value")
  void testSerializeS64Min() throws WitValueException {
    final WitS64 value = WitS64.of(Long.MIN_VALUE);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(8, result.length, "S64 should serialize to 8 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(Long.MIN_VALUE, buffer.getLong(), "Should preserve Long.MIN_VALUE");
  }

  @Test
  @DisplayName("Serialize float64 positive value to little-endian IEEE 754")
  void testSerializeFloat64Positive() throws WitValueException {
    final WitFloat64 value = WitFloat64.of(3.14159);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(8, result.length, "Float64 should serialize to 8 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(3.14159, buffer.getDouble(), 1e-10, "Should deserialize to original value");
  }

  @Test
  @DisplayName("Serialize float64 negative value to little-endian IEEE 754")
  void testSerializeFloat64Negative() throws WitValueException {
    final WitFloat64 value = WitFloat64.of(-999.99);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(8, result.length, "Float64 should serialize to 8 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(-999.99, buffer.getDouble(), 1e-10, "Should deserialize to original value");
  }

  @Test
  @DisplayName("Serialize float64 zero")
  void testSerializeFloat64Zero() throws WitValueException {
    final WitFloat64 value = WitFloat64.of(0.0);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(8, result.length, "Float64 should serialize to 8 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(0.0, buffer.getDouble(), "Should preserve zero value");
  }

  @Test
  @DisplayName("Serialize float64 max value")
  void testSerializeFloat64Max() throws WitValueException {
    final WitFloat64 value = WitFloat64.of(Double.MAX_VALUE);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(8, result.length, "Float64 should serialize to 8 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(Double.MAX_VALUE, buffer.getDouble(), "Should preserve Double.MAX_VALUE");
  }

  @Test
  @DisplayName("Serialize float64 min positive value")
  void testSerializeFloat64MinPositive() throws WitValueException {
    final WitFloat64 value = WitFloat64.of(Double.MIN_VALUE);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(8, result.length, "Float64 should serialize to 8 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(Double.MIN_VALUE, buffer.getDouble(), "Should preserve Double.MIN_VALUE");
  }

  @Test
  @DisplayName("Serialize char ASCII value to little-endian codepoint")
  void testSerializeCharAscii() throws WitValueException {
    final WitChar value = WitChar.of('A');
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(4, result.length, "Char should serialize to 4 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals((int) 'A', buffer.getInt(), "Should serialize to codepoint value");
  }

  @Test
  @DisplayName("Serialize char Unicode emoji to little-endian codepoint")
  void testSerializeCharEmoji() throws WitValueException {
    final WitChar value = WitChar.of(0x1F980); // 🦀 crab emoji
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(4, result.length, "Char should serialize to 4 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(0x1F980, buffer.getInt(), "Should serialize to codepoint value");
  }

  @Test
  @DisplayName("Serialize char Chinese character to little-endian codepoint")
  void testSerializeCharChinese() throws WitValueException {
    final WitChar value = WitChar.of('中');
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(4, result.length, "Char should serialize to 4 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals((int) '中', buffer.getInt(), "Should serialize to codepoint value");
  }

  @Test
  @DisplayName("Serialize char null character")
  void testSerializeCharNull() throws WitValueException {
    final WitChar value = WitChar.of(0x0000);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(4, result.length, "Char should serialize to 4 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(0x0000, buffer.getInt(), "Should serialize null character");
  }

  @Test
  @DisplayName("Serialize char max Unicode value")
  void testSerializeCharMaxUnicode() throws WitValueException {
    final WitChar value = WitChar.of(0x10FFFF);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(4, result.length, "Char should serialize to 4 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(0x10FFFF, buffer.getInt(), "Should serialize max Unicode codepoint");
  }

  @Test
  @DisplayName("Serialize string empty")
  void testSerializeStringEmpty() throws WitValueException {
    final WitString value = WitString.of("");
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(4, result.length, "Empty string should serialize to 4 bytes (length only)");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(0, buffer.getInt(), "Length should be 0");
  }

  @Test
  @DisplayName("Serialize string ASCII")
  void testSerializeStringAscii() throws WitValueException {
    final WitString value = WitString.of("hello");
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    final byte[] expectedUtf8 = "hello".getBytes(StandardCharsets.UTF_8);
    assertEquals(
        4 + expectedUtf8.length,
        result.length,
        "String should serialize to 4 bytes length + UTF-8 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(expectedUtf8.length, buffer.getInt(), "Length should match UTF-8 byte length");

    final byte[] actualUtf8 = new byte[expectedUtf8.length];
    buffer.get(actualUtf8);
    assertArrayEquals(expectedUtf8, actualUtf8, "UTF-8 bytes should match");
  }

  @Test
  @DisplayName("Serialize string with Unicode characters")
  void testSerializeStringUnicode() throws WitValueException {
    final WitString value = WitString.of("Hello 🦀 中文");
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    final byte[] expectedUtf8 = "Hello 🦀 中文".getBytes(StandardCharsets.UTF_8);
    assertEquals(
        4 + expectedUtf8.length,
        result.length,
        "String should serialize to 4 bytes length + UTF-8 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(expectedUtf8.length, buffer.getInt(), "Length should match UTF-8 byte length");

    final byte[] actualUtf8 = new byte[expectedUtf8.length];
    buffer.get(actualUtf8);
    assertArrayEquals(expectedUtf8, actualUtf8, "UTF-8 bytes should match");
  }

  @Test
  @DisplayName("Serialize string with special characters")
  void testSerializeStringSpecialChars() throws WitValueException {
    final WitString value = WitString.of("Line1\nLine2\tTab\r\nCRLF");
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    final byte[] expectedUtf8 = "Line1\nLine2\tTab\r\nCRLF".getBytes(StandardCharsets.UTF_8);
    assertEquals(
        4 + expectedUtf8.length,
        result.length,
        "String should serialize to 4 bytes length + UTF-8 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(expectedUtf8.length, buffer.getInt(), "Length should match UTF-8 byte length");

    final byte[] actualUtf8 = new byte[expectedUtf8.length];
    buffer.get(actualUtf8);
    assertArrayEquals(expectedUtf8, actualUtf8, "UTF-8 bytes should match");
  }

  @Test
  @DisplayName("Get type discriminator for bool")
  void testGetTypeDiscriminatorBool() throws WitValueException {
    final WitBool value = WitBool.of(true);
    assertEquals(
        1, WitValueSerializer.getTypeDiscriminator(value), "Bool discriminator should be 1");
  }

  @Test
  @DisplayName("Get type discriminator for s32")
  void testGetTypeDiscriminatorS32() throws WitValueException {
    final WitS32 value = WitS32.of(42);
    assertEquals(
        2, WitValueSerializer.getTypeDiscriminator(value), "S32 discriminator should be 2");
  }

  @Test
  @DisplayName("Get type discriminator for s64")
  void testGetTypeDiscriminatorS64() throws WitValueException {
    final WitS64 value = WitS64.of(100L);
    assertEquals(
        3, WitValueSerializer.getTypeDiscriminator(value), "S64 discriminator should be 3");
  }

  @Test
  @DisplayName("Get type discriminator for float64")
  void testGetTypeDiscriminatorFloat64() throws WitValueException {
    final WitFloat64 value = WitFloat64.of(3.14);
    assertEquals(
        4, WitValueSerializer.getTypeDiscriminator(value), "Float64 discriminator should be 4");
  }

  @Test
  @DisplayName("Get type discriminator for char")
  void testGetTypeDiscriminatorChar() throws WitValueException {
    final WitChar value = WitChar.of('A');
    assertEquals(
        5, WitValueSerializer.getTypeDiscriminator(value), "Char discriminator should be 5");
  }

  @Test
  @DisplayName("Get type discriminator for string")
  void testGetTypeDiscriminatorString() throws WitValueException {
    final WitString value = WitString.of("hello");
    assertEquals(
        6, WitValueSerializer.getTypeDiscriminator(value), "String discriminator should be 6");
  }

  @Test
  @DisplayName("Serialize null value throws WitValueException")
  void testSerializeNullValue() {
    final WitValueException exception =
        assertThrows(
            WitValueException.class,
            () -> WitValueSerializer.serialize(null),
            "Should throw WitValueException for null value");

    assertTrue(
        exception.getMessage().contains("null"),
        "Exception message should mention null: " + exception.getMessage());
    assertEquals(
        WitValueException.ErrorCode.NULL_VALUE,
        exception.getCode(),
        "Error code should be NULL_VALUE");
  }

  @Test
  @DisplayName("Get type discriminator for null value throws WitValueException")
  void testGetTypeDiscriminatorNullValue() {
    final WitValueException exception =
        assertThrows(
            WitValueException.class,
            () -> WitValueSerializer.getTypeDiscriminator(null),
            "Should throw WitValueException for null value");

    assertTrue(
        exception.getMessage().contains("null"),
        "Exception message should mention null: " + exception.getMessage());
    assertEquals(
        WitValueException.ErrorCode.NULL_VALUE,
        exception.getCode(),
        "Error code should be NULL_VALUE");
  }

  @Test
  @DisplayName("Serialize and deserialize WitList with integers")
  void testSerializeDeserializeListIntegers() throws WitValueException {
    final WitList list =
        WitList.of(WitS32.of(1), WitS32.of(2), WitS32.of(3), WitS32.of(4), WitS32.of(5));
    final byte[] serialized = WitValueSerializer.serialize(list);

    assertNotNull(serialized, "Serialized result should not be null");
    assertTrue(serialized.length > 4, "List should serialize to more than just count");

    final WitList deserialized = (WitList) WitValueDeserializer.deserialize(11, serialized);

    assertNotNull(deserialized, "Deserialized list should not be null");
    assertEquals(5, deserialized.size(), "List should have 5 elements");
    assertEquals(WitS32.of(1), deserialized.get(0), "First element should match");
    assertEquals(WitS32.of(3), deserialized.get(2), "Third element should match");
    assertEquals(WitS32.of(5), deserialized.get(4), "Fifth element should match");
  }

  @Test
  @DisplayName("Serialize and deserialize WitList with strings")
  void testSerializeDeserializeListStrings() throws WitValueException {
    final WitList list =
        WitList.of(WitString.of("hello"), WitString.of("world"), WitString.of("test"));
    final byte[] serialized = WitValueSerializer.serialize(list);

    assertNotNull(serialized, "Serialized result should not be null");

    final WitList deserialized = (WitList) WitValueDeserializer.deserialize(11, serialized);

    assertNotNull(deserialized, "Deserialized list should not be null");
    assertEquals(3, deserialized.size(), "List should have 3 elements");
    assertEquals(WitString.of("hello"), deserialized.get(0), "First element should match");
    assertEquals(WitString.of("world"), deserialized.get(1), "Second element should match");
    assertEquals(WitString.of("test"), deserialized.get(2), "Third element should match");
  }

  @Test
  @DisplayName("Get type discriminator for list")
  void testGetTypeDiscriminatorList() throws WitValueException {
    final WitList value = WitList.of(WitS32.of(1), WitS32.of(2));
    assertEquals(
        11, WitValueSerializer.getTypeDiscriminator(value), "List discriminator should be 11");
  }

  @Test
  @DisplayName("Get type discriminator for record")
  void testGetTypeDiscriminatorRecord() throws WitValueException {
    final java.util.Map<String, WitValue> fields = new java.util.LinkedHashMap<>();
    fields.put("field1", WitS32.of(42));
    final WitRecord value = WitRecord.of(fields);
    assertEquals(
        7, WitValueSerializer.getTypeDiscriminator(value), "Record discriminator should be 7");
  }

  @Test
  @DisplayName("Get type discriminator for u32")
  void testGetTypeDiscriminatorU32() throws WitValueException {
    final WitU32 value = WitU32.of(100);
    assertEquals(
        9, WitValueSerializer.getTypeDiscriminator(value), "U32 discriminator should be 9");
  }

  @Test
  @DisplayName("Get type discriminator for u64")
  void testGetTypeDiscriminatorU64() throws WitValueException {
    final WitU64 value = WitU64.of(100L);
    assertEquals(
        10, WitValueSerializer.getTypeDiscriminator(value), "U64 discriminator should be 10");
  }

  @Test
  @DisplayName("Get type discriminator for variant")
  void testGetTypeDiscriminatorVariant() throws WitValueException {
    final ai.tegmentum.wasmtime4j.WitType variantType =
        ai.tegmentum.wasmtime4j.WitType.variant("result",
            java.util.Map.of("success", java.util.Optional.of(ai.tegmentum.wasmtime4j.WitType.createS32())));
    final WitVariant value = WitVariant.of(variantType, "success", WitS32.of(42));
    assertEquals(
        12, WitValueSerializer.getTypeDiscriminator(value), "Variant discriminator should be 12");
  }

  @Test
  @DisplayName("Get type discriminator for option")
  void testGetTypeDiscriminatorOption() throws WitValueException {
    final ai.tegmentum.wasmtime4j.WitType optionType =
        ai.tegmentum.wasmtime4j.WitType.option(ai.tegmentum.wasmtime4j.WitType.createS32());
    final WitOption value = WitOption.some(optionType, WitS32.of(42));
    assertEquals(
        14, WitValueSerializer.getTypeDiscriminator(value), "Option discriminator should be 14");
  }

  @Test
  @DisplayName("Get type discriminator for own")
  void testGetTypeDiscriminatorOwn() throws WitValueException {
    final WitOwn value = WitOwn.of("TestResource", 1);
    assertEquals(
        22, WitValueSerializer.getTypeDiscriminator(value), "Own discriminator should be 22");
  }

  @Test
  @DisplayName("Get type discriminator for borrow")
  void testGetTypeDiscriminatorBorrow() throws WitValueException {
    final WitBorrow value = WitBorrow.of("TestResource", 1);
    assertEquals(
        23, WitValueSerializer.getTypeDiscriminator(value), "Borrow discriminator should be 23");
  }

  @Test
  @DisplayName("Serialize s8 to binary format")
  void testSerializeS8() throws WitValueException {
    final WitS8 value = WitS8.of((byte) -42);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(1, result.length, "S8 should serialize to 1 byte");
    assertEquals((byte) -42, result[0], "Value should be preserved");
  }

  @Test
  @DisplayName("Serialize s16 to binary format")
  void testSerializeS16() throws WitValueException {
    final WitS16 value = WitS16.of((short) -1000);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(2, result.length, "S16 should serialize to 2 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals((short) -1000, buffer.getShort(), "Value should be preserved");
  }

  @Test
  @DisplayName("Serialize u8 to binary format")
  void testSerializeU8() throws WitValueException {
    final WitU8 value = WitU8.of((byte) 0xFF);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(1, result.length, "U8 should serialize to 1 byte");
    assertEquals((byte) 0xFF, result[0], "Value should be preserved");
  }

  @Test
  @DisplayName("Serialize u16 to binary format")
  void testSerializeU16() throws WitValueException {
    final WitU16 value = WitU16.of((short) 0xFFFF);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(2, result.length, "U16 should serialize to 2 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals((short) 0xFFFF, buffer.getShort(), "Value should be preserved");
  }

  @Test
  @DisplayName("Serialize u32 to binary format")
  void testSerializeU32() throws WitValueException {
    final WitU32 value = WitU32.of(0xFFFFFFFF);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(4, result.length, "U32 should serialize to 4 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(0xFFFFFFFF, buffer.getInt(), "Value should be preserved");
  }

  @Test
  @DisplayName("Serialize u64 to binary format")
  void testSerializeU64() throws WitValueException {
    final WitU64 value = WitU64.of(-1L); // Max unsigned
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(8, result.length, "U64 should serialize to 8 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(-1L, buffer.getLong(), "Value should be preserved");
  }

  @Test
  @DisplayName("Serialize float32 to binary format")
  void testSerializeFloat32() throws WitValueException {
    final WitFloat32 value = WitFloat32.of(3.14f);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(4, result.length, "Float32 should serialize to 4 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(3.14f, buffer.getFloat(), 0.001f, "Value should be preserved");
  }

  @Test
  @DisplayName("Serialize record to binary format")
  void testSerializeRecord() throws WitValueException {
    final java.util.Map<String, WitValue> fields = new java.util.LinkedHashMap<>();
    fields.put("name", WitString.of("test"));
    fields.put("age", WitS32.of(42));
    final WitRecord value = WitRecord.of(fields);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertTrue(result.length > 4, "Record should have more than just field count");
  }

  @Test
  @DisplayName("Serialize variant to binary format")
  void testSerializeVariant() throws WitValueException {
    final ai.tegmentum.wasmtime4j.WitType variantType =
        ai.tegmentum.wasmtime4j.WitType.variant("opt",
            java.util.Map.of("some", java.util.Optional.of(ai.tegmentum.wasmtime4j.WitType.createS32())));
    final WitVariant value = WitVariant.of(variantType, "some", WitS32.of(42));
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertTrue(result.length > 0, "Variant should have serialized data");
  }

  @Test
  @DisplayName("Serialize option some to binary format")
  void testSerializeOptionSome() throws WitValueException {
    final ai.tegmentum.wasmtime4j.WitType optionType =
        ai.tegmentum.wasmtime4j.WitType.option(ai.tegmentum.wasmtime4j.WitType.createS32());
    final WitOption value = WitOption.some(optionType, WitS32.of(42));
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertTrue(result.length > 1, "Option some should have discriminator + payload");
  }

  @Test
  @DisplayName("Serialize option none to binary format")
  void testSerializeOptionNone() throws WitValueException {
    final ai.tegmentum.wasmtime4j.WitType optionType =
        ai.tegmentum.wasmtime4j.WitType.option(ai.tegmentum.wasmtime4j.WitType.createS32());
    final WitOption value = WitOption.none(optionType);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(1, result.length, "Option none should have just discriminator");
    assertEquals((byte) 0, result[0], "None should be discriminator 0");
  }

  @Test
  @DisplayName("Serialize own resource handle to binary format")
  void testSerializeOwn() throws WitValueException {
    final WitOwn value = WitOwn.of("FileHandle", 42);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertTrue(result.length > 4, "Own should have type name + index");
  }

  @Test
  @DisplayName("Serialize borrow resource handle to binary format")
  void testSerializeBorrow() throws WitValueException {
    final WitBorrow value = WitBorrow.of("FileHandle", 42);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertTrue(result.length > 4, "Borrow should have type name + index");
  }

  @Test
  @DisplayName("Serialize empty list to binary format")
  void testSerializeEmptyList() throws WitValueException {
    final ai.tegmentum.wasmtime4j.WitType elementType = ai.tegmentum.wasmtime4j.WitType.createS32();
    final WitList value = WitList.empty(elementType);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(4, result.length, "Empty list should just have count");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(0, buffer.getInt(), "Count should be 0");
  }

  @Test
  @DisplayName("Serialize single element list to binary format")
  void testSerializeSingleElementList() throws WitValueException {
    final WitList value = WitList.of(WitS32.of(42));
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertTrue(result.length > 4, "Single element list should have count + element data");
  }
}
