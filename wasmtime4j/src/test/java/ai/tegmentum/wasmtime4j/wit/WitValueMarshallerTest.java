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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.ValidationException;
import ai.tegmentum.wasmtime4j.wit.WitValueMarshaller.MarshalledValue;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Comprehensive unit tests for WitValueMarshaller. */
@DisplayName("WitValueMarshaller Tests")
final class WitValueMarshallerTest {

  @Test
  @DisplayName("Marshal bool value")
  void testMarshalBool() throws ValidationException {
    final WitBool value = WitBool.of(true);
    final MarshalledValue result = WitValueMarshaller.marshal(value);

    assertNotNull(result, "Marshalled result should not be null");
    assertEquals(1, result.getTypeDiscriminator(), "Type discriminator should be 1 for bool");
    assertEquals(1, result.getDataSize(), "Data size should be 1 byte for bool");
  }

  @Test
  @DisplayName("Marshal s32 value")
  void testMarshalS32() throws ValidationException {
    final WitS32 value = WitS32.of(42);
    final MarshalledValue result = WitValueMarshaller.marshal(value);

    assertNotNull(result, "Marshalled result should not be null");
    assertEquals(2, result.getTypeDiscriminator(), "Type discriminator should be 2 for s32");
    assertEquals(4, result.getDataSize(), "Data size should be 4 bytes for s32");
  }

  @Test
  @DisplayName("Marshal s64 value")
  void testMarshalS64() throws ValidationException {
    final WitS64 value = WitS64.of(1_000_000L);
    final MarshalledValue result = WitValueMarshaller.marshal(value);

    assertNotNull(result, "Marshalled result should not be null");
    assertEquals(3, result.getTypeDiscriminator(), "Type discriminator should be 3 for s64");
    assertEquals(8, result.getDataSize(), "Data size should be 8 bytes for s64");
  }

  @Test
  @DisplayName("Marshal float64 value")
  void testMarshalFloat64() throws ValidationException {
    final WitFloat64 value = WitFloat64.of(3.14159);
    final MarshalledValue result = WitValueMarshaller.marshal(value);

    assertNotNull(result, "Marshalled result should not be null");
    assertEquals(4, result.getTypeDiscriminator(), "Type discriminator should be 4 for float64");
    assertEquals(8, result.getDataSize(), "Data size should be 8 bytes for float64");
  }

  @Test
  @DisplayName("Marshal char value")
  void testMarshalChar() throws ValidationException {
    final WitChar value = WitChar.of('A');
    final MarshalledValue result = WitValueMarshaller.marshal(value);

    assertNotNull(result, "Marshalled result should not be null");
    assertEquals(5, result.getTypeDiscriminator(), "Type discriminator should be 5 for char");
    assertEquals(4, result.getDataSize(), "Data size should be 4 bytes for char");
  }

  @Test
  @DisplayName("Marshal string value")
  void testMarshalString() throws ValidationException {
    final WitString value = WitString.of("hello");
    final MarshalledValue result = WitValueMarshaller.marshal(value);

    assertNotNull(result, "Marshalled result should not be null");
    assertEquals(6, result.getTypeDiscriminator(), "Type discriminator should be 6 for string");
    assertEquals(9, result.getDataSize(), "Data size should be 4 (length) + 5 (UTF-8 bytes)");
  }

  @Test
  @DisplayName("Marshal null value throws exception")
  void testMarshalNull() {
    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueMarshaller.marshal(null),
            "Should throw exception for null value");

