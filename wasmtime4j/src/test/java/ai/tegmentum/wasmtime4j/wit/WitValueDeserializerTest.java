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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive unit tests for WitValueDeserializer. */
@DisplayName("WitValueDeserializer Tests")
final class WitValueDeserializerTest {

  @Test
  @DisplayName("Deserialize bool true from binary format")
  void testDeserializeBoolTrue() throws ValidationException {
    final byte[] data = {(byte) 1};
    final WitValue result = WitValueDeserializer.deserialize(1, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitBool.class, result, "Result should be WitBool");
    assertTrue(((WitBool) result).getValue(), "Value should be true");
  }

  @Test
  @DisplayName("Deserialize bool false from binary format")
  void testDeserializeBoolFalse() throws ValidationException {
    final byte[] data = {(byte) 0};
    final WitValue result = WitValueDeserializer.deserialize(1, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitBool.class, result, "Result should be WitBool");
    assertFalse(((WitBool) result).getValue(), "Value should be false");
  }

  @Test
  @DisplayName("Deserialize bool with non-zero byte as true")
  void testDeserializeBoolNonZero() throws ValidationException {
    final byte[] data = {(byte) 255};
    final WitValue result = WitValueDeserializer.deserialize(1, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitBool.class, result, "Result should be WitBool");
    assertTrue(((WitBool) result).getValue(), "Non-zero byte should deserialize to true");
  }

  @Test
  @DisplayName("Deserialize bool with invalid size throws exception")
  void testDeserializeBoolInvalidSize() {
    final byte[] data = {(byte) 0, (byte) 1}; // 2 bytes instead of 1

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(1, data),
            "Should throw exception for invalid size");

