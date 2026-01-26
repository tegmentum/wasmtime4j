/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.wit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WitType;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for WIT (WebAssembly Interface Types) package.
 *
 * <p>This test class validates WIT value types including primitives, composites, and resources.
 */
@DisplayName("WIT Integration Tests")
public class WitIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(WitIntegrationTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting WIT Integration Tests");
  }

  @Nested
  @DisplayName("WitS32 Tests")
  class WitS32Tests {

    @Test
    @DisplayName("Should create WitS32 with of() factory method")
    void shouldCreateWitS32WithOfFactoryMethod() {
      LOGGER.info("Testing WitS32.of() factory method");

      WitS32 value = WitS32.of(42);

      assertNotNull(value, "WitS32 should not be null");
      assertEquals(42, value.getValue(), "Value should be 42");

      LOGGER.info("WitS32.of() factory method verified");
    }

    @Test
    @DisplayName("Should return correct Java representation")
    void shouldReturnCorrectJavaRepresentation() {
      LOGGER.info("Testing WitS32 toJava() method");

      WitS32 value = WitS32.of(100);

      Object javaValue = value.toJava();
      assertNotNull(javaValue, "Java value should not be null");
      assertTrue(javaValue instanceof Integer, "Java value should be Integer");
      assertEquals(100, javaValue, "Java value should be 100");

      LOGGER.info("WitS32 toJava() method verified");
    }

    @Test
    @DisplayName("Should have correct WitType")
    void shouldHaveCorrectWitType() {
      LOGGER.info("Testing WitS32 getType() method");

      WitS32 value = WitS32.of(0);

      WitType type = value.getType();
      assertNotNull(type, "Type should not be null");

      LOGGER.info("WitS32 getType() method verified");
    }

    @Test
    @DisplayName("Should handle minimum and maximum values")
    void shouldHandleMinimumAndMaximumValues() {
      LOGGER.info("Testing WitS32 with boundary values");

      WitS32 minValue = WitS32.of(Integer.MIN_VALUE);
      WitS32 maxValue = WitS32.of(Integer.MAX_VALUE);

      assertEquals(Integer.MIN_VALUE, minValue.getValue(), "Should handle MIN_VALUE");
      assertEquals(Integer.MAX_VALUE, maxValue.getValue(), "Should handle MAX_VALUE");

      LOGGER.info("WitS32 boundary values verified");
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
      LOGGER.info("Testing WitS32 equals() method");

      WitS32 value1 = WitS32.of(42);
      WitS32 value2 = WitS32.of(42);
      WitS32 value3 = WitS32.of(100);

      assertEquals(value1, value2, "Equal values should be equal");
      assertNotEquals(value1, value3, "Different values should not be equal");
      assertEquals(value1, value1, "Same instance should be equal to itself");
      assertNotEquals(value1, null, "Should not be equal to null");
      assertNotEquals(value1, "42", "Should not be equal to different type");

      LOGGER.info("WitS32 equals() method verified");
    }

    @Test
    @DisplayName("Should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
      LOGGER.info("Testing WitS32 hashCode() method");

      WitS32 value1 = WitS32.of(42);
      WitS32 value2 = WitS32.of(42);
      WitS32 value3 = WitS32.of(100);

      assertEquals(value1.hashCode(), value2.hashCode(), "Equal objects should have same hashCode");
      // Note: Different values MAY have same hashCode (not required to be different)

      LOGGER.info("WitS32 hashCode() method verified");
    }

    @Test
    @DisplayName("Should have proper toString representation")
    void shouldHaveProperToStringRepresentation() {
      LOGGER.info("Testing WitS32 toString() method");

      WitS32 value = WitS32.of(42);

      String str = value.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("42"), "toString should contain the value");
      assertTrue(str.contains("WitS32"), "toString should contain type name");

      LOGGER.info("WitS32 toString() method verified: " + str);
    }
  }

  @Nested
  @DisplayName("WitS64 Tests")
  class WitS64Tests {

    @Test
    @DisplayName("Should create WitS64 with of() factory method")
    void shouldCreateWitS64WithOfFactoryMethod() {
      LOGGER.info("Testing WitS64.of() factory method");

      WitS64 value = WitS64.of(100L);

      assertNotNull(value, "WitS64 should not be null");
      assertEquals(100L, value.getValue(), "Value should be 100");

      LOGGER.info("WitS64.of() factory method verified");
    }

    @Test
    @DisplayName("Should handle boundary values")
    void shouldHandleBoundaryValues() {
      LOGGER.info("Testing WitS64 with boundary values");

      WitS64 minValue = WitS64.of(Long.MIN_VALUE);
      WitS64 maxValue = WitS64.of(Long.MAX_VALUE);

      assertEquals(Long.MIN_VALUE, minValue.getValue(), "Should handle MIN_VALUE");
      assertEquals(Long.MAX_VALUE, maxValue.getValue(), "Should handle MAX_VALUE");

      LOGGER.info("WitS64 boundary values verified");
    }

    @Test
    @DisplayName("Should return correct Java representation")
    void shouldReturnCorrectJavaRepresentation() {
      LOGGER.info("Testing WitS64 toJava() method");

      WitS64 value = WitS64.of(Long.MAX_VALUE);

      Object javaValue = value.toJava();
      assertNotNull(javaValue, "Java value should not be null");
      assertTrue(javaValue instanceof Long, "Java value should be Long");
      assertEquals(Long.MAX_VALUE, javaValue, "Java value should match");

      LOGGER.info("WitS64 toJava() method verified");
    }
  }

  @Nested
  @DisplayName("WitBool Tests")
  class WitBoolTests {

    @Test
    @DisplayName("Should create WitBool with true value")
    void shouldCreateWitBoolWithTrueValue() {
      LOGGER.info("Testing WitBool.of(true)");

      WitBool value = WitBool.of(true);

      assertNotNull(value, "WitBool should not be null");
      assertTrue(value.getValue(), "Value should be true");

      LOGGER.info("WitBool true value verified");
    }

    @Test
    @DisplayName("Should create WitBool with false value")
    void shouldCreateWitBoolWithFalseValue() {
      LOGGER.info("Testing WitBool.of(false)");

      WitBool value = WitBool.of(false);

      assertNotNull(value, "WitBool should not be null");
      assertFalse(value.getValue(), "Value should be false");

      LOGGER.info("WitBool false value verified");
    }

    @Test
    @DisplayName("Should return correct Java representation")
    void shouldReturnCorrectJavaRepresentation() {
      LOGGER.info("Testing WitBool toJava() method");

      WitBool trueValue = WitBool.of(true);
      WitBool falseValue = WitBool.of(false);

      assertEquals(Boolean.TRUE, trueValue.toJava(), "True should convert to Boolean.TRUE");
      assertEquals(Boolean.FALSE, falseValue.toJava(), "False should convert to Boolean.FALSE");

      LOGGER.info("WitBool toJava() method verified");
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
      LOGGER.info("Testing WitBool equals() method");

      WitBool true1 = WitBool.of(true);
      WitBool true2 = WitBool.of(true);
      WitBool false1 = WitBool.of(false);

      assertEquals(true1, true2, "True values should be equal");
      assertNotEquals(true1, false1, "True and false should not be equal");

      LOGGER.info("WitBool equals() method verified");
    }
  }

  @Nested
  @DisplayName("WitFloat32 Tests")
  class WitFloat32Tests {

    @Test
    @DisplayName("Should create WitFloat32 with of() factory method")
    void shouldCreateWitFloat32WithOfFactoryMethod() {
      LOGGER.info("Testing WitFloat32.of() factory method");

      WitFloat32 value = WitFloat32.of(3.14f);

      assertNotNull(value, "WitFloat32 should not be null");
      assertEquals(3.14f, value.getValue(), 0.001f, "Value should be approximately 3.14");

      LOGGER.info("WitFloat32.of() factory method verified");
    }

    @Test
    @DisplayName("Should handle special float values")
    void shouldHandleSpecialFloatValues() {
      LOGGER.info("Testing WitFloat32 with special values");

      WitFloat32 posInf = WitFloat32.of(Float.POSITIVE_INFINITY);
      assertEquals(Float.POSITIVE_INFINITY, posInf.getValue(), "Should handle POSITIVE_INFINITY");

      WitFloat32 negInf = WitFloat32.of(Float.NEGATIVE_INFINITY);
      assertEquals(Float.NEGATIVE_INFINITY, negInf.getValue(), "Should handle NEGATIVE_INFINITY");

      WitFloat32 nan = WitFloat32.of(Float.NaN);
      assertTrue(Float.isNaN(nan.getValue()), "Should handle NaN");

      WitFloat32 zero = WitFloat32.of(0.0f);
      assertEquals(0.0f, zero.getValue(), "Should handle zero");

      LOGGER.info("WitFloat32 special values verified");
    }
  }

  @Nested
  @DisplayName("WitFloat64 Tests")
  class WitFloat64Tests {

    @Test
    @DisplayName("Should create WitFloat64 with of() factory method")
    void shouldCreateWitFloat64WithOfFactoryMethod() {
      LOGGER.info("Testing WitFloat64.of() factory method");

      WitFloat64 value = WitFloat64.of(3.14159265358979);

      assertNotNull(value, "WitFloat64 should not be null");
      assertEquals(
          3.14159265358979, value.getValue(), 0.0000001, "Value should be approximately pi");

      LOGGER.info("WitFloat64.of() factory method verified");
    }

    @Test
    @DisplayName("Should handle special double values")
    void shouldHandleSpecialDoubleValues() {
      LOGGER.info("Testing WitFloat64 with special values");

      WitFloat64 posInf = WitFloat64.of(Double.POSITIVE_INFINITY);
      WitFloat64 negInf = WitFloat64.of(Double.NEGATIVE_INFINITY);
      WitFloat64 nan = WitFloat64.of(Double.NaN);

      assertEquals(Double.POSITIVE_INFINITY, posInf.getValue(), "Should handle POSITIVE_INFINITY");
      assertEquals(Double.NEGATIVE_INFINITY, negInf.getValue(), "Should handle NEGATIVE_INFINITY");
      assertTrue(Double.isNaN(nan.getValue()), "Should handle NaN");

      LOGGER.info("WitFloat64 special values verified");
    }
  }

  @Nested
  @DisplayName("WitString Tests")
  class WitStringTests {

    @Test
    @DisplayName("Should create WitString with of() factory method")
    void shouldCreateWitStringWithOfFactoryMethod() throws Exception {
      LOGGER.info("Testing WitString.of() factory method");

      WitString value = WitString.of("Hello, WASM!");

      assertNotNull(value, "WitString should not be null");
      assertEquals("Hello, WASM!", value.getValue(), "Value should match");

      LOGGER.info("WitString.of() factory method verified");
    }

    @Test
    @DisplayName("Should handle empty string")
    void shouldHandleEmptyString() throws Exception {
      LOGGER.info("Testing WitString with empty string");

      WitString value = WitString.of("");

      assertNotNull(value, "WitString should not be null");
      assertEquals("", value.getValue(), "Value should be empty");

      LOGGER.info("WitString empty string verified");
    }

    @Test
    @DisplayName("Should handle unicode characters")
    void shouldHandleUnicodeCharacters() throws Exception {
      LOGGER.info("Testing WitString with unicode characters");

      // Unicode test with Chinese characters and emoji
      String unicode = "Hello 世界! 😀";
      WitString value = WitString.of(unicode);

      assertNotNull(value, "WitString should not be null");
      assertEquals(unicode, value.getValue(), "Unicode value should match");

      LOGGER.info("WitString unicode verified");
    }

    @Test
    @DisplayName("Should return correct Java representation")
    void shouldReturnCorrectJavaRepresentation() throws Exception {
      LOGGER.info("Testing WitString toJava() method");

      WitString value = WitString.of("test");

      Object javaValue = value.toJava();
      assertNotNull(javaValue, "Java value should not be null");
      assertTrue(javaValue instanceof String, "Java value should be String");
      assertEquals("test", javaValue, "Java value should match");

      LOGGER.info("WitString toJava() method verified");
    }
  }

  @Nested
  @DisplayName("WitChar Tests")
  class WitCharTests {

    @Test
    @DisplayName("Should create WitChar with of() factory method")
    void shouldCreateWitCharWithOfFactoryMethod() throws Exception {
      LOGGER.info("Testing WitChar.of() factory method");

      WitChar value = WitChar.of('A');

      assertNotNull(value, "WitChar should not be null");
      assertEquals((int) 'A', value.getCodepoint(), "Codepoint should be 'A'");

      LOGGER.info("WitChar.of() factory method verified");
    }

    @Test
    @DisplayName("Should handle unicode codepoints")
    void shouldHandleUnicodeCodepoints() throws Exception {
      LOGGER.info("Testing WitChar with unicode codepoints");

      // Test with unicode codepoint (Chinese character for 'world')
      int worldCodepoint = 0x4E16;
      WitChar value = WitChar.of(worldCodepoint);

      assertNotNull(value, "WitChar should not be null");
      assertEquals(worldCodepoint, value.getCodepoint(), "Unicode codepoint should match");

      LOGGER.info("WitChar unicode verified");
    }
  }

  @Nested
  @DisplayName("Unsigned Integer Tests")
  class UnsignedIntegerTests {

    @Test
    @DisplayName("Should create WitU8 within valid range")
    void shouldCreateWitU8WithinValidRange() {
      LOGGER.info("Testing WitU8 creation");

      WitU8 zero = WitU8.ofUnsigned(0);
      assertEquals(0, zero.toUnsignedInt(), "Should handle 0");

      WitU8 max = WitU8.ofUnsigned(255);
      assertEquals(255, max.toUnsignedInt(), "Should handle 255");

      WitU8 mid = WitU8.ofUnsigned(128);
      assertEquals(128, mid.toUnsignedInt(), "Should handle 128");

      LOGGER.info("WitU8 values verified");
    }

    @Test
    @DisplayName("Should create WitU16 within valid range")
    void shouldCreateWitU16WithinValidRange() {
      LOGGER.info("Testing WitU16 creation");

      WitU16 zero = WitU16.ofUnsigned(0);
      assertEquals(0, zero.toUnsignedInt(), "Should handle 0");

      WitU16 max = WitU16.ofUnsigned(65535);
      assertEquals(65535, max.toUnsignedInt(), "Should handle 65535");

      WitU16 mid = WitU16.ofUnsigned(32768);
      assertEquals(32768, mid.toUnsignedInt(), "Should handle 32768");

      LOGGER.info("WitU16 values verified");
    }

    @Test
    @DisplayName("Should create WitU32 within valid range")
    void shouldCreateWitU32WithinValidRange() {
      LOGGER.info("Testing WitU32 creation");

      WitU32 zero = WitU32.ofUnsigned(0L);
      assertEquals(0L, zero.toUnsignedLong(), "Should handle 0");

      WitU32 max = WitU32.ofUnsigned(4294967295L);
      assertEquals(4294967295L, max.toUnsignedLong(), "Should handle max U32");

      LOGGER.info("WitU32 values verified");
    }

    @Test
    @DisplayName("Should create WitU64 values")
    void shouldCreateWitU64Values() {
      LOGGER.info("Testing WitU64 creation");

      WitU64 zero = WitU64.of(0L);
      WitU64 positive = WitU64.of(Long.MAX_VALUE);

      assertEquals(0L, zero.getValue(), "Should handle 0");
      assertEquals(Long.MAX_VALUE, positive.getValue(), "Should handle MAX_VALUE");

      LOGGER.info("WitU64 values verified");
    }
  }

  @Nested
  @DisplayName("Signed Integer Tests")
  class SignedIntegerTests {

    @Test
    @DisplayName("Should create WitS8 within valid range")
    void shouldCreateWitS8WithinValidRange() {
      LOGGER.info("Testing WitS8 creation");

      WitS8 min = WitS8.of(Byte.MIN_VALUE);
      WitS8 max = WitS8.of(Byte.MAX_VALUE);
      WitS8 zero = WitS8.of((byte) 0);

      assertEquals(Byte.MIN_VALUE, min.getValue(), "Should handle MIN_VALUE");
      assertEquals(Byte.MAX_VALUE, max.getValue(), "Should handle MAX_VALUE");
      assertEquals(0, zero.getValue(), "Should handle 0");

      LOGGER.info("WitS8 values verified");
    }

    @Test
    @DisplayName("Should create WitS16 within valid range")
    void shouldCreateWitS16WithinValidRange() {
      LOGGER.info("Testing WitS16 creation");

      WitS16 min = WitS16.of(Short.MIN_VALUE);
      WitS16 max = WitS16.of(Short.MAX_VALUE);
      WitS16 zero = WitS16.of((short) 0);

      assertEquals(Short.MIN_VALUE, min.getValue(), "Should handle MIN_VALUE");
      assertEquals(Short.MAX_VALUE, max.getValue(), "Should handle MAX_VALUE");
      assertEquals(0, zero.getValue(), "Should handle 0");

      LOGGER.info("WitS16 values verified");
    }
  }

  @Nested
  @DisplayName("WitValue Abstract Class Tests")
  class WitValueTests {

    @Test
    @DisplayName("Should verify WitValue is abstract")
    void shouldVerifyWitValueIsAbstract() {
      LOGGER.info("Testing WitValue is abstract");

      assertTrue(
          java.lang.reflect.Modifier.isAbstract(WitValue.class.getModifiers()),
          "WitValue should be abstract");

      LOGGER.info("WitValue abstract verified");
    }

    @Test
    @DisplayName("Should verify WitPrimitiveValue extends WitValue")
    void shouldVerifyWitPrimitiveValueExtendsWitValue() {
      LOGGER.info("Testing WitPrimitiveValue extends WitValue");

      assertTrue(
          WitValue.class.isAssignableFrom(WitPrimitiveValue.class),
          "WitPrimitiveValue should extend WitValue");

      LOGGER.info("WitPrimitiveValue inheritance verified");
    }

    @Test
    @DisplayName("Should verify all primitive types extend WitPrimitiveValue")
    void shouldVerifyAllPrimitiveTypesExtendWitPrimitiveValue() {
      LOGGER.info("Testing primitive type inheritance");

      assertTrue(
          WitPrimitiveValue.class.isAssignableFrom(WitS32.class),
          "WitS32 should extend WitPrimitiveValue");
      assertTrue(
          WitPrimitiveValue.class.isAssignableFrom(WitS64.class),
          "WitS64 should extend WitPrimitiveValue");
      assertTrue(
          WitPrimitiveValue.class.isAssignableFrom(WitBool.class),
          "WitBool should extend WitPrimitiveValue");
      assertTrue(
          WitPrimitiveValue.class.isAssignableFrom(WitFloat32.class),
          "WitFloat32 should extend WitPrimitiveValue");
      assertTrue(
          WitPrimitiveValue.class.isAssignableFrom(WitFloat64.class),
          "WitFloat64 should extend WitPrimitiveValue");
      assertTrue(
          WitPrimitiveValue.class.isAssignableFrom(WitString.class),
          "WitString should extend WitPrimitiveValue");
      assertTrue(
          WitPrimitiveValue.class.isAssignableFrom(WitChar.class),
          "WitChar should extend WitPrimitiveValue");

      LOGGER.info("Primitive type inheritance verified");
    }
  }
}