    assertTrue(
        exception.getMessage().contains("null"),
        "Exception message should mention null: " + exception.getMessage());
  }

  @Test
  @DisplayName("Unmarshal bool value")
  void testUnmarshalBool() throws ValidationException {
    final byte[] data = {(byte) 1};
    final WitValue result = WitValueMarshaller.unmarshal(1, data);

    assertNotNull(result, "Unmarshalled result should not be null");
    assertInstanceOf(WitBool.class, result, "Result should be WitBool");
    assertTrue(((WitBool) result).getValue(), "Value should be true");
  }

  @Test
  @DisplayName("Unmarshal s32 value")
  void testUnmarshalS32() throws ValidationException {
    final WitS32 value = WitS32.of(-123);
    final byte[] data = WitValueSerializer.serialize(value);
    final WitValue result = WitValueMarshaller.unmarshal(2, data);

    assertNotNull(result, "Unmarshalled result should not be null");
    assertInstanceOf(WitS32.class, result, "Result should be WitS32");
    assertEquals(-123, ((WitS32) result).getValue(), "Value should be -123");
  }

  @Test
  @DisplayName("Unmarshal s64 value")
  void testUnmarshalS64() throws ValidationException {
    final WitS64 value = WitS64.of(9_876_543_210L);
    final byte[] data = WitValueSerializer.serialize(value);
    final WitValue result = WitValueMarshaller.unmarshal(3, data);

    assertNotNull(result, "Unmarshalled result should not be null");
    assertInstanceOf(WitS64.class, result, "Result should be WitS64");
    assertEquals(9_876_543_210L, ((WitS64) result).getValue(), "Value should match original");
  }

  @Test
  @DisplayName("Unmarshal float64 value")
  void testUnmarshalFloat64() throws ValidationException {
    final WitFloat64 value = WitFloat64.of(2.71828);
    final byte[] data = WitValueSerializer.serialize(value);
    final WitValue result = WitValueMarshaller.unmarshal(4, data);

    assertNotNull(result, "Unmarshalled result should not be null");
    assertInstanceOf(WitFloat64.class, result, "Result should be WitFloat64");
    assertEquals(2.71828, ((WitFloat64) result).getValue(), 1e-10, "Value should match original");
  }

  @Test
  @DisplayName("Unmarshal char value")
  void testUnmarshalChar() throws ValidationException {
    final WitChar value = WitChar.of(0x1F980); // 🦀
    final byte[] data = WitValueSerializer.serialize(value);
    final WitValue result = WitValueMarshaller.unmarshal(5, data);

    assertNotNull(result, "Unmarshalled result should not be null");
    assertInstanceOf(WitChar.class, result, "Result should be WitChar");
    assertEquals(0x1F980, ((WitChar) result).getCodepoint(), "Value should be crab emoji");
  }

  @Test
  @DisplayName("Unmarshal string value")
  void testUnmarshalString() throws ValidationException {
    final WitString value = WitString.of("Hello 🦀 World");
    final byte[] data = WitValueSerializer.serialize(value);
    final WitValue result = WitValueMarshaller.unmarshal(6, data);

    assertNotNull(result, "Unmarshalled result should not be null");
    assertInstanceOf(WitString.class, result, "Result should be WitString");
    assertEquals("Hello 🦀 World", ((WitString) result).getValue(), "Value should match original");
  }

  @Test
  @DisplayName("Unmarshal null data throws exception")
  void testUnmarshalNullData() {
    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueMarshaller.unmarshal(1, null),
            "Should throw exception for null data");

    assertTrue(
        exception.getMessage().contains("null"),
        "Exception message should mention null: " + exception.getMessage());
  }

  @Test
  @DisplayName("Round-trip marshal and unmarshal preserves value")
  void testRoundTrip() throws ValidationException {
    final WitValue original = WitS32.of(42);
    final MarshalledValue marshalled = WitValueMarshaller.marshal(original);
    final WitValue unmarshalled =
        WitValueMarshaller.unmarshal(marshalled.getTypeDiscriminator(), marshalled.getData());

    assertInstanceOf(WitS32.class, unmarshalled, "Unmarshalled value should be WitS32");
    assertEquals(42, ((WitS32) unmarshalled).getValue(), "Value should be preserved");
  }

  @Test
  @DisplayName("Marshal all with multiple values")
  void testMarshalAll() throws ValidationException {
    final List<WitValue> values =
        Arrays.asList(WitBool.of(true), WitS32.of(100), WitString.of("test"));

    final List<MarshalledValue> results = WitValueMarshaller.marshalAll(values);

    assertNotNull(results, "Results list should not be null");
    assertEquals(3, results.size(), "Should marshal all 3 values");

    assertEquals(1, results.get(0).getTypeDiscriminator(), "First value should be bool");
    assertEquals(2, results.get(1).getTypeDiscriminator(), "Second value should be s32");
    assertEquals(6, results.get(2).getTypeDiscriminator(), "Third value should be string");
  }

  @Test
  @DisplayName("Marshal all with empty list")
  void testMarshalAllEmpty() throws ValidationException {
    final List<WitValue> values = Arrays.asList();
    final List<MarshalledValue> results = WitValueMarshaller.marshalAll(values);

    assertNotNull(results, "Results list should not be null");
    assertEquals(0, results.size(), "Should return empty list for empty input");
  }

  @Test
  @DisplayName("Marshal all with null list throws exception")
  void testMarshalAllNull() {
    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueMarshaller.marshalAll(null),
            "Should throw exception for null list");

    assertTrue(
        exception.getMessage().contains("null"),
        "Exception message should mention null: " + exception.getMessage());
  }

  @Test
  @DisplayName("Unmarshal all with multiple values")
  void testUnmarshalAll() throws ValidationException {
    final List<WitValue> original =
        Arrays.asList(WitBool.of(false), WitS64.of(-500L), WitFloat64.of(1.5));

    final List<MarshalledValue> marshalled = WitValueMarshaller.marshalAll(original);
    final List<WitValue> unmarshalled = WitValueMarshaller.unmarshalAll(marshalled);

    assertNotNull(unmarshalled, "Unmarshalled list should not be null");
    assertEquals(3, unmarshalled.size(), "Should unmarshal all 3 values");

    assertInstanceOf(WitBool.class, unmarshalled.get(0), "First value should be WitBool");
    assertFalse(((WitBool) unmarshalled.get(0)).getValue(), "First value should be false");

    assertInstanceOf(WitS64.class, unmarshalled.get(1), "Second value should be WitS64");
    assertEquals(-500L, ((WitS64) unmarshalled.get(1)).getValue(), "Second value should be -500");

    assertInstanceOf(WitFloat64.class, unmarshalled.get(2), "Third value should be WitFloat64");
    assertEquals(
        1.5, ((WitFloat64) unmarshalled.get(2)).getValue(), 1e-10, "Third value should be 1.5");
  }

  @Test
  @DisplayName("Unmarshal all with empty list")
  void testUnmarshalAllEmpty() throws ValidationException {
    final List<MarshalledValue> marshalled = Arrays.asList();
    final List<WitValue> unmarshalled = WitValueMarshaller.unmarshalAll(marshalled);

    assertNotNull(unmarshalled, "Unmarshalled list should not be null");
    assertEquals(0, unmarshalled.size(), "Should return empty list for empty input");
  }

  @Test
  @DisplayName("Unmarshal all with null list throws exception")
  void testUnmarshalAllNull() {
    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueMarshaller.unmarshalAll(null),
            "Should throw exception for null list");

    assertTrue(
        exception.getMessage().contains("null"),
        "Exception message should mention null: " + exception.getMessage());
  }

  @Test
  @DisplayName("To Java conversion for primitive values")
  void testToJava() throws ValidationException {
    assertEquals(Boolean.TRUE, WitValueMarshaller.toJava(WitBool.of(true)), "Bool to Boolean");
    assertEquals(Integer.valueOf(42), WitValueMarshaller.toJava(WitS32.of(42)), "S32 to Integer");
    assertEquals(Long.valueOf(100L), WitValueMarshaller.toJava(WitS64.of(100L)), "S64 to Long");
    assertEquals(
        Double.valueOf(3.14), WitValueMarshaller.toJava(WitFloat64.of(3.14)), "Float64 to Double");
    assertEquals(
        Character.valueOf('A'), WitValueMarshaller.toJava(WitChar.of('A')), "Char to Character");
    assertEquals("hello", WitValueMarshaller.toJava(WitString.of("hello")), "String to String");
  }

  @Test
  @DisplayName("To Java with null value returns null")
  void testToJavaNull() {
    assertEquals(null, WitValueMarshaller.toJava(null), "Null value should return null");
  }

  @Test
  @DisplayName("From Java creates bool value")
  void testFromJavaBool() throws ValidationException {
    final WitValue result = WitValueMarshaller.fromJava(Boolean.TRUE, "bool");

    assertInstanceOf(WitBool.class, result, "Result should be WitBool");
    assertTrue(((WitBool) result).getValue(), "Value should be true");
  }

  @Test
  @DisplayName("From Java creates s32 value")
  void testFromJavaS32() throws ValidationException {
    final WitValue result = WitValueMarshaller.fromJava(Integer.valueOf(42), "s32");

    assertInstanceOf(WitS32.class, result, "Result should be WitS32");
    assertEquals(42, ((WitS32) result).getValue(), "Value should be 42");
  }

  @Test
  @DisplayName("From Java creates s64 value")
  void testFromJavaS64() throws ValidationException {
    final WitValue result = WitValueMarshaller.fromJava(Long.valueOf(100L), "s64");

    assertInstanceOf(WitS64.class, result, "Result should be WitS64");
    assertEquals(100L, ((WitS64) result).getValue(), "Value should be 100");
  }

  @Test
  @DisplayName("From Java creates float64 value")
  void testFromJavaFloat64() throws ValidationException {
    final WitValue result = WitValueMarshaller.fromJava(Double.valueOf(2.5), "float64");

    assertInstanceOf(WitFloat64.class, result, "Result should be WitFloat64");
    assertEquals(2.5, ((WitFloat64) result).getValue(), 1e-10, "Value should be 2.5");
  }

  @Test
  @DisplayName("From Java creates char value from Integer")
  void testFromJavaCharFromInteger() throws ValidationException {
    final WitValue result = WitValueMarshaller.fromJava(Integer.valueOf((int) 'Z'), "char");

    assertInstanceOf(WitChar.class, result, "Result should be WitChar");
    assertEquals((int) 'Z', ((WitChar) result).getCodepoint(), "Value should be 'Z'");
  }

  @Test
  @DisplayName("From Java creates char value from Character")
  void testFromJavaCharFromCharacter() throws ValidationException {
    final WitValue result = WitValueMarshaller.fromJava(Character.valueOf('X'), "char");

    assertInstanceOf(WitChar.class, result, "Result should be WitChar");
    assertEquals((int) 'X', ((WitChar) result).getCodepoint(), "Value should be 'X'");
  }

  @Test
  @DisplayName("From Java creates string value")
  void testFromJavaString() throws ValidationException {
    final WitValue result = WitValueMarshaller.fromJava("test", "string");

    assertInstanceOf(WitString.class, result, "Result should be WitString");
    assertEquals("test", ((WitString) result).getValue(), "Value should be 'test'");
  }

  @Test
  @DisplayName("From Java with case-insensitive type name")
  void testFromJavaCaseInsensitive() throws ValidationException {
    final WitValue result1 = WitValueMarshaller.fromJava(Boolean.TRUE, "BOOL");
    final WitValue result2 = WitValueMarshaller.fromJava(Integer.valueOf(1), "S32");
    final WitValue result3 = WitValueMarshaller.fromJava("hi", "STRING");

    assertInstanceOf(WitBool.class, result1, "Should handle uppercase BOOL");
    assertInstanceOf(WitS32.class, result2, "Should handle uppercase S32");
    assertInstanceOf(WitString.class, result3, "Should handle uppercase STRING");
  }

  @Test
  @DisplayName("From Java with null value throws exception")
  void testFromJavaNull() {
    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueMarshaller.fromJava(null, "bool"),
            "Should throw exception for null value");

    assertTrue(
        exception.getMessage().contains("null"),
        "Exception message should mention null: " + exception.getMessage());
  }

  @Test
  @DisplayName("From Java with type mismatch throws exception")
  void testFromJavaTypeMismatch() {
    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueMarshaller.fromJava("not a boolean", "bool"),
            "Should throw exception for type mismatch");

    assertTrue(
        exception.getMessage().contains("Expected Boolean"),
        "Exception message should mention type mismatch: " + exception.getMessage());
  }

  @Test
  @DisplayName("From Java with unsupported type throws exception")
  void testFromJavaUnsupportedType() {
    final ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> WitValueMarshaller.fromJava(Integer.valueOf(1), "unsupported"),
            "Should throw exception for unsupported type");

    assertTrue(
        exception.getMessage().contains("Unsupported WIT type"),
        "Exception message should mention unsupported type: " + exception.getMessage());
  }

  @Test
  @DisplayName("MarshalledValue constructor with null data throws exception")
  void testMarshalledValueNullData() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new MarshalledValue(1, null),
            "Should throw exception for null data");

    assertTrue(
        exception.getMessage().contains("null"),
        "Exception message should mention null: " + exception.getMessage());
  }

  @Test
  @DisplayName("MarshalledValue getData returns defensive copy")
  void testMarshalledValueDefensiveCopy() throws ValidationException {
    final WitBool value = WitBool.of(true);
    final MarshalledValue marshalled = WitValueMarshaller.marshal(value);

    final byte[] data1 = marshalled.getData();
    final byte[] data2 = marshalled.getData();

    assertNotSame(data1, data2, "getData should return different array instances");
    assertArrayEquals(data1, data2, "getData copies should have same content");

    // Modify first copy
    data1[0] = (byte) 99;

    // Second copy should be unaffected
    final byte[] data3 = marshalled.getData();
    assertEquals((byte) 1, data3[0], "Internal data should be protected from modification");
  }

  @Test
  @DisplayName("MarshalledValue toString contains discriminator and size")
  void testMarshalledValueToString() throws ValidationException {
    final WitS32 value = WitS32.of(42);
    final MarshalledValue marshalled = WitValueMarshaller.marshal(value);

    final String str = marshalled.toString();

    assertTrue(str.contains("discriminator=2"), "Should contain discriminator: " + str);
    assertTrue(str.contains("size=4"), "Should contain size: " + str);
  }

  @Test
  @DisplayName("Complex round-trip with all primitive types")
  void testComplexRoundTrip() throws ValidationException {
    final List<WitValue> original =
        Arrays.asList(
            WitBool.of(true),
            WitS32.of(-999),
            WitS64.of(Long.MAX_VALUE),
            WitFloat64.of(Math.PI),
            WitChar.of(0x1F680), // 🚀 rocket emoji
            WitString.of("Hello 世界 🌍"));

    // Marshal all
    final List<MarshalledValue> marshalled = WitValueMarshaller.marshalAll(original);

    // Unmarshal all
    final List<WitValue> unmarshalled = WitValueMarshaller.unmarshalAll(marshalled);

    // Verify all values preserved
    assertEquals(original.size(), unmarshalled.size(), "Should preserve count");

    assertTrue(((WitBool) unmarshalled.get(0)).getValue(), "Bool value preserved");
    assertEquals(-999, ((WitS32) unmarshalled.get(1)).getValue(), "S32 value preserved");
    assertEquals(Long.MAX_VALUE, ((WitS64) unmarshalled.get(2)).getValue(), "S64 value preserved");
    assertEquals(
        Math.PI, ((WitFloat64) unmarshalled.get(3)).getValue(), 1e-10, "Float64 value preserved");
    assertEquals(0x1F680, ((WitChar) unmarshalled.get(4)).getCodepoint(), "Char value preserved");
    assertEquals(
        "Hello 世界 🌍", ((WitString) unmarshalled.get(5)).getValue(), "String value preserved");
  }
}
