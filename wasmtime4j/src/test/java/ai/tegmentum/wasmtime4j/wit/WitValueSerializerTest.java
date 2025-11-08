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
}