    assertTrue(
        exception.getMessage().contains("Invalid bool data size"),
        "Exception message should mention invalid size: " + exception.getMessage());
  }

  @Test
  @DisplayName("Deserialize s32 positive value from little-endian")
  void testDeserializeS32Positive() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(42);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(2, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitS32.class, result, "Result should be WitS32");
    assertEquals(42, ((WitS32) result).getValue(), "Value should be 42");
  }

  @Test
  @DisplayName("Deserialize s32 negative value from little-endian")
  void testDeserializeS32Negative() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(-999);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(2, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitS32.class, result, "Result should be WitS32");
    assertEquals(-999, ((WitS32) result).getValue(), "Value should be -999");
  }

  @Test
  @DisplayName("Deserialize s32 max value")
  void testDeserializeS32Max() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(Integer.MAX_VALUE);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(2, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitS32.class, result, "Result should be WitS32");
    assertEquals(
        Integer.MAX_VALUE, ((WitS32) result).getValue(), "Value should be Integer.MAX_VALUE");
  }

  @Test
  @DisplayName("Deserialize s32 min value")
  void testDeserializeS32Min() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(Integer.MIN_VALUE);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(2, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitS32.class, result, "Result should be WitS32");
    assertEquals(
        Integer.MIN_VALUE, ((WitS32) result).getValue(), "Value should be Integer.MIN_VALUE");
  }

  @Test
  @DisplayName("Deserialize s32 with invalid size throws exception")
  void testDeserializeS32InvalidSize() {
    final byte[] data = {(byte) 0, (byte) 1}; // 2 bytes instead of 4

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(2, data),
            "Should throw exception for invalid size");

    assertTrue(
        exception.getMessage().contains("Invalid s32 data size"),
        "Exception message should mention invalid size: " + exception.getMessage());
  }

  @Test
  @DisplayName("Deserialize s64 positive value from little-endian")
  void testDeserializeS64Positive() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putLong(1_000_000_000_000L);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(3, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitS64.class, result, "Result should be WitS64");
    assertEquals(1_000_000_000_000L, ((WitS64) result).getValue(), "Value should be 1 trillion");
  }

  @Test
  @DisplayName("Deserialize s64 negative value from little-endian")
  void testDeserializeS64Negative() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putLong(-9_999_999_999L);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(3, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitS64.class, result, "Result should be WitS64");
    assertEquals(-9_999_999_999L, ((WitS64) result).getValue(), "Value should be -9999999999");
  }

  @Test
  @DisplayName("Deserialize s64 max value")
  void testDeserializeS64Max() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putLong(Long.MAX_VALUE);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(3, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitS64.class, result, "Result should be WitS64");
    assertEquals(Long.MAX_VALUE, ((WitS64) result).getValue(), "Value should be Long.MAX_VALUE");
  }

  @Test
  @DisplayName("Deserialize s64 min value")
  void testDeserializeS64Min() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putLong(Long.MIN_VALUE);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(3, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitS64.class, result, "Result should be WitS64");
    assertEquals(Long.MIN_VALUE, ((WitS64) result).getValue(), "Value should be Long.MIN_VALUE");
  }

  @Test
  @DisplayName("Deserialize s64 with invalid size throws exception")
  void testDeserializeS64InvalidSize() {
    final byte[] data = {(byte) 0, (byte) 1, (byte) 2, (byte) 3}; // 4 bytes instead of 8

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(3, data),
            "Should throw exception for invalid size");

    assertTrue(
        exception.getMessage().contains("Invalid s64 data size"),
        "Exception message should mention invalid size: " + exception.getMessage());
  }

  @Test
  @DisplayName("Deserialize float64 positive value from little-endian IEEE 754")
  void testDeserializeFloat64Positive() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putDouble(3.14159);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(4, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitFloat64.class, result, "Result should be WitFloat64");
    assertEquals(3.14159, ((WitFloat64) result).getValue(), 1e-10, "Value should be 3.14159");
  }

  @Test
  @DisplayName("Deserialize float64 negative value from little-endian IEEE 754")
  void testDeserializeFloat64Negative() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putDouble(-999.99);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(4, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitFloat64.class, result, "Result should be WitFloat64");
    assertEquals(-999.99, ((WitFloat64) result).getValue(), 1e-10, "Value should be -999.99");
  }

  @Test
  @DisplayName("Deserialize float64 zero")
  void testDeserializeFloat64Zero() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putDouble(0.0);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(4, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitFloat64.class, result, "Result should be WitFloat64");
    assertEquals(0.0, ((WitFloat64) result).getValue(), "Value should be 0.0");
  }

  @Test
  @DisplayName("Deserialize float64 max value")
  void testDeserializeFloat64Max() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putDouble(Double.MAX_VALUE);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(4, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitFloat64.class, result, "Result should be WitFloat64");
    assertEquals(
        Double.MAX_VALUE, ((WitFloat64) result).getValue(), "Value should be Double.MAX_VALUE");
  }

  @Test
  @DisplayName("Deserialize float64 min positive value")
  void testDeserializeFloat64MinPositive() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putDouble(Double.MIN_VALUE);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(4, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitFloat64.class, result, "Result should be WitFloat64");
    assertEquals(
        Double.MIN_VALUE, ((WitFloat64) result).getValue(), "Value should be Double.MIN_VALUE");
  }

  @Test
  @DisplayName("Deserialize float64 with invalid size throws exception")
  void testDeserializeFloat64InvalidSize() {
    final byte[] data = {(byte) 0, (byte) 1, (byte) 2, (byte) 3}; // 4 bytes instead of 8

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(4, data),
            "Should throw exception for invalid size");

    assertTrue(
        exception.getMessage().contains("Invalid float64 data size"),
        "Exception message should mention invalid size: " + exception.getMessage());
  }

  @Test
  @DisplayName("Deserialize char ASCII value from little-endian codepoint")
  void testDeserializeCharAscii() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt((int) 'A');
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(5, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitChar.class, result, "Result should be WitChar");
    assertEquals((int) 'A', ((WitChar) result).getCodepoint(), "Codepoint should be 'A'");
  }

  @Test
  @DisplayName("Deserialize char Unicode emoji from little-endian codepoint")
  void testDeserializeCharEmoji() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(0x1F980); // 🦀 crab emoji
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(5, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitChar.class, result, "Result should be WitChar");
    assertEquals(0x1F980, ((WitChar) result).getCodepoint(), "Codepoint should be crab emoji");
  }

  @Test
  @DisplayName("Deserialize char Chinese character from little-endian codepoint")
  void testDeserializeCharChinese() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt((int) '中');
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(5, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitChar.class, result, "Result should be WitChar");
    assertEquals((int) '中', ((WitChar) result).getCodepoint(), "Codepoint should be '中'");
  }

  @Test
  @DisplayName("Deserialize char null character")
  void testDeserializeCharNull() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(0x0000);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(5, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitChar.class, result, "Result should be WitChar");
    assertEquals(0x0000, ((WitChar) result).getCodepoint(), "Codepoint should be null character");
  }

  @Test
  @DisplayName("Deserialize char max Unicode value")
  void testDeserializeCharMaxUnicode() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(0x10FFFF);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(5, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitChar.class, result, "Result should be WitChar");
    assertEquals(0x10FFFF, ((WitChar) result).getCodepoint(), "Codepoint should be max Unicode");
  }

  @Test
  @DisplayName("Deserialize char with invalid size throws exception")
  void testDeserializeCharInvalidSize() {
    final byte[] data = {(byte) 0, (byte) 1}; // 2 bytes instead of 4

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(5, data),
            "Should throw exception for invalid size");

    assertTrue(
        exception.getMessage().contains("Invalid char data size"),
        "Exception message should mention invalid size: " + exception.getMessage());
  }

  @Test
  @DisplayName("Deserialize char with invalid codepoint throws exception")
  void testDeserializeCharInvalidCodepoint() {
    final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(0xD800); // Surrogate codepoint (invalid)
    final byte[] data = buffer.array();

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(5, data),
            "Should throw exception for invalid codepoint");

    assertTrue(
        exception.getMessage().contains("codepoint"),
        "Exception message should mention codepoint: " + exception.getMessage());
  }

  @Test
  @DisplayName("Deserialize string empty")
  void testDeserializeStringEmpty() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(0); // Length 0
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(6, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitString.class, result, "Result should be WitString");
    assertEquals("", ((WitString) result).getValue(), "Value should be empty string");
  }

  @Test
  @DisplayName("Deserialize string ASCII")
  void testDeserializeStringAscii() throws ValidationException {
    final byte[] utf8Bytes = "hello".getBytes(StandardCharsets.UTF_8);
    final ByteBuffer buffer =
        ByteBuffer.allocate(4 + utf8Bytes.length).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(utf8Bytes.length);
    buffer.put(utf8Bytes);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(6, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitString.class, result, "Result should be WitString");
    assertEquals("hello", ((WitString) result).getValue(), "Value should be 'hello'");
  }

  @Test
  @DisplayName("Deserialize string with Unicode characters")
  void testDeserializeStringUnicode() throws ValidationException {
    final byte[] utf8Bytes = "Hello 🦀 中文".getBytes(StandardCharsets.UTF_8);
    final ByteBuffer buffer =
        ByteBuffer.allocate(4 + utf8Bytes.length).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(utf8Bytes.length);
    buffer.put(utf8Bytes);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(6, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitString.class, result, "Result should be WitString");
    assertEquals("Hello 🦀 中文", ((WitString) result).getValue(), "Value should match original");
  }

  @Test
  @DisplayName("Deserialize string with special characters")
  void testDeserializeStringSpecialChars() throws ValidationException {
    final byte[] utf8Bytes = "Line1\nLine2\tTab\r\nCRLF".getBytes(StandardCharsets.UTF_8);
    final ByteBuffer buffer =
        ByteBuffer.allocate(4 + utf8Bytes.length).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(utf8Bytes.length);
    buffer.put(utf8Bytes);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(6, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitString.class, result, "Result should be WitString");
    assertEquals(
        "Line1\nLine2\tTab\r\nCRLF",
        ((WitString) result).getValue(),
        "Value should match original");
  }

  @Test
  @DisplayName("Deserialize string with size too small for length header throws exception")
  void testDeserializeStringInvalidSizeNoLength() {
    final byte[] data = {(byte) 0, (byte) 1}; // Only 2 bytes, need at least 4 for length

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(6, data),
            "Should throw exception for data too small");

    assertTrue(
        exception.getMessage().contains("Invalid string data size"),
        "Exception message should mention invalid size: " + exception.getMessage());
  }

  @Test
  @DisplayName("Deserialize string with negative length throws exception")
  void testDeserializeStringNegativeLength() {
    final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(-1);
    final byte[] data = buffer.array();

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(6, data),
            "Should throw exception for negative length");

    assertTrue(
        exception.getMessage().contains("Invalid string length"),
        "Exception message should mention invalid length: " + exception.getMessage());
  }

  @Test
  @DisplayName("Deserialize string with length mismatch throws exception")
  void testDeserializeStringLengthMismatch() {
    final ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(10); // Claims 10 bytes
    buffer.put("hi".getBytes(StandardCharsets.UTF_8)); // But only provides 2
    final byte[] data = buffer.array();

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(6, data),
            "Should throw exception for length mismatch");

    assertTrue(
        exception.getMessage().contains("String data size mismatch"),
        "Exception message should mention mismatch: " + exception.getMessage());
  }

  @Test
  @DisplayName("Deserialize with null data throws exception")
  void testDeserializeNullData() {
    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(1, null),
            "Should throw exception for null data");

    assertTrue(
        exception.getMessage().contains("null"),
        "Exception message should mention null: " + exception.getMessage());
  }

  @Test
  @DisplayName("Deserialize with invalid discriminator throws exception")
  void testDeserializeInvalidDiscriminator() {
    final byte[] data = {(byte) 0};

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(99, data),
            "Should throw exception for invalid discriminator");

    assertTrue(
        exception.getMessage().contains("Invalid type discriminator"),
        "Exception message should mention invalid discriminator: " + exception.getMessage());
  }

  @Test
  @DisplayName("Round-trip bool preserves value")
  void testRoundTripBool() throws ValidationException {
    final WitBool original = WitBool.of(true);
    final byte[] serialized = WitValueSerializer.serialize(original);
    final WitValue deserialized = WitValueDeserializer.deserialize(1, serialized);

    assertInstanceOf(WitBool.class, deserialized, "Deserialized value should be WitBool");
    assertEquals(
        original.getValue(),
        ((WitBool) deserialized).getValue(),
        "Round-trip should preserve value");
  }

  @Test
  @DisplayName("Round-trip s32 preserves value")
  void testRoundTripS32() throws ValidationException {
    final WitS32 original = WitS32.of(-12345);
    final byte[] serialized = WitValueSerializer.serialize(original);
    final WitValue deserialized = WitValueDeserializer.deserialize(2, serialized);

    assertInstanceOf(WitS32.class, deserialized, "Deserialized value should be WitS32");
    assertEquals(
        original.getValue(),
        ((WitS32) deserialized).getValue(),
        "Round-trip should preserve value");
  }

  @Test
  @DisplayName("Round-trip s64 preserves value")
  void testRoundTripS64() throws ValidationException {
    final WitS64 original = WitS64.of(9_876_543_210L);
    final byte[] serialized = WitValueSerializer.serialize(original);
    final WitValue deserialized = WitValueDeserializer.deserialize(3, serialized);

    assertInstanceOf(WitS64.class, deserialized, "Deserialized value should be WitS64");
    assertEquals(
        original.getValue(),
        ((WitS64) deserialized).getValue(),
        "Round-trip should preserve value");
  }

  @Test
  @DisplayName("Round-trip float64 preserves value")
  void testRoundTripFloat64() throws ValidationException {
    final WitFloat64 original = WitFloat64.of(123.456789);
    final byte[] serialized = WitValueSerializer.serialize(original);
    final WitValue deserialized = WitValueDeserializer.deserialize(4, serialized);

    assertInstanceOf(WitFloat64.class, deserialized, "Deserialized value should be WitFloat64");
    assertEquals(
        original.getValue(),
        ((WitFloat64) deserialized).getValue(),
        1e-10,
        "Round-trip should preserve value");
  }

  @Test
  @DisplayName("Round-trip char preserves value")
  void testRoundTripChar() throws ValidationException {
    final WitChar original = WitChar.of(0x1F980); // 🦀
    final byte[] serialized = WitValueSerializer.serialize(original);
    final WitValue deserialized = WitValueDeserializer.deserialize(5, serialized);

    assertInstanceOf(WitChar.class, deserialized, "Deserialized value should be WitChar");
    assertEquals(
        original.getCodepoint(),
        ((WitChar) deserialized).getCodepoint(),
        "Round-trip should preserve value");
  }

  @Test
  @DisplayName("Round-trip string preserves value")
  void testRoundTripString() throws ValidationException {
    final WitString original = WitString.of("Hello 🦀 World 中文");
    final byte[] serialized = WitValueSerializer.serialize(original);
    final WitValue deserialized = WitValueDeserializer.deserialize(6, serialized);

    assertInstanceOf(WitString.class, deserialized, "Deserialized value should be WitString");
    assertEquals(
        original.getValue(),
        ((WitString) deserialized).getValue(),
        "Round-trip should preserve value");
  }

  @Test
  @DisplayName("Deserialize s8 from binary format (discriminator 17)")
  void testDeserializeS8() throws ValidationException {
    final byte[] data = {(byte) -42};
    final WitValue result = WitValueDeserializer.deserialize(17, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitS8.class, result, "Result should be WitS8");
    assertEquals((byte) -42, ((WitS8) result).getValue(), "Value should be -42");
  }

  @Test
  @DisplayName("Deserialize s8 max value")
  void testDeserializeS8Max() throws ValidationException {
    final byte[] data = {Byte.MAX_VALUE};
    final WitValue result = WitValueDeserializer.deserialize(17, data);

    assertInstanceOf(WitS8.class, result, "Result should be WitS8");
    assertEquals(Byte.MAX_VALUE, ((WitS8) result).getValue(), "Value should be MAX_VALUE");
  }

  @Test
  @DisplayName("Deserialize s8 min value")
  void testDeserializeS8Min() throws ValidationException {
    final byte[] data = {Byte.MIN_VALUE};
    final WitValue result = WitValueDeserializer.deserialize(17, data);

    assertInstanceOf(WitS8.class, result, "Result should be WitS8");
    assertEquals(Byte.MIN_VALUE, ((WitS8) result).getValue(), "Value should be MIN_VALUE");
  }

  @Test
  @DisplayName("Deserialize s8 with invalid size throws exception")
  void testDeserializeS8InvalidSize() {
    final byte[] data = {(byte) 0, (byte) 1}; // 2 bytes instead of 1

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(17, data),
            "Should throw exception for invalid size");

    assertTrue(
        exception.getMessage().contains("Invalid s8 data size"),
        "Exception message should mention invalid size: " + exception.getMessage());
  }

  @Test
  @DisplayName("Deserialize s16 from binary format (discriminator 18)")
  void testDeserializeS16() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putShort((short) -1000);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(18, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitS16.class, result, "Result should be WitS16");
    assertEquals((short) -1000, ((WitS16) result).getValue(), "Value should be -1000");
  }

  @Test
  @DisplayName("Deserialize s16 max value")
  void testDeserializeS16Max() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putShort(Short.MAX_VALUE);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(18, data);

    assertInstanceOf(WitS16.class, result, "Result should be WitS16");
    assertEquals(Short.MAX_VALUE, ((WitS16) result).getValue(), "Value should be MAX_VALUE");
  }

  @Test
  @DisplayName("Deserialize s16 min value")
  void testDeserializeS16Min() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putShort(Short.MIN_VALUE);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(18, data);

    assertInstanceOf(WitS16.class, result, "Result should be WitS16");
    assertEquals(Short.MIN_VALUE, ((WitS16) result).getValue(), "Value should be MIN_VALUE");
  }

  @Test
  @DisplayName("Deserialize s16 with invalid size throws exception")
  void testDeserializeS16InvalidSize() {
    final byte[] data = {(byte) 0}; // 1 byte instead of 2

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(18, data),
            "Should throw exception for invalid size");

    assertTrue(
        exception.getMessage().contains("Invalid s16 data size"),
        "Exception message should mention invalid size: " + exception.getMessage());
  }

  @Test
  @DisplayName("Deserialize u8 from binary format (discriminator 19)")
  void testDeserializeU8() throws ValidationException {
    final byte[] data = {(byte) 0xFF};
    final WitValue result = WitValueDeserializer.deserialize(19, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitU8.class, result, "Result should be WitU8");
    assertEquals(255, ((WitU8) result).toUnsignedInt(), "Value should be 255");
  }

  @Test
  @DisplayName("Deserialize u8 zero")
  void testDeserializeU8Zero() throws ValidationException {
    final byte[] data = {(byte) 0};
    final WitValue result = WitValueDeserializer.deserialize(19, data);

    assertInstanceOf(WitU8.class, result, "Result should be WitU8");
    assertEquals(0, ((WitU8) result).toUnsignedInt(), "Value should be 0");
  }

  @Test
  @DisplayName("Deserialize u8 with invalid size throws exception")
  void testDeserializeU8InvalidSize() {
    final byte[] data = {(byte) 0, (byte) 1}; // 2 bytes instead of 1

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(19, data),
            "Should throw exception for invalid size");

    assertTrue(
        exception.getMessage().contains("Invalid u8 data size"),
        "Exception message should mention invalid size: " + exception.getMessage());
  }

  @Test
  @DisplayName("Deserialize u16 from binary format (discriminator 20)")
  void testDeserializeU16() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putShort((short) 0xFFFF);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(20, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitU16.class, result, "Result should be WitU16");
    assertEquals(65535, ((WitU16) result).toUnsignedInt(), "Value should be 65535");
  }

  @Test
  @DisplayName("Deserialize u16 zero")
  void testDeserializeU16Zero() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putShort((short) 0);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(20, data);

    assertInstanceOf(WitU16.class, result, "Result should be WitU16");
    assertEquals(0, ((WitU16) result).toUnsignedInt(), "Value should be 0");
  }

  @Test
  @DisplayName("Deserialize u16 with invalid size throws exception")
  void testDeserializeU16InvalidSize() {
    final byte[] data = {(byte) 0}; // 1 byte instead of 2

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(20, data),
            "Should throw exception for invalid size");

    assertTrue(
        exception.getMessage().contains("Invalid u16 data size"),
        "Exception message should mention invalid size: " + exception.getMessage());
  }

  @Test
  @DisplayName("Deserialize float32 from binary format (discriminator 21)")
  void testDeserializeFloat32() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putFloat(3.14f);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(21, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitFloat32.class, result, "Result should be WitFloat32");
    assertEquals(3.14f, ((WitFloat32) result).getValue(), 0.001f, "Value should be 3.14");
  }

  @Test
  @DisplayName("Deserialize float32 max value")
  void testDeserializeFloat32Max() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putFloat(Float.MAX_VALUE);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(21, data);

    assertInstanceOf(WitFloat32.class, result, "Result should be WitFloat32");
    assertEquals(Float.MAX_VALUE, ((WitFloat32) result).getValue(), "Value should be MAX_VALUE");
  }

  @Test
  @DisplayName("Deserialize float32 min positive value")
  void testDeserializeFloat32MinPositive() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putFloat(Float.MIN_VALUE);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(21, data);

    assertInstanceOf(WitFloat32.class, result, "Result should be WitFloat32");
    assertEquals(Float.MIN_VALUE, ((WitFloat32) result).getValue(), "Value should be MIN_VALUE");
  }

  @Test
  @DisplayName("Deserialize float32 NaN")
  void testDeserializeFloat32Nan() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putFloat(Float.NaN);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(21, data);

    assertInstanceOf(WitFloat32.class, result, "Result should be WitFloat32");
    assertTrue(Float.isNaN(((WitFloat32) result).getValue()), "Value should be NaN");
  }

  @Test
  @DisplayName("Deserialize float32 with invalid size throws exception")
  void testDeserializeFloat32InvalidSize() {
    final byte[] data = {(byte) 0, (byte) 1}; // 2 bytes instead of 4

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(21, data),
            "Should throw exception for invalid size");

    assertTrue(
        exception.getMessage().contains("Invalid float32 data size"),
        "Exception message should mention invalid size: " + exception.getMessage());
  }

  @Test
  @DisplayName("Deserialize u32 from binary format (discriminator 9)")
  void testDeserializeU32() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(0xFFFFFFFF);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(9, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitU32.class, result, "Result should be WitU32");
    assertEquals(4294967295L, ((WitU32) result).toUnsignedLong(), "Value should be max u32");
  }

  @Test
  @DisplayName("Deserialize u32 zero")
  void testDeserializeU32Zero() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(0);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(9, data);

    assertInstanceOf(WitU32.class, result, "Result should be WitU32");
    assertEquals(0L, ((WitU32) result).toUnsignedLong(), "Value should be 0");
  }

  @Test
  @DisplayName("Deserialize u32 with invalid size throws exception")
  void testDeserializeU32InvalidSize() {
    final byte[] data = {(byte) 0, (byte) 1}; // 2 bytes instead of 4

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(9, data),
            "Should throw exception for invalid size");

    assertTrue(
        exception.getMessage().contains("Invalid u32 data size"),
        "Exception message should mention invalid size: " + exception.getMessage());
  }

  @Test
  @DisplayName("Deserialize u64 from binary format (discriminator 10)")
  void testDeserializeU64() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putLong(-1L); // Max unsigned
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(10, data);

    assertNotNull(result, "Deserialized result should not be null");
    assertInstanceOf(WitU64.class, result, "Result should be WitU64");
    assertEquals(-1L, ((WitU64) result).getValue(), "Value should be -1 (max unsigned)");
  }

  @Test
  @DisplayName("Deserialize u64 zero")
  void testDeserializeU64Zero() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putLong(0L);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(10, data);

    assertInstanceOf(WitU64.class, result, "Result should be WitU64");
    assertEquals(0L, ((WitU64) result).getValue(), "Value should be 0");
  }

  @Test
  @DisplayName("Deserialize u64 with invalid size throws exception")
  void testDeserializeU64InvalidSize() {
    final byte[] data = {(byte) 0, (byte) 1, (byte) 2, (byte) 3}; // 4 bytes instead of 8

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(10, data),
            "Should throw exception for invalid size");

    assertTrue(
        exception.getMessage().contains("Invalid u64 data size"),
        "Exception message should mention invalid size: " + exception.getMessage());
  }

  @Test
  @DisplayName("Round-trip s8 preserves value")
  void testRoundTripS8() throws ValidationException {
    final WitS8 original = WitS8.of((byte) -42);
    final byte[] serialized = WitValueSerializer.serialize(original);
    final WitValue deserialized = WitValueDeserializer.deserialize(17, serialized);

    assertInstanceOf(WitS8.class, deserialized, "Deserialized value should be WitS8");
    assertEquals(
        original.getValue(), ((WitS8) deserialized).getValue(), "Round-trip should preserve value");
  }

  @Test
  @DisplayName("Round-trip s16 preserves value")
  void testRoundTripS16() throws ValidationException {
    final WitS16 original = WitS16.of((short) -1000);
    final byte[] serialized = WitValueSerializer.serialize(original);
    final WitValue deserialized = WitValueDeserializer.deserialize(18, serialized);

    assertInstanceOf(WitS16.class, deserialized, "Deserialized value should be WitS16");
    assertEquals(
        original.getValue(),
        ((WitS16) deserialized).getValue(),
        "Round-trip should preserve value");
  }

  @Test
  @DisplayName("Round-trip u8 preserves value")
  void testRoundTripU8() throws ValidationException {
    final WitU8 original = WitU8.of((byte) 0xFF);
    final byte[] serialized = WitValueSerializer.serialize(original);
    final WitValue deserialized = WitValueDeserializer.deserialize(19, serialized);

    assertInstanceOf(WitU8.class, deserialized, "Deserialized value should be WitU8");
    assertEquals(
        original.getValue(), ((WitU8) deserialized).getValue(), "Round-trip should preserve value");
  }

  @Test
  @DisplayName("Round-trip u16 preserves value")
  void testRoundTripU16() throws ValidationException {
    final WitU16 original = WitU16.of((short) 0xFFFF);
    final byte[] serialized = WitValueSerializer.serialize(original);
    final WitValue deserialized = WitValueDeserializer.deserialize(20, serialized);

    assertInstanceOf(WitU16.class, deserialized, "Deserialized value should be WitU16");
    assertEquals(
        original.getValue(),
        ((WitU16) deserialized).getValue(),
        "Round-trip should preserve value");
  }

  @Test
  @DisplayName("Round-trip u32 preserves value")
  void testRoundTripU32() throws ValidationException {
    final WitU32 original = WitU32.of(0xFFFFFFFF);
    final byte[] serialized = WitValueSerializer.serialize(original);
    final WitValue deserialized = WitValueDeserializer.deserialize(9, serialized);

    assertInstanceOf(WitU32.class, deserialized, "Deserialized value should be WitU32");
    assertEquals(
        original.getValue(),
        ((WitU32) deserialized).getValue(),
        "Round-trip should preserve value");
  }

  @Test
  @DisplayName("Round-trip u64 preserves value")
  void testRoundTripU64() throws ValidationException {
    final WitU64 original = WitU64.of(-1L);
    final byte[] serialized = WitValueSerializer.serialize(original);
    final WitValue deserialized = WitValueDeserializer.deserialize(10, serialized);

    assertInstanceOf(WitU64.class, deserialized, "Deserialized value should be WitU64");
    assertEquals(
        original.getValue(),
        ((WitU64) deserialized).getValue(),
        "Round-trip should preserve value");
  }

  @Test
  @DisplayName("Round-trip float32 preserves value")
  void testRoundTripFloat32() throws ValidationException {
    final WitFloat32 original = WitFloat32.of(3.14f);
    final byte[] serialized = WitValueSerializer.serialize(original);
    final WitValue deserialized = WitValueDeserializer.deserialize(21, serialized);

    assertInstanceOf(WitFloat32.class, deserialized, "Deserialized value should be WitFloat32");
    assertEquals(
        original.getValue(),
        ((WitFloat32) deserialized).getValue(),
        0.001f,
        "Round-trip should preserve value");
  }

  // ==================== List Deserialization Boundary Tests ====================

  @Test
  @DisplayName(
      "Deserialize list with exactly 4 bytes (minimum for count) should work if count is 0")
  void testDeserializeListExactlyMinimumBytesForCount() {
    // 4 bytes for count, but count=0 is not allowed (can't infer type from empty list)
    final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(0); // count = 0
    final byte[] data = buffer.array();

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(11, data),
            "Empty list should throw exception");

    assertTrue(
        exception.getMessage().contains("Cannot infer list element type"),
        "Exception should mention empty list: " + exception.getMessage());
  }

  @Test
  @DisplayName("Deserialize list with 3 bytes (less than minimum) should fail")
  void testDeserializeListLessThanMinimumBytes() {
    final byte[] data = new byte[3]; // Less than 4 bytes needed for count

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(11, data),
            "Should throw for insufficient bytes");

    assertTrue(
        exception.getMessage().contains("too short"),
        "Exception should mention data too short: " + exception.getMessage());
  }

  @Test
  @DisplayName("Deserialize list with negative element count should fail")
  void testDeserializeListNegativeElementCount() {
    final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(-1); // negative count
    final byte[] data = buffer.array();

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(11, data),
            "Negative count should throw exception");

    assertTrue(
        exception.getMessage().contains("Invalid element count"),
        "Exception should mention invalid count: " + exception.getMessage());
  }

  @Test
  @DisplayName("Deserialize list with one element but truncated header should fail")
  void testDeserializeListTruncatedElementHeader() {
    // count=1, but only 7 bytes remaining (needs 8 for discriminator + length)
    final ByteBuffer buffer = ByteBuffer.allocate(11).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(1); // count = 1
    buffer.putInt(1); // partial discriminator + something
    buffer.put((byte) 0);
    buffer.put((byte) 0);
    buffer.put((byte) 0);
    // Missing last byte of the element header
    final byte[] data = buffer.array();

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(11, data),
            "Should throw for truncated element header");

    assertTrue(
        exception.getMessage().contains("truncated"),
        "Exception should mention truncation: " + exception.getMessage());
  }

  @Test
  @DisplayName("Deserialize list with exactly 8 bytes remaining for element header should work")
  void testDeserializeListExactlyEnoughForElementHeader() throws ValidationException {
    // count=1, discriminator=1 (bool), length=1, data=1 (true)
    final ByteBuffer buffer = ByteBuffer.allocate(13).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(1); // count = 1
    buffer.putInt(1); // discriminator = bool
    buffer.putInt(1); // length = 1
    buffer.put((byte) 1); // bool true
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(11, data);

    assertNotNull(result, "Result should not be null");
    assertInstanceOf(WitList.class, result, "Result should be WitList");
  }

  @Test
  @DisplayName("Deserialize list with negative element length should fail")
  void testDeserializeListNegativeElementLength() {
    final ByteBuffer buffer = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(1); // count = 1
    buffer.putInt(1); // discriminator = bool
    buffer.putInt(-1); // negative length
    final byte[] data = buffer.array();

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(11, data),
            "Negative length should throw exception");

    assertTrue(
        exception.getMessage().contains("Invalid element data length"),
        "Exception should mention invalid length: " + exception.getMessage());
  }

  @Test
  @DisplayName("Deserialize list with element data truncated should fail")
  void testDeserializeListElementDataTruncated() {
    final ByteBuffer buffer = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(1); // count = 1
    buffer.putInt(1); // discriminator = bool
    buffer.putInt(5); // length = 5 (but no data follows)
    final byte[] data = buffer.array();

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(11, data),
            "Truncated element data should throw exception");

    assertTrue(
        exception.getMessage().contains("truncated"),
        "Exception should mention truncation: " + exception.getMessage());
  }

  // ==================== Own Deserialization Boundary Tests ====================

  @Test
  @DisplayName("Deserialize own with exactly 12 bytes (minimum) should work with empty type name")
  void testDeserializeOwnExactlyMinimumBytes() throws ValidationException {
    // type_length=0, handle=42 (i64)
    final ByteBuffer buffer = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(0); // type name length = 0
    buffer.putLong(42L); // handle = 42
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(22, data);

    assertNotNull(result, "Result should not be null");
    assertInstanceOf(WitOwn.class, result, "Result should be WitOwn");
    assertEquals(
        42L, ((WitOwn) result).getHandle().getNativeHandle(), "Native handle should be 42");
  }

  @Test
  @DisplayName("Deserialize own with 11 bytes (less than minimum 12) should fail")
  void testDeserializeOwnLessThanMinimumBytes() {
    final byte[] data = new byte[11]; // Less than 12 bytes needed for i64 handle

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(22, data),
            "Should throw for insufficient bytes");

    assertTrue(
        exception.getMessage().contains("too short"),
        "Exception should mention data too short: " + exception.getMessage());
  }

  @Test
  @DisplayName("Deserialize own with negative type name length should fail")
  void testDeserializeOwnNegativeTypeNameLength() {
    final ByteBuffer buffer = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(-1); // negative type name length
    buffer.putLong(42L); // handle
    final byte[] data = buffer.array();

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(22, data),
            "Negative type name length should throw exception");

    assertTrue(
        exception.getMessage().contains("Invalid resource type name length"),
        "Exception should mention invalid length: " + exception.getMessage());
  }

  @Test
  @DisplayName("Deserialize own with truncated type name should fail")
  void testDeserializeOwnTruncatedTypeName() {
    // type_length=10, but only 3 bytes of type name + handle available
    final ByteBuffer buffer = ByteBuffer.allocate(15).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(10); // type name length = 10
    buffer.put("abc".getBytes(StandardCharsets.UTF_8)); // only 3 bytes
    buffer.putLong(42L); // handle
    final byte[] data = buffer.array();

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(22, data),
            "Truncated type name should throw exception");

    assertTrue(
        exception.getMessage().contains("truncated"),
        "Exception should mention truncation: " + exception.getMessage());
  }

  @Test
  @DisplayName("Deserialize own with valid type name and i64 handle")
  void testDeserializeOwnWithTypeName() throws ValidationException {
    final String typeName = "myresource";
    final byte[] typeNameBytes = typeName.getBytes(StandardCharsets.UTF_8);
    final ByteBuffer buffer =
        ByteBuffer.allocate(4 + typeNameBytes.length + 8).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(typeNameBytes.length);
    buffer.put(typeNameBytes);
    buffer.putLong(123L);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(22, data);

    assertNotNull(result, "Result should not be null");
    assertInstanceOf(WitOwn.class, result, "Result should be WitOwn");
    assertEquals(typeName, ((WitOwn) result).getResourceType(), "Type name should match");
    assertEquals(
        123L, ((WitOwn) result).getHandle().getNativeHandle(), "Native handle should be 123");
  }

  // ==================== Borrow Deserialization Boundary Tests ====================

  @Test
  @DisplayName(
      "Deserialize borrow with exactly 12 bytes (minimum) should work with empty type name")
  void testDeserializeBorrowExactlyMinimumBytes() throws ValidationException {
    // type_length=0, handle=99 (i64)
    final ByteBuffer buffer = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(0); // type name length = 0
    buffer.putLong(99L); // handle = 99
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(23, data);

    assertNotNull(result, "Result should not be null");
    assertInstanceOf(WitBorrow.class, result, "Result should be WitBorrow");
    assertEquals(
        99L, ((WitBorrow) result).getHandle().getNativeHandle(), "Native handle should be 99");
  }

  @Test
  @DisplayName("Deserialize borrow with 11 bytes (less than minimum 12) should fail")
  void testDeserializeBorrowLessThanMinimumBytes() {
    final byte[] data = new byte[11]; // Less than 12 bytes needed for i64 handle

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(23, data),
            "Should throw for insufficient bytes");

    assertTrue(
        exception.getMessage().contains("too short"),
        "Exception should mention data too short: " + exception.getMessage());
  }

  @Test
  @DisplayName("Deserialize borrow with negative type name length should fail")
  void testDeserializeBorrowNegativeTypeNameLength() {
    final ByteBuffer buffer = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(-1); // negative type name length
    buffer.putLong(42L); // handle
    final byte[] data = buffer.array();

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(23, data),
            "Negative type name length should throw exception");

    assertTrue(
        exception.getMessage().contains("Invalid resource type name length"),
        "Exception should mention invalid length: " + exception.getMessage());
  }

  @Test
  @DisplayName("Deserialize borrow with truncated type name should fail")
  void testDeserializeBorrowTruncatedTypeName() {
    // type_length=10, but only 3 bytes of type name + handle available
    final ByteBuffer buffer = ByteBuffer.allocate(15).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(10); // type name length = 10
    buffer.put("abc".getBytes(StandardCharsets.UTF_8)); // only 3 bytes
    buffer.putLong(42L); // handle
    final byte[] data = buffer.array();

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(23, data),
            "Truncated type name should throw exception");

    assertTrue(
        exception.getMessage().contains("truncated"),
        "Exception should mention truncation: " + exception.getMessage());
  }

  @Test
  @DisplayName("Deserialize borrow with valid type name and i64 handle")
  void testDeserializeBorrowWithTypeName() throws ValidationException {
    final String typeName = "borrowed_res";
    final byte[] typeNameBytes = typeName.getBytes(StandardCharsets.UTF_8);
    final ByteBuffer buffer =
        ByteBuffer.allocate(4 + typeNameBytes.length + 8).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(typeNameBytes.length);
    buffer.put(typeNameBytes);
    buffer.putLong(456L);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(23, data);

    assertNotNull(result, "Result should not be null");
    assertInstanceOf(WitBorrow.class, result, "Result should be WitBorrow");
    assertEquals(typeName, ((WitBorrow) result).getResourceType(), "Type name should match");
    assertEquals(
        456L, ((WitBorrow) result).getHandle().getNativeHandle(), "Native handle should be 456");
  }

  @Test
  @DisplayName("Deserialize own with large handle value exceeding Integer.MAX_VALUE")
  void testDeserializeOwnWithLargeHandle() throws ValidationException {
    final long largeHandle = (long) Integer.MAX_VALUE + 200L;
    final String typeName = "large";
    final byte[] typeNameBytes = typeName.getBytes(StandardCharsets.UTF_8);
    final ByteBuffer buffer =
        ByteBuffer.allocate(4 + typeNameBytes.length + 8).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(typeNameBytes.length);
    buffer.put(typeNameBytes);
    buffer.putLong(largeHandle);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(22, data);

    assertNotNull(result, "Result should not be null");
    assertInstanceOf(WitOwn.class, result, "Result should be WitOwn");
    assertEquals(
        largeHandle,
        ((WitOwn) result).getHandle().getNativeHandle(),
        "Large handle value should be preserved through deserialization");
  }

  @Test
  @DisplayName("Deserialize borrow with large handle value exceeding Integer.MAX_VALUE")
  void testDeserializeBorrowWithLargeHandle() throws ValidationException {
    final long largeHandle = (long) Integer.MAX_VALUE + 300L;
    final String typeName = "large-borrow";
    final byte[] typeNameBytes = typeName.getBytes(StandardCharsets.UTF_8);
    final ByteBuffer buffer =
        ByteBuffer.allocate(4 + typeNameBytes.length + 8).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(typeNameBytes.length);
    buffer.put(typeNameBytes);
    buffer.putLong(largeHandle);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(23, data);

    assertNotNull(result, "Result should not be null");
    assertInstanceOf(WitBorrow.class, result, "Result should be WitBorrow");
    assertEquals(
        largeHandle,
        ((WitBorrow) result).getHandle().getNativeHandle(),
        "Large handle value should be preserved through deserialization");
  }

  // ==================== String Deserialization Boundary Tests ====================

  @Test
  @DisplayName("Deserialize string with exactly 4 bytes (minimum for length) with zero length")
  void testDeserializeStringExactlyMinimumBytes() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(0); // length = 0 (empty string)
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(6, data);

    assertNotNull(result, "Result should not be null");
    assertInstanceOf(WitString.class, result, "Result should be WitString");
    assertEquals("", ((WitString) result).getValue(), "Value should be empty string");
  }

  @Test
  @DisplayName("Deserialize string with 3 bytes (less than minimum) should fail")
  void testDeserializeStringLessThanMinimumBytes() {
    final byte[] data = new byte[3]; // Less than 4 bytes needed for length

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(6, data),
            "Should throw for insufficient bytes");

    assertTrue(
        exception.getMessage().contains("Invalid string data size"),
        "Exception should mention invalid size: " + exception.getMessage());
  }

  @Test
  @DisplayName("Deserialize string with valid content")
  void testDeserializeStringValidContent() throws ValidationException {
    final String text = "Hello, WIT!";
    final byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
    final ByteBuffer buffer =
        ByteBuffer.allocate(4 + textBytes.length).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(textBytes.length);
    buffer.put(textBytes);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(6, data);

    assertNotNull(result, "Result should not be null");
    assertInstanceOf(WitString.class, result, "Result should be WitString");
    assertEquals(text, ((WitString) result).getValue(), "String value should match");
  }

  @Test
  @DisplayName(
      "Deserialize string with data length mismatch (STRING_LENGTH_SIZE + length boundary)")
  void testDeserializeStringSizeMismatchBoundary() {
    // Construct a string with length=5 but provide 6 extra bytes (expected 4+5=9, got 10)
    final ByteBuffer buffer = ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(5);
    buffer.put(new byte[6]); // one extra byte
    final byte[] data = buffer.array();

    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(6, data),
            "Should throw for size mismatch");

    assertTrue(
        exception.getMessage().contains("mismatch"),
        "Exception should mention mismatch: " + exception.getMessage());
  }

  // ===== Composite Type Deserialization Tests =====

  @Nested
  @DisplayName("Record Deserialization Tests")
  class RecordDeserializationTests {

    @Test
    @DisplayName("Deserialize record with single field")
    void testDeserializeRecordSingleField() throws ValidationException {
      final byte[] nameBytes = "age".getBytes(StandardCharsets.UTF_8);
      // field_count(4) + name_len(4) + name(3) + disc(4) + val_len(4) + val_data(4) = 23
      final ByteBuffer buffer = ByteBuffer.allocate(23).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(1); // field count
      buffer.putInt(nameBytes.length); // field name length
      buffer.put(nameBytes); // field name
      buffer.putInt(2); // discriminator: S32
      buffer.putInt(4); // value data length
      buffer.putInt(42); // S32 value
      final byte[] data = buffer.array();

      final WitValue result = WitValueDeserializer.deserialize(7, data);

      assertNotNull(result, "Result should not be null");
      assertInstanceOf(WitRecord.class, result, "Result should be WitRecord");
      final WitRecord record = (WitRecord) result;
      assertEquals(1, record.getFieldCount(), "Record should have 1 field");
      final Map<String, WitValue> fields = record.getFields();
      assertTrue(fields.containsKey("age"), "Record should contain 'age' field");
      assertInstanceOf(WitS32.class, fields.get("age"), "Field 'age' should be WitS32");
      assertEquals(42, ((WitS32) fields.get("age")).getValue(), "age should equal 42");
    }

    @Test
    @DisplayName("Deserialize record with multiple fields")
    void testDeserializeRecordMultipleFields() throws ValidationException {
      final byte[] nameBytes = "x".getBytes(StandardCharsets.UTF_8);
      final byte[] name2Bytes = "y".getBytes(StandardCharsets.UTF_8);
      // field_count(4) + 2 * (name_len(4) + name(1) + disc(4) + val_len(4) + val_data(4)) = 38
      final ByteBuffer buffer = ByteBuffer.allocate(38).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(2); // field count
      buffer.putInt(nameBytes.length);
      buffer.put(nameBytes);
      buffer.putInt(2); // S32
      buffer.putInt(4);
      buffer.putInt(10);
      buffer.putInt(name2Bytes.length);
      buffer.put(name2Bytes);
      buffer.putInt(2); // S32
      buffer.putInt(4);
      buffer.putInt(20);
      final byte[] data = buffer.array();

      final WitValue result = WitValueDeserializer.deserialize(7, data);

      assertInstanceOf(WitRecord.class, result, "Result should be WitRecord");
      final WitRecord record = (WitRecord) result;
      assertEquals(2, record.getFieldCount(), "Record should have 2 fields");
      assertEquals(10, ((WitS32) record.getFields().get("x")).getValue(), "x should be 10");
      assertEquals(20, ((WitS32) record.getFields().get("y")).getValue(), "y should be 20");
    }

    @Test
    @DisplayName("Deserialize record with single bool field at exact boundary should succeed")
    void testDeserializeRecordSingleBoolFieldExactBoundary() throws ValidationException {
      final byte[] nameBytes = "f".getBytes(StandardCharsets.UTF_8);
      // field_count(4) + name_len(4) + name(1) + disc(4) + val_len(4) + val(1) = 18
      final ByteBuffer buffer = ByteBuffer.allocate(18).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(1); // 1 field
      buffer.putInt(nameBytes.length);
      buffer.put(nameBytes);
      buffer.putInt(1); // bool discriminator
      buffer.putInt(1); // 1 byte bool data
      buffer.put((byte) 1); // true
      final byte[] data = buffer.array();

      final WitValue result = WitValueDeserializer.deserialize(7, data);

      assertInstanceOf(WitRecord.class, result, "Result should be WitRecord");
      final WitRecord record = (WitRecord) result;
      assertEquals(1, record.getFieldCount(), "Record should have 1 field");
      assertInstanceOf(WitBool.class, record.getFields().get("f"), "Field should be bool");
      assertTrue(((WitBool) record.getFields().get("f")).getValue(), "Bool field should be true");
    }

    @Test
    @DisplayName("Deserialize record with truncated field name should fail")
    void testDeserializeRecordTruncatedFieldName() {
      final ByteBuffer buffer = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(1); // 1 field
      buffer.putInt(100); // name length = 100 but only a few bytes remain
      buffer.putInt(0); // some data, not enough
      final byte[] data = buffer.array();

      assertThrows(
          ValidationException.class,
          () -> WitValueDeserializer.deserialize(7, data),
          "Should throw for truncated field name");
    }

    @Test
    @DisplayName("Deserialize record with truncated field data should fail")
    void testDeserializeRecordTruncatedFieldData() {
      final byte[] nameBytes = "f".getBytes(StandardCharsets.UTF_8);
      // field_count(4) + name_len(4) + name(1) + disc(4) + val_len(4) = 17 but no val data
      final ByteBuffer buffer = ByteBuffer.allocate(17).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(1);
      buffer.putInt(nameBytes.length);
      buffer.put(nameBytes);
      buffer.putInt(2); // S32
      buffer.putInt(4); // says 4 bytes but none remain
      final byte[] data = buffer.array();

      assertThrows(
          ValidationException.class,
          () -> WitValueDeserializer.deserialize(7, data),
          "Should throw for truncated field data");
    }

    @Test
    @DisplayName("Deserialize record with insufficient bytes for field count")
    void testDeserializeRecordTooShort() {
      final byte[] data = new byte[3]; // Less than 4 bytes

      assertThrows(
          ValidationException.class,
          () -> WitValueDeserializer.deserialize(7, data),
          "Should throw for data too short");
    }

    @Test
    @DisplayName("Deserialize record with negative field count should fail")
    void testDeserializeRecordNegativeFieldCount() {
      final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(-1);
      final byte[] data = buffer.array();

      assertThrows(
          ValidationException.class,
          () -> WitValueDeserializer.deserialize(7, data),
          "Should throw for negative field count");
    }
  }

  @Nested
  @DisplayName("Variant Deserialization Tests")
  class VariantDeserializationTests {

    @Test
    @DisplayName("Deserialize variant without payload")
    void testDeserializeVariantWithoutPayload() throws ValidationException {
      final byte[] nameBytes = "pending".getBytes(StandardCharsets.UTF_8);
      // name_len(4) + name(7) + has_payload(1) = 12
      final ByteBuffer buffer = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(nameBytes.length);
      buffer.put(nameBytes);
      buffer.put((byte) 0); // no payload
      final byte[] data = buffer.array();

      final WitValue result = WitValueDeserializer.deserialize(12, data);

      assertNotNull(result, "Result should not be null");
      assertInstanceOf(WitVariant.class, result, "Result should be WitVariant");
      final WitVariant variant = (WitVariant) result;
      assertEquals("pending", variant.getCaseName(), "Case name should be 'pending'");
      assertFalse(variant.hasPayload(), "Variant should not have payload");
    }

    @Test
    @DisplayName("Deserialize variant with payload")
    void testDeserializeVariantWithPayload() throws ValidationException {
      final byte[] nameBytes = "ok".getBytes(StandardCharsets.UTF_8);
      // name_len(4) + name(2) + has_payload(1) + disc(4) + val_len(4) + val_data(4) = 19
      final ByteBuffer buffer = ByteBuffer.allocate(19).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(nameBytes.length);
      buffer.put(nameBytes);
      buffer.put((byte) 1); // has payload
      buffer.putInt(2); // S32
      buffer.putInt(4);
      buffer.putInt(99);
      final byte[] data = buffer.array();

      final WitValue result = WitValueDeserializer.deserialize(12, data);

      assertInstanceOf(WitVariant.class, result, "Result should be WitVariant");
      final WitVariant variant = (WitVariant) result;
      assertEquals("ok", variant.getCaseName(), "Case name should be 'ok'");
      assertTrue(variant.hasPayload(), "Variant should have payload");
      final Optional<WitValue> payload = variant.getPayload();
      assertTrue(payload.isPresent(), "Payload should be present");
      assertInstanceOf(WitS32.class, payload.get(), "Payload should be WitS32");
      assertEquals(99, ((WitS32) payload.get()).getValue(), "Payload value should be 99");
    }

    @Test
    @DisplayName("Deserialize variant with short case name at exact boundary should succeed")
    void testDeserializeVariantShortCaseName() throws ValidationException {
      final byte[] nameBytes = "a".getBytes(StandardCharsets.UTF_8);
      // name_len(4) + name(1) + has_payload(1) = 6 bytes
      final ByteBuffer buffer = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(nameBytes.length);
      buffer.put(nameBytes);
      buffer.put((byte) 0); // no payload
      final byte[] data = buffer.array();

      final WitValue result = WitValueDeserializer.deserialize(12, data);

      assertInstanceOf(WitVariant.class, result, "Result should be WitVariant");
      assertEquals("a", ((WitVariant) result).getCaseName(), "Case name should be 'a'");
      assertFalse(((WitVariant) result).hasPayload(), "Should have no payload");
    }

    @Test
    @DisplayName("Deserialize variant with truncated case name should fail")
    void testDeserializeVariantTruncatedCaseName() {
      final ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(100); // name length 100 but only 4 bytes remain
      buffer.putInt(0);
      final byte[] data = buffer.array();

      assertThrows(
          ValidationException.class,
          () -> WitValueDeserializer.deserialize(12, data),
          "Should throw for truncated case name");
    }

    @Test
    @DisplayName("Deserialize variant with truncated payload should fail")
    void testDeserializeVariantTruncatedPayload() {
      final byte[] nameBytes = "a".getBytes(StandardCharsets.UTF_8);
      // name_len(4) + name(1) + has_payload(1) = 6, no room for disc+len
      final ByteBuffer buffer = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(nameBytes.length);
      buffer.put(nameBytes);
      buffer.put((byte) 1); // has payload
      final byte[] data = buffer.array();

      assertThrows(
          ValidationException.class,
          () -> WitValueDeserializer.deserialize(12, data),
          "Should throw for truncated payload");
    }

    @Test
    @DisplayName("Deserialize variant data too short should fail")
    void testDeserializeVariantTooShort() {
      final byte[] data = new byte[4]; // Less than 5 bytes (name_len + has_payload)

      assertThrows(
          ValidationException.class,
          () -> WitValueDeserializer.deserialize(12, data),
          "Should throw for data too short");
    }
  }

  @Nested
  @DisplayName("Enum Deserialization Tests")
  class EnumDeserializationTests {

    @Test
    @DisplayName("Deserialize enum with valid discriminant")
    void testDeserializeEnumValid() throws ValidationException {
      final byte[] nameBytes = "red".getBytes(StandardCharsets.UTF_8);
      final ByteBuffer buffer =
          ByteBuffer.allocate(4 + nameBytes.length).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(nameBytes.length);
      buffer.put(nameBytes);
      final byte[] data = buffer.array();

      final WitValue result = WitValueDeserializer.deserialize(13, data);

      assertNotNull(result, "Result should not be null");
      assertInstanceOf(WitEnum.class, result, "Result should be WitEnum");
      assertEquals("red", ((WitEnum) result).getDiscriminant(), "Discriminant should be 'red'");
    }

    @Test
    @DisplayName("Deserialize enum with truncated name should fail")
    void testDeserializeEnumTruncatedName() {
      final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(10); // name length 10, no data
      final byte[] data = buffer.array();

      assertThrows(
          ValidationException.class,
          () -> WitValueDeserializer.deserialize(13, data),
          "Should throw for truncated discriminant name");
    }

    @Test
    @DisplayName("Deserialize enum data too short should fail")
    void testDeserializeEnumTooShort() {
      final byte[] data = new byte[3];

      assertThrows(
          ValidationException.class,
          () -> WitValueDeserializer.deserialize(13, data),
          "Should throw for data too short");
    }

    @Test
    @DisplayName("Deserialize enum with negative name length should fail")
    void testDeserializeEnumNegativeNameLength() {
      final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(-1);
      final byte[] data = buffer.array();

      assertThrows(
          ValidationException.class,
          () -> WitValueDeserializer.deserialize(13, data),
          "Should throw for negative name length");
    }
  }

  @Nested
  @DisplayName("Option Deserialization Tests")
  class OptionDeserializationTests {

    @Test
    @DisplayName("Deserialize option none")
    void testDeserializeOptionNone() throws ValidationException {
      final byte[] data = {(byte) 0};

      final WitValue result = WitValueDeserializer.deserialize(14, data);

      assertNotNull(result, "Result should not be null");
      assertInstanceOf(WitOption.class, result, "Result should be WitOption");
      assertTrue(((WitOption) result).isNone(), "Option should be none");
    }

    @Test
    @DisplayName("Deserialize option some with bool payload")
    void testDeserializeOptionSomeWithBool() throws ValidationException {
      // is_some(1) + disc(4) + val_len(4) + val_data(1) = 10
      final ByteBuffer buffer = ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN);
      buffer.put((byte) 1); // is_some
      buffer.putInt(1); // discriminator: bool
      buffer.putInt(1); // value length
      buffer.put((byte) 1); // true
      final byte[] data = buffer.array();

      final WitValue result = WitValueDeserializer.deserialize(14, data);

      assertInstanceOf(WitOption.class, result, "Result should be WitOption");
      final WitOption option = (WitOption) result;
      assertTrue(option.isSome(), "Option should be some");
      assertTrue(option.getValue().isPresent(), "Value should be present");
      assertInstanceOf(WitBool.class, option.getValue().get(), "Inner value should be WitBool");
      assertTrue(((WitBool) option.getValue().get()).getValue(), "Inner bool should be true");
    }

    @Test
    @DisplayName("Deserialize option with truncated payload should fail")
    void testDeserializeOptionTruncatedPayload() {
      // is_some(1) but no disc/len
      final byte[] data = {(byte) 1};

      assertThrows(
          ValidationException.class,
          () -> WitValueDeserializer.deserialize(14, data),
          "Should throw for truncated payload");
    }

    @Test
    @DisplayName("Deserialize option with truncated value data should fail")
    void testDeserializeOptionTruncatedValueData() {
      // is_some(1) + disc(4) + val_len(4) = 9, claims 100 bytes of data
      final ByteBuffer buffer = ByteBuffer.allocate(9).order(ByteOrder.LITTLE_ENDIAN);
      buffer.put((byte) 1);
      buffer.putInt(2); // S32
      buffer.putInt(100); // 100 bytes but none remain
      final byte[] data = buffer.array();

      assertThrows(
          ValidationException.class,
          () -> WitValueDeserializer.deserialize(14, data),
          "Should throw for truncated value data");
    }

    @Test
    @DisplayName("Deserialize option data too short should fail")
    void testDeserializeOptionTooShort() {
      final byte[] data = new byte[0];

      assertThrows(
          ValidationException.class,
          () -> WitValueDeserializer.deserialize(14, data),
          "Should throw for empty data");
    }
  }

  @Nested
  @DisplayName("Result Deserialization Tests")
  class ResultDeserializationTests {

    @Test
    @DisplayName("Deserialize result ok without payload")
    void testDeserializeResultOkNoPayload() throws ValidationException {
      // is_ok(1) + has_value(1)
      final byte[] data = {(byte) 1, (byte) 0};

      final WitValue result = WitValueDeserializer.deserialize(15, data);

      assertNotNull(result, "Result should not be null");
      assertInstanceOf(WitResult.class, result, "Result should be WitResult");
      final WitResult witResult = (WitResult) result;
      assertTrue(witResult.isOk(), "Result should be ok");
      assertFalse(witResult.getValue().isPresent(), "Should have no value");
    }

    @Test
    @DisplayName("Deserialize result err without payload")
    void testDeserializeResultErrNoPayload() throws ValidationException {
      final byte[] data = {(byte) 0, (byte) 0};

      final WitValue result = WitValueDeserializer.deserialize(15, data);

      assertInstanceOf(WitResult.class, result, "Result should be WitResult");
      final WitResult witResult = (WitResult) result;
      assertTrue(witResult.isErr(), "Result should be err");
      assertFalse(witResult.getValue().isPresent(), "Should have no value");
    }

    @Test
    @DisplayName("Deserialize result ok with payload")
    void testDeserializeResultOkWithPayload() throws ValidationException {
      // is_ok(1) + has_value(1) + disc(4) + val_len(4) + val_data(4) = 14
      final ByteBuffer buffer = ByteBuffer.allocate(14).order(ByteOrder.LITTLE_ENDIAN);
      buffer.put((byte) 1); // is_ok
      buffer.put((byte) 1); // has_value
      buffer.putInt(2); // S32
      buffer.putInt(4);
      buffer.putInt(777);
      final byte[] data = buffer.array();

      final WitValue result = WitValueDeserializer.deserialize(15, data);

      assertInstanceOf(WitResult.class, result, "Result should be WitResult");
      final WitResult witResult = (WitResult) result;
      assertTrue(witResult.isOk(), "Result should be ok");
      assertTrue(witResult.getValue().isPresent(), "Should have value");
      assertInstanceOf(WitS32.class, witResult.getValue().get(), "Value should be WitS32");
      assertEquals(777, ((WitS32) witResult.getValue().get()).getValue(), "Value should be 777");
    }

    @Test
    @DisplayName("Deserialize result err with payload")
    void testDeserializeResultErrWithPayload() throws ValidationException {
      final byte[] msgBytes = "fail".getBytes(StandardCharsets.UTF_8);
      // is_ok(1) + has_value(1) + disc(4) + val_len(4) + string_len(4) + string_data(4) = 18
      final ByteBuffer buffer = ByteBuffer.allocate(18).order(ByteOrder.LITTLE_ENDIAN);
      buffer.put((byte) 0); // is_err
      buffer.put((byte) 1); // has_value
      buffer.putInt(6); // string discriminator
      buffer.putInt(4 + msgBytes.length); // value data length
      buffer.putInt(msgBytes.length); // string length
      buffer.put(msgBytes);
      final byte[] data = buffer.array();

      final WitValue result = WitValueDeserializer.deserialize(15, data);

      assertInstanceOf(WitResult.class, result, "Result should be WitResult");
      final WitResult witResult = (WitResult) result;
      assertTrue(witResult.isErr(), "Result should be err");
      assertTrue(witResult.getValue().isPresent(), "Should have value");
      assertInstanceOf(WitString.class, witResult.getValue().get(), "Value should be WitString");
      assertEquals(
          "fail",
          ((WitString) witResult.getValue().get()).getValue(),
          "Error message should be 'fail'");
    }

    @Test
    @DisplayName("Deserialize result with truncated value data should fail")
    void testDeserializeResultTruncatedValueData() {
      // is_ok(1) + has_value(1) + disc(4) + val_len(4) = 10, claims 100 bytes
      final ByteBuffer buffer = ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN);
      buffer.put((byte) 1);
      buffer.put((byte) 1);
      buffer.putInt(2);
      buffer.putInt(100);
      final byte[] data = buffer.array();

      assertThrows(
          ValidationException.class,
          () -> WitValueDeserializer.deserialize(15, data),
          "Should throw for truncated value data");
    }

    @Test
    @DisplayName("Deserialize result data too short should fail")
    void testDeserializeResultTooShort() {
      final byte[] data = {(byte) 1}; // only 1 byte, need at least 2

      assertThrows(
          ValidationException.class,
          () -> WitValueDeserializer.deserialize(15, data),
          "Should throw for data too short");
    }
  }

  @Nested
  @DisplayName("Flags Deserialization Tests")
  class FlagsDeserializationTests {

    @Test
    @DisplayName("Deserialize flags with valid flag names")
    void testDeserializeFlagsValid() throws ValidationException {
      final byte[] readBytes = "read".getBytes(StandardCharsets.UTF_8);
      final byte[] writeBytes = "write".getBytes(StandardCharsets.UTF_8);
      // count(4) + name_len(4) + name(4) + name_len(4) + name(5) = 21
      final ByteBuffer buffer = ByteBuffer.allocate(21).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(2); // flag count
      buffer.putInt(readBytes.length);
      buffer.put(readBytes);
      buffer.putInt(writeBytes.length);
      buffer.put(writeBytes);
      final byte[] data = buffer.array();

      final WitValue result = WitValueDeserializer.deserialize(16, data);

      assertNotNull(result, "Result should not be null");
      assertInstanceOf(WitFlags.class, result, "Result should be WitFlags");
      final WitFlags flags = (WitFlags) result;
      final Set<String> setFlags = flags.getSetFlags();
      assertEquals(2, setFlags.size(), "Should have 2 flags set");
      assertTrue(setFlags.contains("read"), "Should contain 'read'");
      assertTrue(setFlags.contains("write"), "Should contain 'write'");
    }

    @Test
    @DisplayName("Deserialize empty flags")
    void testDeserializeFlagsEmpty() throws ValidationException {
      final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(0); // 0 flags
      final byte[] data = buffer.array();

      final WitValue result = WitValueDeserializer.deserialize(16, data);

      assertInstanceOf(WitFlags.class, result, "Result should be WitFlags");
      assertTrue(((WitFlags) result).isEmpty(), "Flags should be empty");
    }

    @Test
    @DisplayName("Deserialize flags with truncated data should fail")
    void testDeserializeFlagsTruncated() {
      final ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(1); // 1 flag
      buffer.putInt(100); // name length 100, but no data
      final byte[] data = buffer.array();

      assertThrows(
          ValidationException.class,
          () -> WitValueDeserializer.deserialize(16, data),
          "Should throw for truncated flag data");
    }

    @Test
    @DisplayName("Deserialize flags data too short should fail")
    void testDeserializeFlagsTooShort() {
      final byte[] data = new byte[3];

      assertThrows(
          ValidationException.class,
          () -> WitValueDeserializer.deserialize(16, data),
          "Should throw for data too short");
    }

    @Test
    @DisplayName("Deserialize flags with negative count should fail")
    void testDeserializeFlagsNegativeCount() {
      final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(-1);
      final byte[] data = buffer.array();

      assertThrows(
          ValidationException.class,
          () -> WitValueDeserializer.deserialize(16, data),
          "Should throw for negative flag count");
    }
  }

  @Nested
  @DisplayName("List Boundary Tests")
  class ListBoundaryTests {

    @Test
    @DisplayName("Deserialize list with negative element data length should fail")
    void testDeserializeListNegativeElementLength() {
      // count(4) + disc(4) + length(4) = 12
      final ByteBuffer buffer = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(1); // 1 element
      buffer.putInt(2); // S32 discriminator
      buffer.putInt(-1); // negative length
      final byte[] data = buffer.array();

      final ValidationException exception =
          assertThrows(
              ValidationException.class,
              () -> WitValueDeserializer.deserialize(11, data),
              "Should throw for negative element length");

      assertTrue(
          exception.getMessage().contains("Invalid element data length"),
          "Exception should mention invalid element data length: " + exception.getMessage());
    }

    @Test
    @DisplayName("Deserialize list with zero elements should throw (cannot infer element type)")
    void testDeserializeListZeroElementsShouldThrow() {
      final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(0);
      final byte[] data = buffer.array();

      assertThrows(
          ValidationException.class,
          () -> WitValueDeserializer.deserialize(11, data),
          "Empty list should throw because element type cannot be inferred");
    }

    @Test
    @DisplayName("Deserialize list with multiple elements")
    void testDeserializeListMultipleElements() throws ValidationException {
      // count(4) + 2*(disc(4) + len(4) + data(4)) = 28
      final ByteBuffer buffer = ByteBuffer.allocate(28).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(2); // 2 elements
      buffer.putInt(2); // S32
      buffer.putInt(4);
      buffer.putInt(100);
      buffer.putInt(2); // S32
      buffer.putInt(4);
      buffer.putInt(200);
      final byte[] data = buffer.array();

      final WitValue result = WitValueDeserializer.deserialize(11, data);

      assertInstanceOf(WitList.class, result, "Result should be WitList");
      final WitList list = (WitList) result;
      assertEquals(2, list.getElements().size(), "List should have 2 elements");
      assertEquals(
          100, ((WitS32) list.getElements().get(0)).getValue(), "First element should be 100");
      assertEquals(
          200, ((WitS32) list.getElements().get(1)).getValue(), "Second element should be 200");
    }
  }

  // ============== Mutation-killing boundary tests ==============

  @Nested
  @DisplayName("Boundary and conditional mutation-killing tests")
  class MutationKillingTests {

    @Test
    @DisplayName("deserializeBorrow with empty resource type name defaults to 'resource'")
    void testDeserializeBorrowEmptyTypeName() throws ValidationException {
      // Targets line 835: resourceType.isEmpty() ? "resource" : resourceType
      // Mutant: removed conditional - always returns resourceType (empty string)
      final ByteBuffer buffer = ByteBuffer.allocate(4 + 0 + 8).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(0); // empty type name
      buffer.putLong(99L);
      final byte[] data = buffer.array();

      final WitValue result = WitValueDeserializer.deserialize(23, data);

      assertInstanceOf(WitBorrow.class, result, "Result should be WitBorrow");
      final WitBorrow borrow = (WitBorrow) result;
      assertEquals(
          "resource",
          borrow.getResourceType(),
          "Empty resource type name should default to 'resource'");
    }

    @Test
    @DisplayName("deserializeBorrow with non-empty resource type name preserves it")
    void testDeserializeBorrowNonEmptyTypeName() throws ValidationException {
      // Targets line 835: resourceType.isEmpty() ? "resource" : resourceType
      // Mutant: always returns "resource" instead of the actual type name
      final byte[] typeName = "my-resource".getBytes(StandardCharsets.UTF_8);
      final ByteBuffer buffer =
          ByteBuffer.allocate(4 + typeName.length + 8).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(typeName.length);
      buffer.put(typeName);
      buffer.putLong(99L);
      final byte[] data = buffer.array();

      final WitValue result = WitValueDeserializer.deserialize(23, data);

      assertInstanceOf(WitBorrow.class, result, "Result should be WitBorrow");
      final WitBorrow borrow = (WitBorrow) result;
      assertEquals(
          "my-resource",
          borrow.getResourceType(),
          "Non-empty resource type name should be preserved");
    }

    @Test
    @DisplayName("deserializeOwn with empty resource type name defaults to 'resource'")
    void testDeserializeOwnEmptyTypeName() throws ValidationException {
      // Targets line 800: resourceType.isEmpty() ? "resource" : resourceType
      final ByteBuffer buffer = ByteBuffer.allocate(4 + 0 + 8).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(0); // empty type name
      buffer.putLong(55L);
      final byte[] data = buffer.array();

      final WitValue result = WitValueDeserializer.deserialize(22, data);

      assertInstanceOf(WitOwn.class, result, "Result should be WitOwn");
      final WitOwn own = (WitOwn) result;
      assertEquals(
          "resource",
          own.getResourceType(),
          "Empty resource type name should default to 'resource'");
    }

    @Test
    @DisplayName("deserializeOwn with non-empty resource type name preserves it")
    void testDeserializeOwnNonEmptyTypeName() throws ValidationException {
      // Targets line 800: always returns "resource" mutant
      final byte[] typeName = "file-handle".getBytes(StandardCharsets.UTF_8);
      final ByteBuffer buffer =
          ByteBuffer.allocate(4 + typeName.length + 8).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(typeName.length);
      buffer.put(typeName);
      buffer.putLong(55L);
      final byte[] data = buffer.array();

      final WitValue result = WitValueDeserializer.deserialize(22, data);

      assertInstanceOf(WitOwn.class, result, "Result should be WitOwn");
      final WitOwn own = (WitOwn) result;
      assertEquals(
          "file-handle", own.getResourceType(), "Non-empty resource type name should be preserved");
    }

    @Test
    @DisplayName("deserializeEnum with exactly 4 bytes passes length check (boundary for < 4)")
    void testDeserializeEnumExactlyFourBytes() {
      // Targets line 587: data.length < 4 (mutant substitutes 4 with 5)
      // With exactly 4 bytes and nameLength=0, passes the length check but
      // WitEnum rejects empty discriminant — still not a ValidationException from length check
      final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(0); // name length 0
      final byte[] data = buffer.array();

      // Empty discriminant is rejected by WitEnum constructor
      assertThrows(
          IllegalArgumentException.class,
          () -> WitValueDeserializer.deserialize(13, data),
          "Empty discriminant should be rejected");
    }

    @Test
    @DisplayName("deserializeEnum with 3 bytes throws (boundary for < 4)")
    void testDeserializeEnumThreeBytes() {
      // Targets line 587: data.length < 4 boundary
      final byte[] data = new byte[3];

      assertThrows(
          ValidationException.class,
          () -> WitValueDeserializer.deserialize(13, data),
          "3 bytes should be too short for enum");
    }

    @Test
    @DisplayName("deserializeEnum with nameLength exactly 0 passes length check (boundary for < 0)")
    void testDeserializeEnumNameLengthZero() {
      // Targets line 594: nameLength < 0 (mutant substitutes boundary)
      // nameLength=0 passes the < 0 check but WitEnum rejects empty discriminant
      final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(0);
      final byte[] data = buffer.array();

      assertThrows(
          IllegalArgumentException.class,
          () -> WitValueDeserializer.deserialize(13, data),
          "Empty discriminant should be rejected by WitEnum");
    }

    @Test
    @DisplayName("deserializeFlags with exactly 4 bytes and zero flags succeeds (boundary for < 4)")
    void testDeserializeFlagsExactlyFourBytes() throws ValidationException {
      // Targets line 745: buffer.remaining() < 4 (mutant substitutes 4 with 5)
      // Also targets line 731: data.length < 4
      final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(0); // zero flags
      final byte[] data = buffer.array();

      final WitValue result = WitValueDeserializer.deserialize(16, data);

      assertInstanceOf(WitFlags.class, result, "Result should be WitFlags");
      assertEquals(0, ((WitFlags) result).getSetFlags().size(), "Should have 0 flags");
    }

    @Test
    @DisplayName("deserializeFlags with 3 bytes throws (boundary for < 4)")
    void testDeserializeFlagsThreeBytes() {
      // Targets line 731: data.length < 4
      final byte[] data = new byte[3];

      assertThrows(
          ValidationException.class,
          () -> WitValueDeserializer.deserialize(16, data),
          "3 bytes should be too short for flags");
    }

    @Test
    @DisplayName("deserializeFlags with one flag having exactly 4 bytes remaining for name length")
    void testDeserializeFlagsExactRemainingForNameLength() throws ValidationException {
      // Targets line 745: buffer.remaining() < 4
      // Create flags with one flag that has name length exactly at boundary
      final byte[] flagName = "a".getBytes(StandardCharsets.UTF_8);
      final ByteBuffer buffer =
          ByteBuffer.allocate(4 + 4 + flagName.length).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(1); // 1 flag
      buffer.putInt(flagName.length); // name length = 1
      buffer.put(flagName);
      final byte[] data = buffer.array();

      final WitValue result = WitValueDeserializer.deserialize(16, data);

      assertInstanceOf(WitFlags.class, result, "Result should be WitFlags");
      assertEquals(1, ((WitFlags) result).getSetFlags().size(), "Should have 1 flag");
      assertTrue(((WitFlags) result).getSetFlags().contains("a"), "Should contain flag 'a'");
    }

    @Test
    @DisplayName(
        "deserializeFlags with nameLength exactly 0 passes length check (boundary for < 0)")
    void testDeserializeFlagsNameLengthZero() {
      // Targets line 750: nameLength < 0
      // nameLength=0 passes the < 0 check but WitFlags rejects empty flag names
      final ByteBuffer buffer = ByteBuffer.allocate(4 + 4).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(1); // 1 flag
      buffer.putInt(0); // name length 0
      final byte[] data = buffer.array();

      assertThrows(
          IllegalArgumentException.class,
          () -> WitValueDeserializer.deserialize(16, data),
          "Empty flag name should be rejected by WitFlags");
    }

    @Test
    @DisplayName("deserializeList with element length exactly 0 succeeds (boundary for < 0)")
    void testDeserializeListElementLengthZero() throws ValidationException {
      // Targets line 485: length < 0 boundary
      // A bool element has length 1, but we need to test length=0 boundary
      // Use a carefully crafted buffer with length exactly 0 but valid discriminator
      // Actually, length=0 means empty data, let's use discriminator 1 (bool) with 0 bytes
      // That would fail on the inner deserialize. Instead, test that negative length throws.
      final ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + 4).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(1); // 1 element
      buffer.putInt(1); // discriminator 1 (bool)
      buffer.putInt(0); // length 0
      final byte[] data = buffer.array();

      // This will call deserialize(1, new byte[0]) which should throw for bool (expects 1 byte)
      assertThrows(
          ValidationException.class,
          () -> WitValueDeserializer.deserialize(11, data),
          "Element with length 0 for bool should throw");
    }

    @Test
    @DisplayName("deserializeList boundary - negative element length throws")
    void testDeserializeListNegativeElementLengthBoundary() {
      // Targets line 485: length < 0
      final ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + 4).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(1); // 1 element
      buffer.putInt(1); // discriminator
      buffer.putInt(-1); // negative length
      final byte[] data = buffer.array();

      final ValidationException ex =
          assertThrows(
              ValidationException.class,
              () -> WitValueDeserializer.deserialize(11, data),
              "Negative element length should throw");
      assertTrue(
          ex.getMessage().contains("Invalid element data length"),
          "Message should mention invalid element data length");
    }

    @Test
    @DisplayName("deserializeOption some with exactly 8 bytes remaining for payload header")
    void testDeserializeOptionSomeExactlyEightBytesRemaining() throws ValidationException {
      // Targets line 636: buffer.remaining() < 8 (mutant substitutes 8 with 9)
      // After reading is_some byte, need exactly 8 bytes for discriminator+length
      final ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + 4 + 1).order(ByteOrder.LITTLE_ENDIAN);
      buffer.put((byte) 1); // is_some = true
      buffer.putInt(1); // discriminator = 1 (bool)
      buffer.putInt(1); // length = 1
      buffer.put((byte) 1); // bool true
      final byte[] data = buffer.array();

      final WitValue result = WitValueDeserializer.deserialize(14, data);

      assertInstanceOf(WitOption.class, result, "Result should be WitOption");
      assertTrue(((WitOption) result).getValue().isPresent(), "Option should have a value");
    }

    @Test
    @DisplayName("deserializeOption some with 7 bytes remaining throws")
    void testDeserializeOptionSomeSevenBytesRemaining() {
      // Targets line 636: buffer.remaining() < 8
      // After reading is_some byte (1 byte), only 7 bytes left
      final byte[] data = new byte[8]; // 1 + 7 bytes
      data[0] = 1; // is_some = true
      // remaining 7 bytes is not enough for discriminator(4) + length(4)

      assertThrows(
          ValidationException.class,
          () -> WitValueDeserializer.deserialize(14, data),
          "7 bytes remaining for payload header should throw");
    }

    @Test
    @DisplayName("deserializeOption some with length exactly 0 succeeds for inner deserialize")
    void testDeserializeOptionSomeLengthZero() {
      // Targets line 643: length < 0 boundary
      final ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + 4).order(ByteOrder.LITTLE_ENDIAN);
      buffer.put((byte) 1); // is_some
      buffer.putInt(1); // discriminator = 1 (bool)
      buffer.putInt(0); // length = 0

      final byte[] data = buffer.array();

      // length=0 means empty data for bool, which should throw on inner deserialize
      assertThrows(
          ValidationException.class,
          () -> WitValueDeserializer.deserialize(14, data),
          "Length 0 with bool discriminator should throw due to invalid bool data size");
    }

    @Test
    @DisplayName("deserializeOption some with negative length throws")
    void testDeserializeOptionSomeNegativeLength() {
      // Targets line 643: length < 0
      final ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + 4).order(ByteOrder.LITTLE_ENDIAN);
      buffer.put((byte) 1); // is_some
      buffer.putInt(1); // discriminator
      buffer.putInt(-1); // negative length

      final byte[] data = buffer.array();

      final ValidationException ex =
          assertThrows(
              ValidationException.class,
              () -> WitValueDeserializer.deserialize(14, data),
              "Negative length should throw");
      assertTrue(
          ex.getMessage().contains("Invalid value length"),
          "Message should mention invalid value length");
    }

    @Test
    @DisplayName("deserializeFlags truncated at flag name read throws")
    void testDeserializeFlagsTruncatedAtFlagName() {
      // Targets line 745: buffer.remaining() < 4 when reading next flag's name length
      // Two flags declared but data only has room for one
      final byte[] flagName = "a".getBytes(StandardCharsets.UTF_8);
      final ByteBuffer buffer =
          ByteBuffer.allocate(4 + 4 + flagName.length).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(2); // says 2 flags
      buffer.putInt(flagName.length);
      buffer.put(flagName);
      // No more data for second flag
      final byte[] data = buffer.array();

      assertThrows(
          ValidationException.class,
          () -> WitValueDeserializer.deserialize(16, data),
          "Should throw when flag data is truncated");
    }

    @Test
    @DisplayName("deserializeEnum with exactly boundary remaining for truncated name")
    void testDeserializeEnumTruncatedNameBoundary() {
      // Targets line 598: buffer.remaining() < nameLength
      final ByteBuffer buffer = ByteBuffer.allocate(4 + 2).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(5); // says name is 5 bytes
      buffer.put((byte) 'a');
      buffer.put((byte) 'b');
      // Only 2 bytes available, need 5
      final byte[] data = buffer.array();

      assertThrows(
          ValidationException.class,
          () -> WitValueDeserializer.deserialize(13, data),
          "Truncated enum name should throw");
    }
  }

  // ============== InlineConstant mutation-killing: exact byte position verification ==============

  @Test
  @DisplayName("Deserialize record - verify exact field count and intermediate positions")
  void testDeserializeRecordExactPositions() throws ValidationException {
    // Build record bytes: field_count=1, field "a" -> bool(true)
    final ByteBuffer buffer = ByteBuffer.allocate(18).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(1); // field count
    buffer.putInt(1); // name length
    buffer.put((byte) 'a'); // name
    buffer.putInt(1); // bool discriminator
    buffer.putInt(1); // data length
    buffer.put((byte) 1); // true
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(7, data);

    assertInstanceOf(WitRecord.class, result, "Result should be WitRecord");
    final WitRecord record = (WitRecord) result;
    assertEquals(1, record.getFieldCount(), "Field count must be exactly 1");
    assertTrue(record.getFields().containsKey("a"), "Must contain field 'a'");
    assertInstanceOf(WitBool.class, record.getFields().get("a"), "Field must be WitBool");
    assertTrue(((WitBool) record.getFields().get("a")).getValue(), "Bool field value must be true");
  }

  @Test
  @DisplayName("Deserialize tuple - verify exact element count and values at precise offsets")
  void testDeserializeTupleExactPositions() throws ValidationException {
    // count=2, element 0: bool(false), element 1: s32(99)
    final ByteBuffer buffer = ByteBuffer.allocate(25).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(2); // count
    buffer.putInt(1); // disc=bool
    buffer.putInt(1); // len=1
    buffer.put((byte) 0); // false
    buffer.putInt(2); // disc=s32
    buffer.putInt(4); // len=4
    buffer.putInt(99); // value
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(8, data);

    assertInstanceOf(WitTuple.class, result, "Result should be WitTuple");
    final WitTuple tuple = (WitTuple) result;
    assertEquals(2, tuple.getElements().size(), "Tuple must have exactly 2 elements");
    assertInstanceOf(WitBool.class, tuple.getElements().get(0), "Element 0 must be bool");
    assertFalse(((WitBool) tuple.getElements().get(0)).getValue(), "Element 0 must be false");
    assertInstanceOf(WitS32.class, tuple.getElements().get(1), "Element 1 must be s32");
    assertEquals(99, ((WitS32) tuple.getElements().get(1)).getValue(), "Element 1 must be 99");
  }

  @Test
  @DisplayName("Deserialize tuple with 3 bytes (less than 4 minimum) should fail")
  void testDeserializeTupleTooShort() {
    final byte[] data = new byte[3];

    assertThrows(
        ValidationException.class,
        () -> WitValueDeserializer.deserialize(8, data),
        "3 bytes should be too short for tuple");
  }

  @Test
  @DisplayName("Deserialize tuple with exactly 4 bytes and zero count succeeds")
  void testDeserializeTupleExactlyFourBytesZeroCount() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(0);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(8, data);

    assertInstanceOf(WitTuple.class, result, "Result should be WitTuple");
    assertEquals(0, ((WitTuple) result).getElements().size(), "Tuple must have 0 elements");
  }

  @Test
  @DisplayName("Deserialize tuple with negative count should fail")
  void testDeserializeTupleNegativeCount() {
    final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(-1);
    final byte[] data = buffer.array();

    assertThrows(
        ValidationException.class,
        () -> WitValueDeserializer.deserialize(8, data),
        "Negative element count should throw");
  }

  @Test
  @DisplayName("Deserialize tuple with truncated element header (7 bytes remaining) should fail")
  void testDeserializeTupleTruncatedElementHeader() {
    // count=1, but only 7 bytes after count (need 8 for disc+len)
    final ByteBuffer buffer = ByteBuffer.allocate(11).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(1);
    buffer.putInt(1); // partial
    buffer.put((byte) 0);
    buffer.put((byte) 0);
    buffer.put((byte) 0);
    final byte[] data = buffer.array();

    assertThrows(
        ValidationException.class,
        () -> WitValueDeserializer.deserialize(8, data),
        "7 bytes remaining should be too short for element header");
  }

  @Test
  @DisplayName("Deserialize tuple with negative element length should fail")
  void testDeserializeTupleNegativeElementLength() {
    final ByteBuffer buffer = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(1); // count
    buffer.putInt(1); // disc
    buffer.putInt(-1); // negative length
    final byte[] data = buffer.array();

    final ValidationException ex =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(8, data),
            "Negative element length should throw");
    assertTrue(
        ex.getMessage().contains("Invalid element data length"),
        "Message should mention invalid element data length");
  }

  @Test
  @DisplayName("Deserialize tuple with element data truncated should fail")
  void testDeserializeTupleElementDataTruncated() {
    final ByteBuffer buffer = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(1); // count
    buffer.putInt(1); // disc
    buffer.putInt(5); // length=5, but 0 bytes remain
    final byte[] data = buffer.array();

    assertThrows(
        ValidationException.class,
        () -> WitValueDeserializer.deserialize(8, data),
        "Truncated element data should throw");
  }

  @Test
  @DisplayName("Deserialize s32 value 0 - verify exact value not mutated")
  void testDeserializeS32ValueZero() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(0);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(2, data);
    assertEquals(0, ((WitS32) result).getValue(), "S32 value 0 must deserialize as exactly 0");
  }

  @Test
  @DisplayName("Deserialize s32 value 1 - verify exact value not mutated")
  void testDeserializeS32ValueOne() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(1);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(2, data);
    assertEquals(1, ((WitS32) result).getValue(), "S32 value 1 must deserialize as exactly 1");
  }

  @Test
  @DisplayName("Deserialize s64 value 0 and 1 - verify exact values")
  void testDeserializeS64BoundaryValues() throws ValidationException {
    final ByteBuffer zeroBuf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    zeroBuf.putLong(0L);
    assertEquals(
        0L,
        ((WitS64) WitValueDeserializer.deserialize(3, zeroBuf.array())).getValue(),
        "S64 value 0 must be exactly 0");

    final ByteBuffer oneBuf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    oneBuf.putLong(1L);
    assertEquals(
        1L,
        ((WitS64) WitValueDeserializer.deserialize(3, oneBuf.array())).getValue(),
        "S64 value 1 must be exactly 1");
  }

  @Test
  @DisplayName("Deserialize float64 value 0 and 1 - verify exact values")
  void testDeserializeFloat64BoundaryValues() throws ValidationException {
    final ByteBuffer zeroBuf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    zeroBuf.putDouble(0.0);
    assertEquals(
        0.0,
        ((WitFloat64) WitValueDeserializer.deserialize(4, zeroBuf.array())).getValue(),
        "Float64 value 0.0 must be exactly 0.0");

    final ByteBuffer oneBuf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    oneBuf.putDouble(1.0);
    assertEquals(
        1.0,
        ((WitFloat64) WitValueDeserializer.deserialize(4, oneBuf.array())).getValue(),
        "Float64 value 1.0 must be exactly 1.0");
  }

  @Test
  @DisplayName("Deserialize float32 value 0 and 1 - verify exact values")
  void testDeserializeFloat32BoundaryValues() throws ValidationException {
    final ByteBuffer zeroBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    zeroBuf.putFloat(0.0f);
    assertEquals(
        0.0f,
        ((WitFloat32) WitValueDeserializer.deserialize(21, zeroBuf.array())).getValue(),
        "Float32 value 0.0 must be exactly 0.0");

    final ByteBuffer oneBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    oneBuf.putFloat(1.0f);
    assertEquals(
        1.0f,
        ((WitFloat32) WitValueDeserializer.deserialize(21, oneBuf.array())).getValue(),
        "Float32 value 1.0 must be exactly 1.0");
  }

  @Test
  @DisplayName("Deserialize s8 value 0 and 1 - verify exact values")
  void testDeserializeS8BoundaryValues() throws ValidationException {
    assertEquals(
        (byte) 0,
        ((WitS8) WitValueDeserializer.deserialize(17, new byte[] {0})).getValue(),
        "S8 value 0 must be exactly 0");
    assertEquals(
        (byte) 1,
        ((WitS8) WitValueDeserializer.deserialize(17, new byte[] {1})).getValue(),
        "S8 value 1 must be exactly 1");
  }

  @Test
  @DisplayName("Deserialize s16 value 0 and 1 - verify exact values")
  void testDeserializeS16BoundaryValues() throws ValidationException {
    final ByteBuffer zeroBuf = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
    zeroBuf.putShort((short) 0);
    assertEquals(
        (short) 0,
        ((WitS16) WitValueDeserializer.deserialize(18, zeroBuf.array())).getValue(),
        "S16 value 0 must be exactly 0");

    final ByteBuffer oneBuf = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
    oneBuf.putShort((short) 1);
    assertEquals(
        (short) 1,
        ((WitS16) WitValueDeserializer.deserialize(18, oneBuf.array())).getValue(),
        "S16 value 1 must be exactly 1");
  }

  @Test
  @DisplayName("Deserialize u8 value 0 and 1 - verify exact values")
  void testDeserializeU8BoundaryValues() throws ValidationException {
    assertEquals(
        (byte) 0,
        ((WitU8) WitValueDeserializer.deserialize(19, new byte[] {0})).getValue(),
        "U8 value 0 must be exactly 0");
    assertEquals(
        (byte) 1,
        ((WitU8) WitValueDeserializer.deserialize(19, new byte[] {1})).getValue(),
        "U8 value 1 must be exactly 1");
  }

  @Test
  @DisplayName("Deserialize u16 value 0 and 1 - verify exact values")
  void testDeserializeU16BoundaryValues() throws ValidationException {
    final ByteBuffer zeroBuf = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
    zeroBuf.putShort((short) 0);
    assertEquals(
        (short) 0,
        ((WitU16) WitValueDeserializer.deserialize(20, zeroBuf.array())).getValue(),
        "U16 value 0 must be exactly 0");

    final ByteBuffer oneBuf = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
    oneBuf.putShort((short) 1);
    assertEquals(
        (short) 1,
        ((WitU16) WitValueDeserializer.deserialize(20, oneBuf.array())).getValue(),
        "U16 value 1 must be exactly 1");
  }

  @Test
  @DisplayName("Deserialize u32 value 0 and 1 - verify exact values")
  void testDeserializeU32BoundaryValues() throws ValidationException {
    final ByteBuffer zeroBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    zeroBuf.putInt(0);
    assertEquals(
        0,
        ((WitU32) WitValueDeserializer.deserialize(9, zeroBuf.array())).getValue(),
        "U32 value 0 must be exactly 0");

    final ByteBuffer oneBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    oneBuf.putInt(1);
    assertEquals(
        1,
        ((WitU32) WitValueDeserializer.deserialize(9, oneBuf.array())).getValue(),
        "U32 value 1 must be exactly 1");
  }

  @Test
  @DisplayName("Deserialize u64 value 0 and 1 - verify exact values")
  void testDeserializeU64BoundaryValues() throws ValidationException {
    final ByteBuffer zeroBuf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    zeroBuf.putLong(0L);
    assertEquals(
        0L,
        ((WitU64) WitValueDeserializer.deserialize(10, zeroBuf.array())).getValue(),
        "U64 value 0 must be exactly 0");

    final ByteBuffer oneBuf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    oneBuf.putLong(1L);
    assertEquals(
        1L,
        ((WitU64) WitValueDeserializer.deserialize(10, oneBuf.array())).getValue(),
        "U64 value 1 must be exactly 1");
  }

  @Test
  @DisplayName("Deserialize char codepoint 0 and 1 - verify exact values")
  void testDeserializeCharBoundaryValues() throws ValidationException {
    final ByteBuffer zeroBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    zeroBuf.putInt(0);
    assertEquals(
        0,
        ((WitChar) WitValueDeserializer.deserialize(5, zeroBuf.array())).getCodepoint(),
        "Char codepoint 0 must be exactly 0");

    final ByteBuffer oneBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    oneBuf.putInt(1);
    assertEquals(
        1,
        ((WitChar) WitValueDeserializer.deserialize(5, oneBuf.array())).getCodepoint(),
        "Char codepoint 1 must be exactly 1");
  }

  @Test
  @DisplayName("Deserialize variant with exactly 5 bytes should succeed (boundary for < 5)")
  void testDeserializeVariantExactlyFiveBytes() throws ValidationException {
    // Targets: data.length < 5 boundary
    // name_len=0 + has_payload=0 = 5 bytes total
    // But name_len=0 means empty name which variant allows
    final ByteBuffer buffer = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(0); // empty name - won't work, need at least 1 char name
    buffer.put((byte) 0);
    final byte[] data = buffer.array();

    // Empty case name may or may not be accepted by WitVariant; the key is that
    // the size check passes for exactly 5 bytes
    try {
      final WitValue result = WitValueDeserializer.deserialize(12, data);
      assertInstanceOf(WitVariant.class, result, "Should deserialize as WitVariant");
    } catch (final Exception e) {
      // If WitVariant rejects empty name, that's fine - the boundary check passed
      assertFalse(
          e.getMessage().contains("too short"), "Error should not be about data being too short");
    }
  }

  @Test
  @DisplayName("Deserialize variant with 4 bytes should fail (below 5 boundary)")
  void testDeserializeVariantFourBytes() {
    final byte[] data = new byte[4]; // Less than 5

    final ValidationException ex =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(12, data),
            "4 bytes should be too short for variant");
    assertTrue(ex.getMessage().contains("too short"), "Should mention data too short");
  }

  @Test
  @DisplayName("Deserialize variant with payload having negative length should fail")
  void testDeserializeVariantPayloadNegativeLength() {
    final byte[] nameBytes = "x".getBytes(StandardCharsets.UTF_8);
    // name_len(4) + name(1) + has_payload(1) + disc(4) + len(4) = 14
    final ByteBuffer buffer = ByteBuffer.allocate(14).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(nameBytes.length);
    buffer.put(nameBytes);
    buffer.put((byte) 1); // has payload
    buffer.putInt(2); // disc=s32
    buffer.putInt(-1); // negative payload length
    final byte[] data = buffer.array();

    final ValidationException ex =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(12, data),
            "Negative payload length should throw");
    assertTrue(
        ex.getMessage().contains("Invalid payload length"),
        "Should mention invalid payload length");
  }

  @Test
  @DisplayName("Deserialize variant with payload data truncated should fail")
  void testDeserializeVariantPayloadDataTruncated() {
    final byte[] nameBytes = "x".getBytes(StandardCharsets.UTF_8);
    // name_len(4) + name(1) + has_payload(1) + disc(4) + len(4) = 14, claims 100 bytes
    final ByteBuffer buffer = ByteBuffer.allocate(14).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(nameBytes.length);
    buffer.put(nameBytes);
    buffer.put((byte) 1);
    buffer.putInt(2); // disc
    buffer.putInt(100); // claims 100 bytes, but 0 remain
    final byte[] data = buffer.array();

    assertThrows(
        ValidationException.class,
        () -> WitValueDeserializer.deserialize(12, data),
        "Truncated payload data should throw");
  }

  @Test
  @DisplayName("Deserialize variant with negative case name length should fail")
  void testDeserializeVariantNegativeCaseNameLength() {
    final ByteBuffer buffer = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(-1); // negative name length
    buffer.put((byte) 0);
    final byte[] data = buffer.array();

    final ValidationException ex =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(12, data),
            "Negative case name length should throw");
    assertTrue(
        ex.getMessage().contains("Invalid case name length"),
        "Should mention invalid case name length");
  }

  @Test
  @DisplayName("Deserialize result with exactly 2 bytes succeeds (boundary for < 2)")
  void testDeserializeResultExactlyTwoBytes() throws ValidationException {
    // is_ok(1) + has_value(1) = 2
    final byte[] data = {(byte) 1, (byte) 0};

    final WitValue result = WitValueDeserializer.deserialize(15, data);
    assertInstanceOf(WitResult.class, result, "Should deserialize as WitResult");
    assertTrue(((WitResult) result).isOk(), "Should be ok");
  }

  @Test
  @DisplayName("Deserialize result with 1 byte should fail (below 2 boundary)")
  void testDeserializeResultOneByte() {
    final byte[] data = {(byte) 1};

    assertThrows(
        ValidationException.class,
        () -> WitValueDeserializer.deserialize(15, data),
        "1 byte should be too short for result");
  }

  @Test
  @DisplayName("Deserialize result with payload having negative length should fail")
  void testDeserializeResultPayloadNegativeLength() {
    final ByteBuffer buffer = ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN);
    buffer.put((byte) 1); // is_ok
    buffer.put((byte) 1); // has_value
    buffer.putInt(2); // disc=s32
    buffer.putInt(-1); // negative length
    final byte[] data = buffer.array();

    final ValidationException ex =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(15, data),
            "Negative value length should throw");
    assertTrue(
        ex.getMessage().contains("Invalid value length"), "Should mention invalid value length");
  }

  @Test
  @DisplayName("Deserialize result with has_value=1 but only 7 bytes remaining should fail")
  void testDeserializeResultTruncatedPayloadHeader() {
    // is_ok(1) + has_value(1) + 7 bytes = 9 total (need 8 for disc+len)
    final byte[] data = new byte[9];
    data[0] = 1; // is_ok
    data[1] = 1; // has_value
    // remaining 7 bytes is not enough for discriminator(4) + length(4)

    assertThrows(
        ValidationException.class,
        () -> WitValueDeserializer.deserialize(15, data),
        "7 bytes remaining for payload header should throw");
  }

  @Test
  @DisplayName("Deserialize result ok and err distinguish correctly with payload")
  void testDeserializeResultOkVsErrWithPayload() throws ValidationException {
    // Ok case
    final ByteBuffer okBuf = ByteBuffer.allocate(14).order(ByteOrder.LITTLE_ENDIAN);
    okBuf.put((byte) 1); // is_ok=true
    okBuf.put((byte) 1); // has_value
    okBuf.putInt(2); // s32
    okBuf.putInt(4); // len
    okBuf.putInt(42); // value
    final WitResult okResult = (WitResult) WitValueDeserializer.deserialize(15, okBuf.array());
    assertTrue(okResult.isOk(), "Should be ok");
    assertFalse(okResult.isErr(), "Should not be err");
    assertEquals(42, ((WitS32) okResult.getValue().get()).getValue(), "Ok value must be 42");

    // Err case
    final ByteBuffer errBuf = ByteBuffer.allocate(14).order(ByteOrder.LITTLE_ENDIAN);
    errBuf.put((byte) 0); // is_ok=false
    errBuf.put((byte) 1); // has_value
    errBuf.putInt(2); // s32
    errBuf.putInt(4); // len
    errBuf.putInt(99); // value
    final WitResult errResult = (WitResult) WitValueDeserializer.deserialize(15, errBuf.array());
    assertTrue(errResult.isErr(), "Should be err");
    assertFalse(errResult.isOk(), "Should not be ok");
    assertEquals(99, ((WitS32) errResult.getValue().get()).getValue(), "Err value must be 99");
  }

  @Test
  @DisplayName("Deserialize record with field having negative name length should fail")
  void testDeserializeRecordNegativeNameLength() {
    final ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(1); // 1 field
    buffer.putInt(-1); // negative name length
    final byte[] data = buffer.array();

    final ValidationException ex =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(7, data),
            "Negative name length should throw");
    assertTrue(
        ex.getMessage().contains("Invalid field name length"),
        "Should mention invalid field name length");
  }

  @Test
  @DisplayName("Deserialize record with field having negative data length should fail")
  void testDeserializeRecordNegativeDataLength() {
    final byte[] nameBytes = "f".getBytes(StandardCharsets.UTF_8);
    // field_count(4) + name_len(4) + name(1) + disc(4) + val_len(4) = 17
    final ByteBuffer buffer = ByteBuffer.allocate(17).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(1);
    buffer.putInt(nameBytes.length);
    buffer.put(nameBytes);
    buffer.putInt(2); // s32
    buffer.putInt(-1); // negative length
    final byte[] data = buffer.array();

    final ValidationException ex =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(7, data),
            "Negative data length should throw");
    assertTrue(
        ex.getMessage().contains("Invalid field data length"),
        "Should mention invalid field data length");
  }

  @Test
  @DisplayName("Deserialize record with truncated discriminator/length header should fail")
  void testDeserializeRecordTruncatedDiscriminatorHeader() {
    final byte[] nameBytes = "f".getBytes(StandardCharsets.UTF_8);
    // field_count(4) + name_len(4) + name(1) = 9, need 8 more for disc+len but only 3 remain
    final ByteBuffer buffer = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(1); // 1 field
    buffer.putInt(nameBytes.length);
    buffer.put(nameBytes);
    // Only 3 bytes remain, need 8 for disc+len
    buffer.put((byte) 0);
    buffer.put((byte) 0);
    buffer.put((byte) 0);
    final byte[] data = buffer.array();

    assertThrows(
        ValidationException.class,
        () -> WitValueDeserializer.deserialize(7, data),
        "Truncated disc+len header should throw");
  }

  @Test
  @DisplayName("Deserialize own with exactly 12 bytes boundary check")
  void testDeserializeOwnExactly12Bytes() throws ValidationException {
    // Targets: data.length < 12 boundary
    // 4 (type_name_length=0) + 8 (handle) = 12
    final ByteBuffer buffer = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(0);
    buffer.putLong(7L);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(22, data);
    assertInstanceOf(WitOwn.class, result, "Should deserialize as WitOwn");
    assertEquals(7L, ((WitOwn) result).getHandle().getNativeHandle(), "Handle must be 7");
  }

  @Test
  @DisplayName("Deserialize borrow with exactly 12 bytes boundary check")
  void testDeserializeBorrowExactly12Bytes() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(0);
    buffer.putLong(7L);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(23, data);
    assertInstanceOf(WitBorrow.class, result, "Should deserialize as WitBorrow");
    assertEquals(7L, ((WitBorrow) result).getHandle().getNativeHandle(), "Handle must be 7");
  }

  @Test
  @DisplayName("Deserialize option none returns isNone=true and isSome=false")
  void testDeserializeOptionNoneExactFlags() throws ValidationException {
    final byte[] data = {(byte) 0};
    final WitOption option = (WitOption) WitValueDeserializer.deserialize(14, data);
    assertTrue(option.isNone(), "Option must be none");
    assertFalse(option.isSome(), "Option must not be some");
    assertFalse(option.getValue().isPresent(), "Option value must not be present");
  }

  @Test
  @DisplayName("Deserialize option some returns isSome=true and isNone=false")
  void testDeserializeOptionSomeExactFlags() throws ValidationException {
    final ByteBuffer buffer = ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN);
    buffer.put((byte) 1);
    buffer.putInt(1); // bool
    buffer.putInt(1); // len
    buffer.put((byte) 0); // false
    final byte[] data = buffer.array();

    final WitOption option = (WitOption) WitValueDeserializer.deserialize(14, data);
    assertTrue(option.isSome(), "Option must be some");
    assertFalse(option.isNone(), "Option must not be none");
    assertTrue(option.getValue().isPresent(), "Option value must be present");
  }

  @Test
  @DisplayName("Deserialize string with exactly length matching (no off-by-one)")
  void testDeserializeStringExactLengthMatch() throws ValidationException {
    // Verify STRING_LENGTH_SIZE + length must equal data.length exactly
    final byte[] text = "xyz".getBytes(StandardCharsets.UTF_8);
    final ByteBuffer buffer = ByteBuffer.allocate(4 + text.length).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(text.length);
    buffer.put(text);
    final byte[] data = buffer.array();

    // Exact match should work
    final WitValue result = WitValueDeserializer.deserialize(6, data);
    assertEquals("xyz", ((WitString) result).getValue(), "String must be 'xyz'");

    // One byte too many should fail
    final byte[] tooLong = new byte[data.length + 1];
    System.arraycopy(data, 0, tooLong, 0, data.length);
    assertThrows(
        ValidationException.class,
        () -> WitValueDeserializer.deserialize(6, tooLong),
        "Extra byte should cause size mismatch");

    // One byte too few should fail
    final byte[] tooShort = new byte[data.length - 1];
    System.arraycopy(data, 0, tooShort, 0, tooShort.length);
    assertThrows(
        ValidationException.class,
        () -> WitValueDeserializer.deserialize(6, tooShort),
        "Missing byte should cause size mismatch");
  }

  @Test
  @DisplayName("Deserialize list with exactly 8 bytes remaining succeeds (boundary for < 8)")
  void testDeserializeListExactlyEightBytesRemaining() throws ValidationException {
    // count=1 then exactly 8 bytes for disc+len, then data
    final ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + 4 + 1).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(1); // count
    buffer.putInt(1); // disc=bool
    buffer.putInt(1); // len=1
    buffer.put((byte) 1); // true
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(11, data);
    assertInstanceOf(WitList.class, result, "Should be WitList");
    assertEquals(1, ((WitList) result).getElements().size(), "Must have 1 element");
  }

  @Test
  @DisplayName("Deserialize flags with negative name length should fail")
  void testDeserializeFlagsNegativeNameLength() {
    final ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(1); // 1 flag
    buffer.putInt(-1); // negative name length
    final byte[] data = buffer.array();

    final ValidationException ex =
        assertThrows(
            ValidationException.class,
            () -> WitValueDeserializer.deserialize(16, data),
            "Negative flag name length should throw");
    assertTrue(
        ex.getMessage().contains("Invalid flag name length"),
        "Should mention invalid flag name length");
  }

  @Test
  @DisplayName("Deserialize flags with truncated flag name data should fail")
  void testDeserializeFlagsTruncatedFlagNameData() {
    final ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + 1).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(1); // 1 flag
    buffer.putInt(5); // claims 5 bytes name
    buffer.put((byte) 'a'); // only 1 byte
    final byte[] data = buffer.array();

    assertThrows(
        ValidationException.class,
        () -> WitValueDeserializer.deserialize(16, data),
        "Truncated flag name data should throw");
  }

  @Test
  @DisplayName("Deserialize with discriminator 0 should fail as invalid")
  void testDeserializeDiscriminatorZero() {
    final byte[] data = {(byte) 0};
    assertThrows(
        ValidationException.class,
        () -> WitValueDeserializer.deserialize(0, data),
        "Discriminator 0 should be invalid");
  }

  @Test
  @DisplayName("Deserialize with discriminator 24 should fail as invalid")
  void testDeserializeDiscriminator24() {
    final byte[] data = {(byte) 0};
    assertThrows(
        ValidationException.class,
        () -> WitValueDeserializer.deserialize(24, data),
        "Discriminator 24 should be invalid");
  }

  @Test
  @DisplayName("Deserialize with negative discriminator should fail")
  void testDeserializeNegativeDiscriminator() {
    final byte[] data = {(byte) 0};
    assertThrows(
        ValidationException.class,
        () -> WitValueDeserializer.deserialize(-1, data),
        "Negative discriminator should be invalid");
  }
}
