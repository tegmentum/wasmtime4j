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
  @DisplayName("Deserialize own with exactly 8 bytes (minimum) should work with empty type name")
  void testDeserializeOwnExactlyMinimumBytes() throws ValidationException {
    // type_length=0, index=42
    final ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(0); // type name length = 0
    buffer.putInt(42); // index = 42
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(22, data);

    assertNotNull(result, "Result should not be null");
    assertInstanceOf(WitOwn.class, result, "Result should be WitOwn");
    assertEquals(42, ((WitOwn) result).getIndex(), "Index should be 42");
  }

  @Test
  @DisplayName("Deserialize own with 7 bytes (less than minimum) should fail")
  void testDeserializeOwnLessThanMinimumBytes() {
    final byte[] data = new byte[7]; // Less than 8 bytes needed

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
    final ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(-1); // negative type name length
    buffer.putInt(42); // index
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
    // type_length=10, but only 3 bytes of type name + index available
    final ByteBuffer buffer = ByteBuffer.allocate(11).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(10); // type name length = 10
    buffer.put("abc".getBytes(StandardCharsets.UTF_8)); // only 3 bytes
    buffer.putInt(42); // index (would overwrite if we had enough space)
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
  @DisplayName("Deserialize own with valid type name and index")
  void testDeserializeOwnWithTypeName() throws ValidationException {
    final String typeName = "myresource";
    final byte[] typeNameBytes = typeName.getBytes(StandardCharsets.UTF_8);
    final ByteBuffer buffer =
        ByteBuffer.allocate(4 + typeNameBytes.length + 4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(typeNameBytes.length);
    buffer.put(typeNameBytes);
    buffer.putInt(123);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(22, data);

    assertNotNull(result, "Result should not be null");
    assertInstanceOf(WitOwn.class, result, "Result should be WitOwn");
    assertEquals(typeName, ((WitOwn) result).getResourceType(), "Type name should match");
    assertEquals(123, ((WitOwn) result).getIndex(), "Index should be 123");
  }

  // ==================== Borrow Deserialization Boundary Tests ====================

  @Test
  @DisplayName("Deserialize borrow with exactly 8 bytes (minimum) should work with empty type name")
  void testDeserializeBorrowExactlyMinimumBytes() throws ValidationException {
    // type_length=0, index=99
    final ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(0); // type name length = 0
    buffer.putInt(99); // index = 99
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(23, data);

    assertNotNull(result, "Result should not be null");
    assertInstanceOf(WitBorrow.class, result, "Result should be WitBorrow");
    assertEquals(99, ((WitBorrow) result).getIndex(), "Index should be 99");
  }

  @Test
  @DisplayName("Deserialize borrow with 7 bytes (less than minimum) should fail")
  void testDeserializeBorrowLessThanMinimumBytes() {
    final byte[] data = new byte[7]; // Less than 8 bytes needed

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
    final ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(-1); // negative type name length
    buffer.putInt(42); // index
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
    // type_length=10, but only 3 bytes of type name + index available
    final ByteBuffer buffer = ByteBuffer.allocate(11).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(10); // type name length = 10
    buffer.put("abc".getBytes(StandardCharsets.UTF_8)); // only 3 bytes
    buffer.putInt(42); // index
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
  @DisplayName("Deserialize borrow with valid type name and index")
  void testDeserializeBorrowWithTypeName() throws ValidationException {
    final String typeName = "borrowed_res";
    final byte[] typeNameBytes = typeName.getBytes(StandardCharsets.UTF_8);
    final ByteBuffer buffer =
        ByteBuffer.allocate(4 + typeNameBytes.length + 4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(typeNameBytes.length);
    buffer.put(typeNameBytes);
    buffer.putInt(456);
    final byte[] data = buffer.array();

    final WitValue result = WitValueDeserializer.deserialize(23, data);

    assertNotNull(result, "Result should not be null");
    assertInstanceOf(WitBorrow.class, result, "Result should be WitBorrow");
    assertEquals(typeName, ((WitBorrow) result).getResourceType(), "Type name should match");
    assertEquals(456, ((WitBorrow) result).getIndex(), "Index should be 456");
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
}
