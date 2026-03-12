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
package ai.tegmentum.wasmtime4j.wit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.ValidationException;
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
  void testSerializeBoolTrue() throws ValidationException {
    final WitBool value = WitBool.of(true);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(1, result.length, "Bool should serialize to 1 byte");
    assertEquals((byte) 1, result[0], "True should serialize to byte value 1");
  }

  @Test
  @DisplayName("Serialize bool false to binary format")
  void testSerializeBoolFalse() throws ValidationException {
    final WitBool value = WitBool.of(false);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(1, result.length, "Bool should serialize to 1 byte");
    assertEquals((byte) 0, result[0], "False should serialize to byte value 0");
  }

  @Test
  @DisplayName("Serialize s32 positive value to little-endian binary")
  void testSerializeS32Positive() throws ValidationException {
    final WitS32 value = WitS32.of(42);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(4, result.length, "S32 should serialize to 4 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(42, buffer.getInt(), "Should deserialize to original value");
  }

  @Test
  @DisplayName("Serialize s32 negative value to little-endian binary")
  void testSerializeS32Negative() throws ValidationException {
    final WitS32 value = WitS32.of(-999);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(4, result.length, "S32 should serialize to 4 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(-999, buffer.getInt(), "Should deserialize to original value");
  }

  @Test
  @DisplayName("Serialize s32 max value")
  void testSerializeS32Max() throws ValidationException {
    final WitS32 value = WitS32.of(Integer.MAX_VALUE);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(4, result.length, "S32 should serialize to 4 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(Integer.MAX_VALUE, buffer.getInt(), "Should preserve Integer.MAX_VALUE");
  }

  @Test
  @DisplayName("Serialize s32 min value")
  void testSerializeS32Min() throws ValidationException {
    final WitS32 value = WitS32.of(Integer.MIN_VALUE);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(4, result.length, "S32 should serialize to 4 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(Integer.MIN_VALUE, buffer.getInt(), "Should preserve Integer.MIN_VALUE");
  }

  @Test
  @DisplayName("Serialize s64 positive value to little-endian binary")
  void testSerializeS64Positive() throws ValidationException {
    final WitS64 value = WitS64.of(1_000_000_000_000L);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(8, result.length, "S64 should serialize to 8 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(1_000_000_000_000L, buffer.getLong(), "Should deserialize to original value");
  }

  @Test
  @DisplayName("Serialize s64 negative value to little-endian binary")
  void testSerializeS64Negative() throws ValidationException {
    final WitS64 value = WitS64.of(-9_999_999_999L);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(8, result.length, "S64 should serialize to 8 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(-9_999_999_999L, buffer.getLong(), "Should deserialize to original value");
  }

  @Test
  @DisplayName("Serialize s64 max value")
  void testSerializeS64Max() throws ValidationException {
    final WitS64 value = WitS64.of(Long.MAX_VALUE);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(8, result.length, "S64 should serialize to 8 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(Long.MAX_VALUE, buffer.getLong(), "Should preserve Long.MAX_VALUE");
  }

  @Test
  @DisplayName("Serialize s64 min value")
  void testSerializeS64Min() throws ValidationException {
    final WitS64 value = WitS64.of(Long.MIN_VALUE);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(8, result.length, "S64 should serialize to 8 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(Long.MIN_VALUE, buffer.getLong(), "Should preserve Long.MIN_VALUE");
  }

  @Test
  @DisplayName("Serialize float64 positive value to little-endian IEEE 754")
  void testSerializeFloat64Positive() throws ValidationException {
    final WitFloat64 value = WitFloat64.of(3.14159);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(8, result.length, "Float64 should serialize to 8 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(3.14159, buffer.getDouble(), 1e-10, "Should deserialize to original value");
  }

  @Test
  @DisplayName("Serialize float64 negative value to little-endian IEEE 754")
  void testSerializeFloat64Negative() throws ValidationException {
    final WitFloat64 value = WitFloat64.of(-999.99);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(8, result.length, "Float64 should serialize to 8 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(-999.99, buffer.getDouble(), 1e-10, "Should deserialize to original value");
  }

  @Test
  @DisplayName("Serialize float64 zero")
  void testSerializeFloat64Zero() throws ValidationException {
    final WitFloat64 value = WitFloat64.of(0.0);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(8, result.length, "Float64 should serialize to 8 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(0.0, buffer.getDouble(), "Should preserve zero value");
  }

  @Test
  @DisplayName("Serialize float64 max value")
  void testSerializeFloat64Max() throws ValidationException {
    final WitFloat64 value = WitFloat64.of(Double.MAX_VALUE);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(8, result.length, "Float64 should serialize to 8 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(Double.MAX_VALUE, buffer.getDouble(), "Should preserve Double.MAX_VALUE");
  }

  @Test
  @DisplayName("Serialize float64 min positive value")
  void testSerializeFloat64MinPositive() throws ValidationException {
    final WitFloat64 value = WitFloat64.of(Double.MIN_VALUE);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(8, result.length, "Float64 should serialize to 8 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(Double.MIN_VALUE, buffer.getDouble(), "Should preserve Double.MIN_VALUE");
  }

  @Test
  @DisplayName("Serialize char ASCII value to little-endian codepoint")
  void testSerializeCharAscii() throws ValidationException {
    final WitChar value = WitChar.of('A');
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(4, result.length, "Char should serialize to 4 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals((int) 'A', buffer.getInt(), "Should serialize to codepoint value");
  }

  @Test
  @DisplayName("Serialize char Unicode emoji to little-endian codepoint")
  void testSerializeCharEmoji() throws ValidationException {
    final WitChar value = WitChar.of(0x1F980); // 🦀 crab emoji
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(4, result.length, "Char should serialize to 4 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(0x1F980, buffer.getInt(), "Should serialize to codepoint value");
  }

  @Test
  @DisplayName("Serialize char Chinese character to little-endian codepoint")
  void testSerializeCharChinese() throws ValidationException {
    final WitChar value = WitChar.of('中');
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(4, result.length, "Char should serialize to 4 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals((int) '中', buffer.getInt(), "Should serialize to codepoint value");
  }

  @Test
  @DisplayName("Serialize char null character")
  void testSerializeCharNull() throws ValidationException {
    final WitChar value = WitChar.of(0x0000);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(4, result.length, "Char should serialize to 4 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(0x0000, buffer.getInt(), "Should serialize null character");
  }

  @Test
  @DisplayName("Serialize char max Unicode value")
  void testSerializeCharMaxUnicode() throws ValidationException {
    final WitChar value = WitChar.of(0x10FFFF);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(4, result.length, "Char should serialize to 4 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(0x10FFFF, buffer.getInt(), "Should serialize max Unicode codepoint");
  }

  @Test
  @DisplayName("Serialize string empty")
  void testSerializeStringEmpty() throws ValidationException {
    final WitString value = WitString.of("");
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(4, result.length, "Empty string should serialize to 4 bytes (length only)");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(0, buffer.getInt(), "Length should be 0");
  }

  @Test
  @DisplayName("Serialize string ASCII")
  void testSerializeStringAscii() throws ValidationException {
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
  void testSerializeStringUnicode() throws ValidationException {
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
  void testSerializeStringSpecialChars() throws ValidationException {
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
  void testGetTypeDiscriminatorBool() throws ValidationException {
    final WitBool value = WitBool.of(true);
    assertEquals(
        1, WitValueSerializer.getTypeDiscriminator(value), "Bool discriminator should be 1");
  }

  @Test
  @DisplayName("Get type discriminator for s32")
  void testGetTypeDiscriminatorS32() throws ValidationException {
    final WitS32 value = WitS32.of(42);
    assertEquals(
        2, WitValueSerializer.getTypeDiscriminator(value), "S32 discriminator should be 2");
  }

  @Test
  @DisplayName("Get type discriminator for s64")
  void testGetTypeDiscriminatorS64() throws ValidationException {
    final WitS64 value = WitS64.of(100L);
    assertEquals(
        3, WitValueSerializer.getTypeDiscriminator(value), "S64 discriminator should be 3");
  }

  @Test
  @DisplayName("Get type discriminator for float64")
  void testGetTypeDiscriminatorFloat64() throws ValidationException {
    final WitFloat64 value = WitFloat64.of(3.14);
    assertEquals(
        4, WitValueSerializer.getTypeDiscriminator(value), "Float64 discriminator should be 4");
  }

  @Test
  @DisplayName("Get type discriminator for char")
  void testGetTypeDiscriminatorChar() throws ValidationException {
    final WitChar value = WitChar.of('A');
    assertEquals(
        5, WitValueSerializer.getTypeDiscriminator(value), "Char discriminator should be 5");
  }

  @Test
  @DisplayName("Get type discriminator for string")
  void testGetTypeDiscriminatorString() throws ValidationException {
    final WitString value = WitString.of("hello");
    assertEquals(
        6, WitValueSerializer.getTypeDiscriminator(value), "String discriminator should be 6");
  }

  @Test
  @DisplayName("Serialize null value throws ValidationException")
  void testSerializeNullValue() {
    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueSerializer.serialize(null),
            "Should throw ValidationException for null value");

    assertTrue(
        exception.getMessage().contains("null"),
        "Exception message should mention null: " + exception.getMessage());
  }

  @Test
  @DisplayName("Get type discriminator for null value throws ValidationException")
  void testGetTypeDiscriminatorNullValue() {
    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueSerializer.getTypeDiscriminator(null),
            "Should throw ValidationException for null value");

    assertTrue(
        exception.getMessage().contains("null"),
        "Exception message should mention null: " + exception.getMessage());
  }

  @Test
  @DisplayName("Serialize and deserialize WitList with integers")
  void testSerializeDeserializeListIntegers() throws ValidationException {
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
  void testSerializeDeserializeListStrings() throws ValidationException {
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
  void testGetTypeDiscriminatorList() throws ValidationException {
    final WitList value = WitList.of(WitS32.of(1), WitS32.of(2));
    assertEquals(
        11, WitValueSerializer.getTypeDiscriminator(value), "List discriminator should be 11");
  }

  @Test
  @DisplayName("Get type discriminator for record")
  void testGetTypeDiscriminatorRecord() throws ValidationException {
    final java.util.Map<String, WitValue> fields = new java.util.LinkedHashMap<>();
    fields.put("field1", WitS32.of(42));
    final WitRecord value = WitRecord.of(fields);
    assertEquals(
        7, WitValueSerializer.getTypeDiscriminator(value), "Record discriminator should be 7");
  }

  @Test
  @DisplayName("Get type discriminator for u32")
  void testGetTypeDiscriminatorU32() throws ValidationException {
    final WitU32 value = WitU32.of(100);
    assertEquals(
        9, WitValueSerializer.getTypeDiscriminator(value), "U32 discriminator should be 9");
  }

  @Test
  @DisplayName("Get type discriminator for u64")
  void testGetTypeDiscriminatorU64() throws ValidationException {
    final WitU64 value = WitU64.of(100L);
    assertEquals(
        10, WitValueSerializer.getTypeDiscriminator(value), "U64 discriminator should be 10");
  }

  @Test
  @DisplayName("Get type discriminator for variant")
  void testGetTypeDiscriminatorVariant() throws ValidationException {
    final WitType variantType =
        WitType.variant(
            "result", java.util.Map.of("success", java.util.Optional.of(WitType.createS32())));
    final WitVariant value = WitVariant.of(variantType, "success", WitS32.of(42));
    assertEquals(
        12, WitValueSerializer.getTypeDiscriminator(value), "Variant discriminator should be 12");
  }

  @Test
  @DisplayName("Get type discriminator for option")
  void testGetTypeDiscriminatorOption() throws ValidationException {
    final WitType optionType = WitType.option(WitType.createS32());
    final WitOption value = WitOption.some(optionType, WitS32.of(42));
    assertEquals(
        14, WitValueSerializer.getTypeDiscriminator(value), "Option discriminator should be 14");
  }

  @Test
  @DisplayName("Get type discriminator for own")
  void testGetTypeDiscriminatorOwn() throws ValidationException {
    final WitOwn value = WitOwn.of("TestResource", 1);
    assertEquals(
        22, WitValueSerializer.getTypeDiscriminator(value), "Own discriminator should be 22");
  }

  @Test
  @DisplayName("Get type discriminator for borrow")
  void testGetTypeDiscriminatorBorrow() throws ValidationException {
    final WitBorrow value = WitBorrow.of("TestResource", 1);
    assertEquals(
        23, WitValueSerializer.getTypeDiscriminator(value), "Borrow discriminator should be 23");
  }

  @Test
  @DisplayName("Get type discriminator for s8")
  void testGetTypeDiscriminatorS8() throws ValidationException {
    final WitS8 value = WitS8.of((byte) 1);
    assertEquals(
        17, WitValueSerializer.getTypeDiscriminator(value), "S8 discriminator should be 17");
  }

  @Test
  @DisplayName("Get type discriminator for s16")
  void testGetTypeDiscriminatorS16() throws ValidationException {
    final WitS16 value = WitS16.of((short) 1);
    assertEquals(
        18, WitValueSerializer.getTypeDiscriminator(value), "S16 discriminator should be 18");
  }

  @Test
  @DisplayName("Get type discriminator for u8")
  void testGetTypeDiscriminatorU8() throws ValidationException {
    final WitU8 value = WitU8.of((byte) 1);
    assertEquals(
        19, WitValueSerializer.getTypeDiscriminator(value), "U8 discriminator should be 19");
  }

  @Test
  @DisplayName("Get type discriminator for u16")
  void testGetTypeDiscriminatorU16() throws ValidationException {
    final WitU16 value = WitU16.of((short) 1);
    assertEquals(
        20, WitValueSerializer.getTypeDiscriminator(value), "U16 discriminator should be 20");
  }

  @Test
  @DisplayName("Get type discriminator for float32")
  void testGetTypeDiscriminatorFloat32() throws ValidationException {
    final WitFloat32 value = WitFloat32.of(1.0f);
    assertEquals(
        21, WitValueSerializer.getTypeDiscriminator(value), "Float32 discriminator should be 21");
  }

  @Test
  @DisplayName("Round-trip serialize/deserialize s8 via marshaller")
  void testRoundTripS8() throws ValidationException {
    final WitS8 original = WitS8.of((byte) -42);
    final int discriminator = WitValueSerializer.getTypeDiscriminator(original);
    final byte[] data = WitValueSerializer.serialize(original);
    final WitValue restored = WitValueDeserializer.deserialize(discriminator, data);

    assertInstanceOf(WitS8.class, restored, "Should deserialize back to WitS8");
    assertEquals(
        original.getValue(), ((WitS8) restored).getValue(), "Value should survive round-trip");
  }

  @Test
  @DisplayName("Round-trip serialize/deserialize s16 via marshaller")
  void testRoundTripS16() throws ValidationException {
    final WitS16 original = WitS16.of((short) -1000);
    final int discriminator = WitValueSerializer.getTypeDiscriminator(original);
    final byte[] data = WitValueSerializer.serialize(original);
    final WitValue restored = WitValueDeserializer.deserialize(discriminator, data);

    assertInstanceOf(WitS16.class, restored, "Should deserialize back to WitS16");
    assertEquals(
        original.getValue(), ((WitS16) restored).getValue(), "Value should survive round-trip");
  }

  @Test
  @DisplayName("Round-trip serialize/deserialize u8 via marshaller")
  void testRoundTripU8() throws ValidationException {
    final WitU8 original = WitU8.of((byte) 0xFF);
    final int discriminator = WitValueSerializer.getTypeDiscriminator(original);
    final byte[] data = WitValueSerializer.serialize(original);
    final WitValue restored = WitValueDeserializer.deserialize(discriminator, data);

    assertInstanceOf(WitU8.class, restored, "Should deserialize back to WitU8");
    assertEquals(
        original.getValue(), ((WitU8) restored).getValue(), "Value should survive round-trip");
  }

  @Test
  @DisplayName("Round-trip serialize/deserialize u16 via marshaller")
  void testRoundTripU16() throws ValidationException {
    final WitU16 original = WitU16.of((short) 0xFFFF);
    final int discriminator = WitValueSerializer.getTypeDiscriminator(original);
    final byte[] data = WitValueSerializer.serialize(original);
    final WitValue restored = WitValueDeserializer.deserialize(discriminator, data);

    assertInstanceOf(WitU16.class, restored, "Should deserialize back to WitU16");
    assertEquals(
        original.getValue(), ((WitU16) restored).getValue(), "Value should survive round-trip");
  }

  @Test
  @DisplayName("Round-trip serialize/deserialize float32 via marshaller")
  void testRoundTripFloat32() throws ValidationException {
    final WitFloat32 original = WitFloat32.of(3.14f);
    final int discriminator = WitValueSerializer.getTypeDiscriminator(original);
    final byte[] data = WitValueSerializer.serialize(original);
    final WitValue restored = WitValueDeserializer.deserialize(discriminator, data);

    assertInstanceOf(WitFloat32.class, restored, "Should deserialize back to WitFloat32");
    assertEquals(
        original.getValue(),
        ((WitFloat32) restored).getValue(),
        0.001f,
        "Value should survive round-trip");
  }

  @Test
  @DisplayName("Serialize s8 to binary format")
  void testSerializeS8() throws ValidationException {
    final WitS8 value = WitS8.of((byte) -42);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(1, result.length, "S8 should serialize to 1 byte");
    assertEquals((byte) -42, result[0], "Value should be preserved");
  }

  @Test
  @DisplayName("Serialize s16 to binary format")
  void testSerializeS16() throws ValidationException {
    final WitS16 value = WitS16.of((short) -1000);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(2, result.length, "S16 should serialize to 2 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals((short) -1000, buffer.getShort(), "Value should be preserved");
  }

  @Test
  @DisplayName("Serialize u8 to binary format")
  void testSerializeU8() throws ValidationException {
    final WitU8 value = WitU8.of((byte) 0xFF);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(1, result.length, "U8 should serialize to 1 byte");
    assertEquals((byte) 0xFF, result[0], "Value should be preserved");
  }

  @Test
  @DisplayName("Serialize u16 to binary format")
  void testSerializeU16() throws ValidationException {
    final WitU16 value = WitU16.of((short) 0xFFFF);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(2, result.length, "U16 should serialize to 2 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals((short) 0xFFFF, buffer.getShort(), "Value should be preserved");
  }

  @Test
  @DisplayName("Serialize u32 to binary format")
  void testSerializeU32() throws ValidationException {
    final WitU32 value = WitU32.of(0xFFFFFFFF);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(4, result.length, "U32 should serialize to 4 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(0xFFFFFFFF, buffer.getInt(), "Value should be preserved");
  }

  @Test
  @DisplayName("Serialize u64 to binary format")
  void testSerializeU64() throws ValidationException {
    final WitU64 value = WitU64.of(-1L); // Max unsigned
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(8, result.length, "U64 should serialize to 8 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(-1L, buffer.getLong(), "Value should be preserved");
  }

  @Test
  @DisplayName("Serialize float32 to binary format")
  void testSerializeFloat32() throws ValidationException {
    final WitFloat32 value = WitFloat32.of(3.14f);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(4, result.length, "Float32 should serialize to 4 bytes");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(3.14f, buffer.getFloat(), 0.001f, "Value should be preserved");
  }

  @Test
  @DisplayName("Serialize record to binary format")
  void testSerializeRecord() throws ValidationException {
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
  void testSerializeVariant() throws ValidationException {
    final WitType variantType =
        WitType.variant(
            "opt", java.util.Map.of("some", java.util.Optional.of(WitType.createS32())));
    final WitVariant value = WitVariant.of(variantType, "some", WitS32.of(42));
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertTrue(result.length > 0, "Variant should have serialized data");
  }

  @Test
  @DisplayName("Serialize option some to binary format")
  void testSerializeOptionSome() throws ValidationException {
    final WitType optionType = WitType.option(WitType.createS32());
    final WitOption value = WitOption.some(optionType, WitS32.of(42));
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertTrue(result.length > 1, "Option some should have discriminator + payload");
  }

  @Test
  @DisplayName("Serialize option none to binary format")
  void testSerializeOptionNone() throws ValidationException {
    final WitType optionType = WitType.option(WitType.createS32());
    final WitOption value = WitOption.none(optionType);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(1, result.length, "Option none should have just discriminator");
    assertEquals((byte) 0, result[0], "None should be discriminator 0");
  }

  @Test
  @DisplayName("Serialize own resource handle with i64 binary layout")
  void testSerializeOwn() throws ValidationException {
    final WitOwn value = WitOwn.of("FileHandle", 42);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    // Format: [type_name_length: i32 LE][type_name: UTF-8][handle: i64 LE]
    final byte[] typeNameBytes = "FileHandle".getBytes(StandardCharsets.UTF_8);
    assertEquals(
        4 + typeNameBytes.length + 8,
        result.length,
        "Own should be 4 (name length) + name bytes + 8 (i64 handle)");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(typeNameBytes.length, buffer.getInt(), "Type name length should match");
    final byte[] readName = new byte[typeNameBytes.length];
    buffer.get(readName);
    assertEquals(
        "FileHandle", new String(readName, StandardCharsets.UTF_8), "Type name should match");
    assertEquals(42L, buffer.getLong(), "Handle should be serialized as i64");
    assertFalse(buffer.hasRemaining(), "No trailing bytes should remain");
  }

  @Test
  @DisplayName("Serialize borrow resource handle with i64 binary layout")
  void testSerializeBorrow() throws ValidationException {
    final WitBorrow value = WitBorrow.of("FileHandle", 42);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    // Format: [type_name_length: i32 LE][type_name: UTF-8][handle: i64 LE]
    final byte[] typeNameBytes = "FileHandle".getBytes(StandardCharsets.UTF_8);
    assertEquals(
        4 + typeNameBytes.length + 8,
        result.length,
        "Borrow should be 4 (name length) + name bytes + 8 (i64 handle)");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(typeNameBytes.length, buffer.getInt(), "Type name length should match");
    final byte[] readName = new byte[typeNameBytes.length];
    buffer.get(readName);
    assertEquals(
        "FileHandle", new String(readName, StandardCharsets.UTF_8), "Type name should match");
    assertEquals(42L, buffer.getLong(), "Handle should be serialized as i64");
    assertFalse(buffer.hasRemaining(), "No trailing bytes should remain");
  }

  @Test
  @DisplayName("Serialize empty list to binary format")
  void testSerializeEmptyList() throws ValidationException {
    final WitType elementType = WitType.createS32();
    final WitList value = WitList.empty(elementType);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertEquals(4, result.length, "Empty list should just have count");

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(0, buffer.getInt(), "Count should be 0");
  }

  @Test
  @DisplayName("Serialize single element list to binary format")
  void testSerializeSingleElementList() throws ValidationException {
    final WitList value = WitList.of(WitS32.of(42));
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    assertTrue(result.length > 4, "Single element list should have count + element data");
  }

  // ============== Option exact byte verification tests ==============

  @Test
  @DisplayName("Serialize option some - verify exact some byte value is 1")
  void testSerializeOptionSomeExactByte() throws ValidationException {
    final WitType optionType = WitType.option(WitType.createS32());
    final WitOption value = WitOption.some(optionType, WitS32.of(42));
    final byte[] result = WitValueSerializer.serialize(value);

    // First byte must be exactly 1 for some (not 0, not 2, not any other value)
    assertEquals((byte) 1, result[0], "Some discriminator must be exactly byte value 1");
  }

  @Test
  @DisplayName("Serialize option none - verify exact none byte value is 0")
  void testSerializeOptionNoneExactByte() throws ValidationException {
    final WitType optionType = WitType.option(WitType.createS32());
    final WitOption value = WitOption.none(optionType);
    final byte[] result = WitValueSerializer.serialize(value);

    // First byte must be exactly 0 for none (not 1, not any other value)
    assertEquals((byte) 0, result[0], "None discriminator must be exactly byte value 0");
  }

  // ============== List count verification ==============

  @Test
  @DisplayName("Serialize list - verify exact count in first 4 bytes")
  void testSerializeListVerifyCount() throws ValidationException {
    final WitList value = WitList.of(WitS32.of(1), WitS32.of(2), WitS32.of(3));
    final byte[] result = WitValueSerializer.serialize(value);

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(3, buffer.getInt(), "List count in first 4 bytes must be exactly 3");
  }

  // ============== Variant payload presence flag tests ==============

  @Test
  @DisplayName("Serialize variant with payload - verify has_payload byte is 1")
  void testSerializeVariantWithPayloadExactByte() throws ValidationException {
    final WitType variantType =
        WitType.variant(
            "opt", java.util.Map.of("some", java.util.Optional.of(WitType.createS32())));
    final WitVariant value = WitVariant.of(variantType, "some", WitS32.of(42));
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    // Format: [name_length: u32][name: UTF-8][has_payload: u8]...
    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    final int nameLength = buffer.getInt();
    buffer.position(4 + nameLength); // Skip past name
    final byte hasPayload = buffer.get();
    assertEquals((byte) 1, hasPayload, "has_payload byte must be exactly 1 when payload present");
  }

  @Test
  @DisplayName("Serialize variant without payload - verify has_payload byte is 0")
  void testSerializeVariantWithoutPayloadExactByte() throws ValidationException {
    final WitType variantType =
        WitType.variant("status", java.util.Map.of("none", java.util.Optional.empty()));
    final WitVariant value = WitVariant.of(variantType, "none", null);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    // Format: [name_length: u32][name: UTF-8][has_payload: u8]
    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    final int nameLength = buffer.getInt();
    buffer.position(4 + nameLength); // Skip past name
    final byte hasPayload = buffer.get();
    assertEquals((byte) 0, hasPayload, "has_payload byte must be exactly 0 when no payload");
  }

  // ============== Flags serialization tests ==============

  @Test
  @DisplayName("Serialize flags with two set flags")
  void testSerializeFlags() throws ValidationException {
    final WitType flagsType =
        WitType.flags("perms", java.util.Arrays.asList("read", "write", "exec"));
    final WitFlags value = WitFlags.of(flagsType, "read", "write");
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized flags should not be null");
    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    final int count = buffer.getInt();
    assertEquals(2, count, "Serialized flag count should be 2");
  }

  @Test
  @DisplayName("Serialize empty flags")
  void testSerializeEmptyFlags() throws ValidationException {
    final WitType flagsType = WitType.flags("perms", java.util.Arrays.asList("read", "write"));
    final WitFlags value = WitFlags.empty(flagsType);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized empty flags should not be null");
    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(0, buffer.getInt(), "Empty flags should serialize with count 0");
    assertEquals(4, result.length, "Empty flags should be just 4 bytes (count)");
  }

  // ============== Result serialization tests ==============

  @Test
  @DisplayName("Serialize result ok without payload")
  void testSerializeResultOkWithoutPayload() throws ValidationException {
    final WitType resultType =
        WitType.result(java.util.Optional.empty(), java.util.Optional.empty());
    final WitResult value = WitResult.ok(resultType);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result ok should not be null");
    assertEquals(2, result.length, "Result ok without payload should be 2 bytes");
    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals((byte) 1, buffer.get(), "is_ok byte should be 1");
    assertEquals((byte) 0, buffer.get(), "has_value byte should be 0");
  }

  @Test
  @DisplayName("Serialize enum should produce roundtrippable bytes")
  void testSerializeEnumRoundtrip() throws ValidationException {
    final WitType enumType =
        WitType.enumType("color", java.util.Arrays.asList("red", "green", "blue"));
    final WitEnum value = WitEnum.of(enumType, "green");

    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized enum should not be null");
    assertTrue(result.length > 4, "Serialized enum should have length header + name bytes");

    // Roundtrip: deserialize and check
    final WitValue deserialized = WitValueDeserializer.deserialize(13, result);
    assertInstanceOf(WitEnum.class, deserialized, "Deserialized should be WitEnum");
    assertEquals(
        "green", ((WitEnum) deserialized).getDiscriminant(), "Roundtrip discriminant should match");
  }

  @Test
  @DisplayName("Serialize result ok with payload should roundtrip")
  void testSerializeResultOkWithPayloadRoundtrip() throws ValidationException {
    final WitType resultType =
        WitType.result(java.util.Optional.of(WitType.createS32()), java.util.Optional.empty());
    final WitResult value = WitResult.ok(resultType, WitS32.of(42));

    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");
    // is_ok(1) + has_value(1) + disc(4) + len(4) + data(4) = 14
    assertEquals(14, result.length, "Result ok with S32 payload should be 14 bytes");

    // Roundtrip: deserialize and check
    final WitValue deserialized = WitValueDeserializer.deserialize(15, result);
    assertInstanceOf(WitResult.class, deserialized, "Deserialized should be WitResult");
    final WitResult deserializedResult = (WitResult) deserialized;
    assertTrue(deserializedResult.isOk(), "Should be ok");
    assertTrue(deserializedResult.getValue().isPresent(), "Should have value");
    assertEquals(
        42, ((WitS32) deserializedResult.getValue().get()).getValue(), "Payload should be 42");
  }

  @Test
  @DisplayName("Serialize result err with payload should roundtrip")
  void testSerializeResultErrWithPayloadRoundtrip() throws ValidationException {
    final WitType resultType =
        WitType.result(java.util.Optional.empty(), java.util.Optional.of(WitType.createS32()));
    final WitResult value = WitResult.err(resultType, WitS32.of(99));

    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result should not be null");

    final WitValue deserialized = WitValueDeserializer.deserialize(15, result);
    assertInstanceOf(WitResult.class, deserialized, "Deserialized should be WitResult");
    final WitResult deserializedResult = (WitResult) deserialized;
    assertTrue(deserializedResult.isErr(), "Should be err");
    assertEquals(
        99,
        ((WitS32) deserializedResult.getValue().get()).getValue(),
        "Error payload should be 99");
  }

  @Test
  @DisplayName("Serialize result err without payload")
  void testSerializeResultErrWithoutPayload() throws ValidationException {
    final WitType resultType =
        WitType.result(java.util.Optional.empty(), java.util.Optional.empty());
    final WitResult value = WitResult.err(resultType);
    final byte[] result = WitValueSerializer.serialize(value);

    assertNotNull(result, "Serialized result err should not be null");
    assertEquals(2, result.length, "Result err without payload should be 2 bytes");
    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals((byte) 0, buffer.get(), "is_ok byte should be 0");
    assertEquals((byte) 0, buffer.get(), "has_value byte should be 0");
  }

  // ============== Mutation-killing tests for getTypeDiscriminator if/else-if chain ==============

  @Test
  @DisplayName("Get type discriminator for enum")
  void testGetTypeDiscriminatorEnum() throws ValidationException {
    final WitType enumType =
        WitType.enumType("color", java.util.Arrays.asList("red", "green", "blue"));
    final WitEnum value = WitEnum.of(enumType, "red");
    assertEquals(
        13, WitValueSerializer.getTypeDiscriminator(value), "Enum discriminator should be 13");
  }

  @Test
  @DisplayName("Get type discriminator for tuple")
  void testGetTypeDiscriminatorTuple() throws ValidationException {
    final WitTuple value = WitTuple.of(java.util.Arrays.asList(WitS32.of(1), WitString.of("a")));
    assertEquals(
        8, WitValueSerializer.getTypeDiscriminator(value), "Tuple discriminator should be 8");
  }

  @Test
  @DisplayName("Get type discriminator for flags")
  void testGetTypeDiscriminatorFlags() throws ValidationException {
    final WitType flagsType = WitType.flags("perms", java.util.Arrays.asList("read", "write"));
    final WitFlags value = WitFlags.of(flagsType, "read");
    assertEquals(
        16, WitValueSerializer.getTypeDiscriminator(value), "Flags discriminator should be 16");
  }

  @Test
  @DisplayName("Get type discriminator for result")
  void testGetTypeDiscriminatorResult() throws ValidationException {
    final WitType resultType =
        WitType.result(java.util.Optional.empty(), java.util.Optional.empty());
    final WitResult value = WitResult.ok(resultType);
    assertEquals(
        15, WitValueSerializer.getTypeDiscriminator(value), "Result discriminator should be 15");
  }

  @Test
  @DisplayName("Get type discriminator for owned WitResource")
  void testGetTypeDiscriminatorOwnedResource() throws ValidationException {
    final WitResource value = WitResource.own("TestRes", 1);
    assertEquals(
        22,
        WitValueSerializer.getTypeDiscriminator(value),
        "Owned WitResource discriminator should be 22");
  }

  @Test
  @DisplayName("Get type discriminator for borrowed WitResource")
  void testGetTypeDiscriminatorBorrowedResource() throws ValidationException {
    final WitResource value = WitResource.borrow("TestRes", 1);
    assertEquals(
        23,
        WitValueSerializer.getTypeDiscriminator(value),
        "Borrowed WitResource discriminator should be 23");
  }

  // ============== Mutation-killing tests for serialize() instanceof chain ==============

  @Test
  @DisplayName("Serialize WitRecord via serialize() dispatch")
  void testSerializeRecordViaDispatch() throws ValidationException {
    final java.util.Map<String, WitValue> fields = new java.util.LinkedHashMap<>();
    fields.put("x", WitS32.of(1));
    final WitRecord value = WitRecord.of(fields);
    final byte[] result = WitValueSerializer.serialize(value);
    assertNotNull(result, "Record serialization should not return null");
    assertTrue(result.length > 4, "Record should have field count + field data");
  }

  @Test
  @DisplayName("Serialize WitVariant via serialize() dispatch")
  void testSerializeVariantViaDispatch() throws ValidationException {
    final WitType variantType =
        WitType.variant("v", java.util.Map.of("a", java.util.Optional.empty()));
    final WitVariant value = WitVariant.of(variantType, "a");
    final byte[] result = WitValueSerializer.serialize(value);
    assertNotNull(result, "Variant serialization should not return null");
  }

  @Test
  @DisplayName("Serialize WitTuple via serialize() dispatch")
  void testSerializeTupleViaDispatch() throws ValidationException {
    final WitTuple value = WitTuple.of(java.util.Arrays.asList(WitS32.of(1)));
    final byte[] result = WitValueSerializer.serialize(value);
    assertNotNull(result, "Tuple serialization should not return null");
    assertTrue(result.length > 4, "Tuple should have element count + element data");
  }

  @Test
  @DisplayName("Serialize owned WitResource via serialize() dispatch")
  void testSerializeOwnedResourceViaDispatch() throws ValidationException {
    final WitResource value = WitResource.own("Res", 1);
    final byte[] result = WitValueSerializer.serialize(value);
    assertNotNull(result, "Owned WitResource serialization should not return null");
    assertTrue(result.length >= 12, "Owned resource should have name length + handle");
  }

  @Test
  @DisplayName("Serialize borrowed WitResource via serialize() dispatch")
  void testSerializeBorrowedResourceViaDispatch() throws ValidationException {
    final WitResource value = WitResource.borrow("Res", 1);
    final byte[] result = WitValueSerializer.serialize(value);
    assertNotNull(result, "Borrowed WitResource serialization should not return null");
    assertTrue(result.length >= 12, "Borrowed resource should have name length + handle");
  }

  // ============== Mutation-killing tests for constant boundary mutations ==============

  @Test
  @DisplayName("Serialize enum - verify exact name length in first 4 bytes (constant 4 boundary)")
  void testSerializeEnumExactNameLength() throws ValidationException {
    final WitType enumType = WitType.enumType("color", java.util.Arrays.asList("red"));
    final WitEnum value = WitEnum.of(enumType, "red");
    final byte[] result = WitValueSerializer.serialize(value);

    // Verify: [name_length: u32][name: UTF-8]
    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    final int nameLength = buffer.getInt();
    assertEquals(3, nameLength, "Name length should be 3 for 'red'");
    assertEquals(
        4 + 3, result.length, "Total length should be 4 (name length int) + 3 (name bytes)");

    // Read the name back to verify it's correct
    final byte[] nameBytes = new byte[nameLength];
    buffer.get(nameBytes);
    assertEquals("red", new String(nameBytes, StandardCharsets.UTF_8), "Name should be 'red'");
  }

  @Test
  @DisplayName(
      "Serialize flags - verify exact name length prefix for each flag (constant 4 boundary)")
  void testSerializeFlagsExactNameLength() throws ValidationException {
    final WitType flagsType = WitType.flags("perms", java.util.Arrays.asList("r"));
    final WitFlags value = WitFlags.of(flagsType, "r");
    final byte[] result = WitValueSerializer.serialize(value);

    // Verify: [count: u32][name_length: u32][name: UTF-8]
    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    final int count = buffer.getInt();
    assertEquals(1, count, "Flag count should be 1");
    final int nameLength = buffer.getInt();
    assertEquals(1, nameLength, "Name length for 'r' should be 1");
    assertEquals(4 + 4 + 1, result.length, "Total: 4 (count) + 4 (name length) + 1 (name byte)");
  }

  @Test
  @DisplayName("Serialize borrow - verify handle uses nativeHandle when >= 0")
  void testSerializeBorrowHandleBoundary() throws ValidationException {
    // Targets line 654: nativeHandle >= 0 boundary (mutant changes 0 to 1)
    final WitBorrow value = WitBorrow.of("T", 0L);
    final byte[] result = WitValueSerializer.serialize(value);

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    final int nameLen = buffer.getInt();
    buffer.position(4 + nameLen);
    final long handle = buffer.getLong();
    assertEquals(0L, handle, "Handle 0 should serialize as 0 (>= 0 condition)");
  }

  @Test
  @DisplayName("Serialize borrow with handle value 1")
  void testSerializeBorrowHandleOne() throws ValidationException {
    // Targets line 654: nativeHandle >= 0 boundary (mutant changes 0 to 1)
    final WitBorrow value = WitBorrow.of("T", 1L);
    final byte[] result = WitValueSerializer.serialize(value);

    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    final int nameLen = buffer.getInt();
    buffer.position(4 + nameLen);
    final long handle = buffer.getLong();
    assertEquals(1L, handle, "Handle 1 should serialize as 1");
  }

  // ============== Serialize primitive type dispatch tests ==============

  @Test
  @DisplayName("Serialize WitS8 via serialize() dispatch")
  void testSerializeS8ViaDispatch() throws ValidationException {
    final WitS8 value = WitS8.of((byte) 42);
    final byte[] result = WitValueSerializer.serialize(value);
    assertEquals(1, result.length, "S8 should be 1 byte");
    assertEquals((byte) 42, result[0], "Byte value should be 42");
  }

  @Test
  @DisplayName("Serialize WitS16 via serialize() dispatch")
  void testSerializeS16ViaDispatch() throws ValidationException {
    final WitS16 value = WitS16.of((short) 1000);
    final byte[] result = WitValueSerializer.serialize(value);
    assertEquals(2, result.length, "S16 should be 2 bytes");
    final ByteBuffer buf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals((short) 1000, buf.getShort(), "Value should be 1000");
  }

  @Test
  @DisplayName("Serialize WitU8 via serialize() dispatch")
  void testSerializeU8ViaDispatch() throws ValidationException {
    final WitU8 value = WitU8.of((byte) 0xFF);
    final byte[] result = WitValueSerializer.serialize(value);
    assertEquals(1, result.length, "U8 should be 1 byte");
  }

  @Test
  @DisplayName("Serialize WitU16 via serialize() dispatch")
  void testSerializeU16ViaDispatch() throws ValidationException {
    final WitU16 value = WitU16.of((short) 0x7FFF);
    final byte[] result = WitValueSerializer.serialize(value);
    assertEquals(2, result.length, "U16 should be 2 bytes");
  }

  @Test
  @DisplayName("Serialize WitU32 via serialize() dispatch")
  void testSerializeU32ViaDispatch() throws ValidationException {
    final WitU32 value = WitU32.of(42);
    final byte[] result = WitValueSerializer.serialize(value);
    assertEquals(4, result.length, "U32 should be 4 bytes");
  }

  @Test
  @DisplayName("Serialize WitU64 via serialize() dispatch")
  void testSerializeU64ViaDispatch() throws ValidationException {
    final WitU64 value = WitU64.of(42L);
    final byte[] result = WitValueSerializer.serialize(value);
    assertEquals(8, result.length, "U64 should be 8 bytes");
  }

  @Test
  @DisplayName("Serialize WitFloat32 via serialize() dispatch")
  void testSerializeFloat32ViaDispatch() throws ValidationException {
    final WitFloat32 value = WitFloat32.of(3.14f);
    final byte[] result = WitValueSerializer.serialize(value);
    assertEquals(4, result.length, "Float32 should be 4 bytes");
  }

  // ============== InlineConstant mutation-killing: exact byte layout tests ==============

  @Test
  @DisplayName(
      "Serialize record - verify exact byte positions for field count, name length, discriminator,"
          + " data length")
  void testSerializeRecordExactByteLayout() throws ValidationException {
    final java.util.Map<String, WitValue> fields = new java.util.LinkedHashMap<>();
    fields.put("a", WitBool.of(true));
    final WitRecord record = WitRecord.of(fields);
    final byte[] result = WitValueSerializer.serialize(record);

    final ByteBuffer buf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    // Position 0-3: field count = 1
    assertEquals(1, buf.getInt(), "Field count at bytes 0-3 must be exactly 1");
    // Position 4-7: name length = 1 (for "a")
    assertEquals(1, buf.getInt(), "Name length at bytes 4-7 must be exactly 1");
    // Position 8: name byte = 'a'
    assertEquals((byte) 'a', buf.get(), "Name byte at position 8 must be 'a'");
    // Position 9-12: discriminator = 1 (bool)
    assertEquals(1, buf.getInt(), "Discriminator at bytes 9-12 must be 1 (bool)");
    // Position 13-16: data length = 1 (bool is 1 byte)
    assertEquals(1, buf.getInt(), "Data length at bytes 13-16 must be exactly 1");
    // Position 17: data = 1 (true)
    assertEquals((byte) 1, buf.get(), "Data byte at position 17 must be 1 (true)");
    // Total size = 4 + 4 + 1 + 4 + 4 + 1 = 18
    assertEquals(18, result.length, "Total record size must be exactly 18 bytes");
    assertFalse(buf.hasRemaining(), "No trailing bytes should remain");
  }

  @Test
  @DisplayName("Serialize record with two fields - verify second field offset")
  void testSerializeRecordTwoFieldsOffsets() throws ValidationException {
    final java.util.Map<String, WitValue> fields = new java.util.LinkedHashMap<>();
    fields.put("x", WitS32.of(10));
    fields.put("y", WitS32.of(20));
    final WitRecord record = WitRecord.of(fields);
    final byte[] result = WitValueSerializer.serialize(record);

    final ByteBuffer buf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    // field count
    assertEquals(2, buf.getInt(), "Field count must be 2");
    // First field: name_len(4) + name(1) + disc(4) + data_len(4) + data(4) = 17
    int nameLen1 = buf.getInt();
    assertEquals(1, nameLen1, "First field name length must be 1");
    buf.position(buf.position() + nameLen1); // skip name
    assertEquals(2, buf.getInt(), "First field discriminator must be 2 (s32)");
    assertEquals(4, buf.getInt(), "First field data length must be 4");
    assertEquals(10, buf.getInt(), "First field value must be 10");
    // Second field at position 4 + 17 = 21
    int nameLen2 = buf.getInt();
    assertEquals(1, nameLen2, "Second field name length must be 1");
    buf.position(buf.position() + nameLen2); // skip name
    assertEquals(2, buf.getInt(), "Second field discriminator must be 2 (s32)");
    assertEquals(4, buf.getInt(), "Second field data length must be 4");
    assertEquals(20, buf.getInt(), "Second field value must be 20");
    assertFalse(buf.hasRemaining(), "No trailing bytes should remain");
  }

  @Test
  @DisplayName("Serialize tuple - verify exact byte positions for count, discriminators, lengths")
  void testSerializeTupleExactByteLayout() throws ValidationException {
    final WitTuple tuple = WitTuple.of(java.util.Arrays.asList(WitBool.of(false), WitS32.of(99)));
    final byte[] result = WitValueSerializer.serialize(tuple);

    final ByteBuffer buf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    // Position 0-3: element count = 2
    assertEquals(2, buf.getInt(), "Element count must be exactly 2");
    // Element 0: disc=1(bool), len=1, data=0(false)
    assertEquals(1, buf.getInt(), "Element 0 discriminator must be 1 (bool)");
    assertEquals(1, buf.getInt(), "Element 0 data length must be 1");
    assertEquals((byte) 0, buf.get(), "Element 0 data must be 0 (false)");
    // Element 1: disc=2(s32), len=4, data=99
    assertEquals(2, buf.getInt(), "Element 1 discriminator must be 2 (s32)");
    assertEquals(4, buf.getInt(), "Element 1 data length must be 4");
    assertEquals(99, buf.getInt(), "Element 1 data must be 99");
    // Total: 4 + (4+4+1) + (4+4+4) = 4 + 9 + 12 = 25
    assertEquals(25, result.length, "Total tuple size must be exactly 25 bytes");
    assertFalse(buf.hasRemaining(), "No trailing bytes should remain");
  }

  @Test
  @DisplayName(
      "Serialize list - verify exact discriminator and data length at each element position")
  void testSerializeListExactByteLayout() throws ValidationException {
    final WitList list = WitList.of(WitS32.of(7), WitS32.of(8));
    final byte[] result = WitValueSerializer.serialize(list);

    final ByteBuffer buf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(2, buf.getInt(), "Element count must be exactly 2");
    // Element 0
    assertEquals(2, buf.getInt(), "Element 0 discriminator must be 2 (s32)");
    assertEquals(4, buf.getInt(), "Element 0 data length must be 4");
    assertEquals(7, buf.getInt(), "Element 0 value must be 7");
    // Element 1
    assertEquals(2, buf.getInt(), "Element 1 discriminator must be 2 (s32)");
    assertEquals(4, buf.getInt(), "Element 1 data length must be 4");
    assertEquals(8, buf.getInt(), "Element 1 value must be 8");
    // Total: 4 + 2*(4+4+4) = 4 + 24 = 28
    assertEquals(28, result.length, "Total list size must be exactly 28 bytes");
  }

  @Test
  @DisplayName("Serialize string - verify exact total size is STRING_LENGTH_SIZE + utf8 length")
  void testSerializeStringExactTotalSize() throws ValidationException {
    final WitString value = WitString.of("AB");
    final byte[] result = WitValueSerializer.serialize(value);
    // STRING_LENGTH_SIZE = 4, "AB" = 2 bytes UTF-8, total = 6
    assertEquals(6, result.length, "String 'AB' must serialize to exactly 6 bytes");
    final ByteBuffer buf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(2, buf.getInt(), "String length field must be exactly 2");
    assertEquals((byte) 'A', buf.get(), "First string byte must be 'A'");
    assertEquals((byte) 'B', buf.get(), "Second string byte must be 'B'");
  }

  @Test
  @DisplayName("Serialize option some - verify exact total size: 1 + 4 + 4 + payload")
  void testSerializeOptionSomeExactTotalSize() throws ValidationException {
    final WitType optionType = WitType.option(WitType.createBool());
    final WitOption value = WitOption.some(optionType, WitBool.of(true));
    final byte[] result = WitValueSerializer.serialize(value);

    // is_some(1) + disc(4) + len(4) + data(1) = 10
    assertEquals(10, result.length, "Option some with bool must be exactly 10 bytes");
    final ByteBuffer buf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals((byte) 1, buf.get(), "is_some must be 1");
    assertEquals(1, buf.getInt(), "Discriminator must be 1 (bool)");
    assertEquals(1, buf.getInt(), "Payload length must be 1");
    assertEquals((byte) 1, buf.get(), "Payload data must be 1 (true)");
  }

  @Test
  @DisplayName(
      "Serialize result ok with payload - verify exact byte positions: is_ok, has_value, disc, len,"
          + " data")
  void testSerializeResultOkWithPayloadExactLayout() throws ValidationException {
    final WitType resultType =
        WitType.result(java.util.Optional.of(WitType.createBool()), java.util.Optional.empty());
    final WitResult value = WitResult.ok(resultType, WitBool.of(false));
    final byte[] result = WitValueSerializer.serialize(value);

    // is_ok(1) + has_value(1) + disc(4) + len(4) + data(1) = 11
    assertEquals(11, result.length, "Result ok with bool must be exactly 11 bytes");
    final ByteBuffer buf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals((byte) 1, buf.get(), "is_ok byte must be 1");
    assertEquals((byte) 1, buf.get(), "has_value byte must be 1");
    assertEquals(1, buf.getInt(), "Discriminator must be 1 (bool)");
    assertEquals(1, buf.getInt(), "Payload length must be 1");
    assertEquals((byte) 0, buf.get(), "Payload data must be 0 (false)");
  }

  @Test
  @DisplayName("Serialize result err with payload - verify is_ok is exactly 0")
  void testSerializeResultErrWithPayloadExactLayout() throws ValidationException {
    final WitType resultType =
        WitType.result(java.util.Optional.empty(), java.util.Optional.of(WitType.createBool()));
    final WitResult value = WitResult.err(resultType, WitBool.of(true));
    final byte[] result = WitValueSerializer.serialize(value);

    // is_ok(1) + has_value(1) + disc(4) + len(4) + data(1) = 11
    assertEquals(11, result.length, "Result err with bool must be exactly 11 bytes");
    final ByteBuffer buf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals((byte) 0, buf.get(), "is_ok byte must be 0 for err");
    assertEquals((byte) 1, buf.get(), "has_value byte must be 1");
    assertEquals(1, buf.getInt(), "Discriminator must be 1 (bool)");
    assertEquals(1, buf.getInt(), "Payload length must be 1");
    assertEquals((byte) 1, buf.get(), "Payload data must be 1 (true)");
  }

  @Test
  @DisplayName("Serialize variant with payload - verify exact total size and byte positions")
  void testSerializeVariantWithPayloadExactLayout() throws ValidationException {
    final WitType variantType =
        WitType.variant("v", java.util.Map.of("ok", java.util.Optional.of(WitType.createBool())));
    final WitVariant value = WitVariant.of(variantType, "ok", WitBool.of(true));
    final byte[] result = WitValueSerializer.serialize(value);

    // name_len(4) + name(2) + has_payload(1) + disc(4) + data_len(4) + data(1) = 16
    assertEquals(
        16, result.length, "Variant with bool payload and name 'ok' must be exactly 16 bytes");
    final ByteBuffer buf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(2, buf.getInt(), "Name length must be 2 for 'ok'");
    assertEquals((byte) 'o', buf.get(), "First name byte must be 'o'");
    assertEquals((byte) 'k', buf.get(), "Second name byte must be 'k'");
    assertEquals((byte) 1, buf.get(), "has_payload must be 1");
    assertEquals(1, buf.getInt(), "Payload discriminator must be 1 (bool)");
    assertEquals(1, buf.getInt(), "Payload data length must be 1");
    assertEquals((byte) 1, buf.get(), "Payload data must be 1 (true)");
  }

  @Test
  @DisplayName("Serialize variant without payload - verify exact total size")
  void testSerializeVariantWithoutPayloadExactLayout() throws ValidationException {
    final WitType variantType =
        WitType.variant("v", java.util.Map.of("n", java.util.Optional.empty()));
    final WitVariant value = WitVariant.of(variantType, "n", null);
    final byte[] result = WitValueSerializer.serialize(value);

    // name_len(4) + name(1) + has_payload(1) = 6
    assertEquals(6, result.length, "Variant without payload and name 'n' must be exactly 6 bytes");
    final ByteBuffer buf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(1, buf.getInt(), "Name length must be 1");
    assertEquals((byte) 'n', buf.get(), "Name byte must be 'n'");
    assertEquals((byte) 0, buf.get(), "has_payload must be 0");
  }

  @Test
  @DisplayName("Serialize flags - verify exact byte layout with two flags")
  void testSerializeFlagsExactByteLayout() throws ValidationException {
    final WitType flagsType = WitType.flags("p", java.util.Arrays.asList("a", "b"));
    // Use TreeSet ordering to guarantee iteration order
    final java.util.Set<String> setFlags = new java.util.TreeSet<>();
    setFlags.add("a");
    setFlags.add("b");
    final WitFlags value = WitFlags.of(flagsType, setFlags);
    final byte[] result = WitValueSerializer.serialize(value);

    final ByteBuffer buf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(2, buf.getInt(), "Flag count must be 2");
    // Each flag: name_len(4) + name(1) = 5
    // Total: 4 + 5 + 5 = 14
    assertEquals(14, result.length, "Flags with two single-char names must be exactly 14 bytes");
  }

  @Test
  @DisplayName("Serialize own - verify exact handle value 0 at boundary")
  void testSerializeOwnHandleBoundaryZero() throws ValidationException {
    final WitOwn value = WitOwn.of("T", 0L);
    final byte[] result = WitValueSerializer.serialize(value);

    final ByteBuffer buf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    final int nameLen = buf.getInt();
    buf.position(4 + nameLen);
    assertEquals(0L, buf.getLong(), "Handle 0 should serialize as 0 (>= 0 boundary)");
  }

  @Test
  @DisplayName("Serialize own - verify exact total size: 4 + name + 8")
  void testSerializeOwnExactTotalSize() throws ValidationException {
    final WitOwn value = WitOwn.of("AB", 5L);
    final byte[] result = WitValueSerializer.serialize(value);
    // 4 (name length) + 2 (name "AB") + 8 (handle i64) = 14
    assertEquals(14, result.length, "Own with name 'AB' must be exactly 14 bytes");
  }

  @Test
  @DisplayName("Serialize s32 value 0 - verify not mutated to 1")
  void testSerializeS32Zero() throws ValidationException {
    final WitS32 value = WitS32.of(0);
    final byte[] result = WitValueSerializer.serialize(value);
    final ByteBuffer buf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(0, buf.getInt(), "S32 value 0 must serialize as exactly 0");
  }

  @Test
  @DisplayName("Serialize s32 value 1 - verify not mutated to 0")
  void testSerializeS32One() throws ValidationException {
    final WitS32 value = WitS32.of(1);
    final byte[] result = WitValueSerializer.serialize(value);
    final ByteBuffer buf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(1, buf.getInt(), "S32 value 1 must serialize as exactly 1");
  }

  @Test
  @DisplayName("Serialize s64 value 0 - verify not mutated")
  void testSerializeS64Zero() throws ValidationException {
    final WitS64 value = WitS64.of(0L);
    final byte[] result = WitValueSerializer.serialize(value);
    final ByteBuffer buf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(0L, buf.getLong(), "S64 value 0 must serialize as exactly 0");
  }

  @Test
  @DisplayName("Serialize s64 value 1 - verify not mutated")
  void testSerializeS64One() throws ValidationException {
    final WitS64 value = WitS64.of(1L);
    final byte[] result = WitValueSerializer.serialize(value);
    final ByteBuffer buf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(1L, buf.getLong(), "S64 value 1 must serialize as exactly 1");
  }

  @Test
  @DisplayName("Serialize s8 value 0 - verify not mutated")
  void testSerializeS8Zero() throws ValidationException {
    final WitS8 value = WitS8.of((byte) 0);
    final byte[] result = WitValueSerializer.serialize(value);
    assertEquals((byte) 0, result[0], "S8 value 0 must serialize as exactly 0");
  }

  @Test
  @DisplayName("Serialize s8 value 1 - verify not mutated")
  void testSerializeS8One() throws ValidationException {
    final WitS8 value = WitS8.of((byte) 1);
    final byte[] result = WitValueSerializer.serialize(value);
    assertEquals((byte) 1, result[0], "S8 value 1 must serialize as exactly 1");
  }

  @Test
  @DisplayName("Serialize u8 value 0 - verify not mutated")
  void testSerializeU8Zero() throws ValidationException {
    final WitU8 value = WitU8.of((byte) 0);
    final byte[] result = WitValueSerializer.serialize(value);
    assertEquals((byte) 0, result[0], "U8 value 0 must serialize as exactly 0");
  }

  @Test
  @DisplayName("Serialize float32 value 0 - verify not mutated")
  void testSerializeFloat32Zero() throws ValidationException {
    final WitFloat32 value = WitFloat32.of(0.0f);
    final byte[] result = WitValueSerializer.serialize(value);
    final ByteBuffer buf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(0.0f, buf.getFloat(), "Float32 value 0.0 must serialize as exactly 0.0");
  }

  @Test
  @DisplayName("Serialize float32 value 1 - verify not mutated")
  void testSerializeFloat32One() throws ValidationException {
    final WitFloat32 value = WitFloat32.of(1.0f);
    final byte[] result = WitValueSerializer.serialize(value);
    final ByteBuffer buf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(1.0f, buf.getFloat(), "Float32 value 1.0 must serialize as exactly 1.0");
  }

  @Test
  @DisplayName("Serialize float64 value 1 - verify not mutated")
  void testSerializeFloat64One() throws ValidationException {
    final WitFloat64 value = WitFloat64.of(1.0);
    final byte[] result = WitValueSerializer.serialize(value);
    final ByteBuffer buf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(1.0, buf.getDouble(), "Float64 value 1.0 must serialize as exactly 1.0");
  }

  @Test
  @DisplayName("Serialize record with one field - verify total size is exactly correct")
  void testSerializeRecordSingleFieldExactSize() throws ValidationException {
    final java.util.Map<String, WitValue> fields = new java.util.LinkedHashMap<>();
    fields.put("b", WitS32.of(0));
    final WitRecord record = WitRecord.of(fields);
    final byte[] result = WitValueSerializer.serialize(record);

    // count(4) + name_len(4) + name(1) + disc(4) + data_len(4) + data(4) = 21
    assertEquals(21, result.length, "Record with field 'b'->s32(0) must be exactly 21 bytes");
  }

  @Test
  @DisplayName("Serialize tuple with single element - verify count is exactly 1")
  void testSerializeSingleElementTupleExact() throws ValidationException {
    final WitTuple tuple = WitTuple.of(java.util.Arrays.asList(WitS32.of(42)));
    final byte[] result = WitValueSerializer.serialize(tuple);

    // count(4) + disc(4) + len(4) + data(4) = 16
    assertEquals(16, result.length, "Tuple with single s32 must be exactly 16 bytes");
    final ByteBuffer buf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(1, buf.getInt(), "Tuple element count must be exactly 1");
    assertEquals(2, buf.getInt(), "Element discriminator must be 2 (s32)");
    assertEquals(4, buf.getInt(), "Element data length must be 4");
    assertEquals(42, buf.getInt(), "Element value must be 42");
  }

  @Test
  @DisplayName("Serialize result with totalSize exactly 2 for no-payload case")
  void testSerializeResultNoPayloadExactSize() throws ValidationException {
    final WitType resultType =
        WitType.result(java.util.Optional.empty(), java.util.Optional.empty());
    final WitResult okResult = WitResult.ok(resultType);
    final byte[] okBytes = WitValueSerializer.serialize(okResult);
    // totalSize = 2 (is_ok + has_value)
    assertEquals(2, okBytes.length, "Result without payload must be exactly 2 bytes");

    final WitResult errResult = WitResult.err(resultType);
    final byte[] errBytes = WitValueSerializer.serialize(errResult);
    assertEquals(2, errBytes.length, "Result err without payload must be exactly 2 bytes");
  }

  @Test
  @DisplayName("Serialize option none - verify totalSize is exactly 1")
  void testSerializeOptionNoneExactSize() throws ValidationException {
    final WitType optionType = WitType.option(WitType.createS32());
    final WitOption value = WitOption.none(optionType);
    final byte[] result = WitValueSerializer.serialize(value);
    assertEquals(1, result.length, "Option none must be exactly 1 byte total");
  }

  @Test
  @DisplayName("Serialize s16 value 0 and 1 - verify not mutated")
  void testSerializeS16BoundaryValues() throws ValidationException {
    final byte[] zeroResult = WitValueSerializer.serialize(WitS16.of((short) 0));
    final ByteBuffer zeroBuf = ByteBuffer.wrap(zeroResult).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals((short) 0, zeroBuf.getShort(), "S16 value 0 must be exactly 0");

    final byte[] oneResult = WitValueSerializer.serialize(WitS16.of((short) 1));
    final ByteBuffer oneBuf = ByteBuffer.wrap(oneResult).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals((short) 1, oneBuf.getShort(), "S16 value 1 must be exactly 1");
  }

  @Test
  @DisplayName("Serialize u16 value 0 and 1 - verify not mutated")
  void testSerializeU16BoundaryValues() throws ValidationException {
    final byte[] zeroResult = WitValueSerializer.serialize(WitU16.of((short) 0));
    final ByteBuffer zeroBuf = ByteBuffer.wrap(zeroResult).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals((short) 0, zeroBuf.getShort(), "U16 value 0 must be exactly 0");

    final byte[] oneResult = WitValueSerializer.serialize(WitU16.of((short) 1));
    final ByteBuffer oneBuf = ByteBuffer.wrap(oneResult).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals((short) 1, oneBuf.getShort(), "U16 value 1 must be exactly 1");
  }

  @Test
  @DisplayName("Serialize u32 value 0 and 1 - verify not mutated")
  void testSerializeU32BoundaryValues() throws ValidationException {
    final byte[] zeroResult = WitValueSerializer.serialize(WitU32.of(0));
    final ByteBuffer zeroBuf = ByteBuffer.wrap(zeroResult).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(0, zeroBuf.getInt(), "U32 value 0 must be exactly 0");

    final byte[] oneResult = WitValueSerializer.serialize(WitU32.of(1));
    final ByteBuffer oneBuf = ByteBuffer.wrap(oneResult).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(1, oneBuf.getInt(), "U32 value 1 must be exactly 1");
  }

  @Test
  @DisplayName("Serialize u64 value 0 and 1 - verify not mutated")
  void testSerializeU64BoundaryValues() throws ValidationException {
    final byte[] zeroResult = WitValueSerializer.serialize(WitU64.of(0L));
    final ByteBuffer zeroBuf = ByteBuffer.wrap(zeroResult).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(0L, zeroBuf.getLong(), "U64 value 0 must be exactly 0");

    final byte[] oneResult = WitValueSerializer.serialize(WitU64.of(1L));
    final ByteBuffer oneBuf = ByteBuffer.wrap(oneResult).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(1L, oneBuf.getLong(), "U64 value 1 must be exactly 1");
  }

  @Test
  @DisplayName("Serialize char value 0 and 1 - verify not mutated")
  void testSerializeCharBoundaryValues() throws ValidationException {
    final byte[] zeroResult = WitValueSerializer.serialize(WitChar.of(0));
    final ByteBuffer zeroBuf = ByteBuffer.wrap(zeroResult).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(0, zeroBuf.getInt(), "Char codepoint 0 must be exactly 0");

    final byte[] oneResult = WitValueSerializer.serialize(WitChar.of(1));
    final ByteBuffer oneBuf = ByteBuffer.wrap(oneResult).order(ByteOrder.LITTLE_ENDIAN);
    assertEquals(1, oneBuf.getInt(), "Char codepoint 1 must be exactly 1");
  }
}
