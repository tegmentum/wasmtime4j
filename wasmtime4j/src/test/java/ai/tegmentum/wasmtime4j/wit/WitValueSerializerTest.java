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
}
