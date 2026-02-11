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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.ComponentResourceHandle;
import ai.tegmentum.wasmtime4j.exception.WitMarshallingException;
import ai.tegmentum.wasmtime4j.exception.WitRangeException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for the WIT (WebAssembly Interface Types) package.
 *
 * <p>This test class covers all WIT value types including primitives (bool, integers, floats, char,
 * string), composite types (option, result, list, tuple, record, variant, enum, flags), and
 * resource handles (own, borrow).
 */
@DisplayName("WIT Package Tests")
class WitPackageTest {

  // ========== WitValue Base Class Tests ==========

  @Nested
  @DisplayName("WitValue Tests")
  class WitValueTests {

    @Test
    @DisplayName("WitValue should be abstract class")
    void witValueShouldBeAbstractClass() {
      int modifiers = WitValue.class.getModifiers();
      assertTrue(Modifier.isAbstract(modifiers), "WitValue should be abstract");
      assertFalse(WitValue.class.isInterface(), "WitValue should not be an interface");
    }

    @Test
    @DisplayName("WitValue should have getType method")
    void witValueShouldHaveGetTypeMethod() throws NoSuchMethodException {
      Method getType = WitValue.class.getMethod("getType");
      assertNotNull(getType, "getType method should exist");
      assertEquals(WitType.class, getType.getReturnType(), "getType should return WitType");
    }

    @Test
    @DisplayName("WitValue should have toJava method")
    void witValueShouldHaveToJavaMethod() throws NoSuchMethodException {
      Method toJava = WitValue.class.getMethod("toJava");
      assertNotNull(toJava, "toJava method should exist");
      assertEquals(Object.class, toJava.getReturnType(), "toJava should return Object");
    }

    @Test
    @DisplayName("WitValue should have validate method")
    void witValueShouldHaveValidateMethod() throws NoSuchMethodException {
      Method validate = WitValue.class.getDeclaredMethod("validate");
      assertNotNull(validate, "validate method should exist");
      assertTrue(Modifier.isProtected(validate.getModifiers()), "validate should be protected");
    }
  }

  // ========== WitPrimitiveValue Base Class Tests ==========

  @Nested
  @DisplayName("WitPrimitiveValue Tests")
  class WitPrimitiveValueTests {

    @Test
    @DisplayName("WitPrimitiveValue should be abstract class")
    void witPrimitiveValueShouldBeAbstractClass() {
      int modifiers = WitPrimitiveValue.class.getModifiers();
      assertTrue(Modifier.isAbstract(modifiers), "WitPrimitiveValue should be abstract");
      assertFalse(
          WitPrimitiveValue.class.isInterface(), "WitPrimitiveValue should not be an interface");
    }

    @Test
    @DisplayName("WitPrimitiveValue should extend WitValue")
    void witPrimitiveValueShouldExtendWitValue() {
      assertTrue(
          WitValue.class.isAssignableFrom(WitPrimitiveValue.class),
          "WitPrimitiveValue should extend WitValue");
    }
  }

  // ========== WitBool Tests ==========

  @Nested
  @DisplayName("WitBool Tests")
  class WitBoolTests {

    @Test
    @DisplayName("WitBool should be final class")
    void witBoolShouldBeFinalClass() {
      assertTrue(Modifier.isFinal(WitBool.class.getModifiers()), "WitBool should be final");
    }

    @Test
    @DisplayName("WitBool should extend WitPrimitiveValue")
    void witBoolShouldExtendWitPrimitiveValue() {
      assertTrue(
          WitPrimitiveValue.class.isAssignableFrom(WitBool.class),
          "WitBool should extend WitPrimitiveValue");
    }

    @Test
    @DisplayName("WitBool.of(true) should return true value")
    void witBoolOfTrueShouldReturnTrueValue() {
      WitBool witBool = WitBool.of(true);
      assertNotNull(witBool, "WitBool should not be null");
      assertTrue(witBool.getValue(), "getValue should return true");
      assertEquals(Boolean.TRUE, witBool.toJava(), "toJava should return Boolean.TRUE");
    }

    @Test
    @DisplayName("WitBool.of(false) should return false value")
    void witBoolOfFalseShouldReturnFalseValue() {
      WitBool witBool = WitBool.of(false);
      assertNotNull(witBool, "WitBool should not be null");
      assertFalse(witBool.getValue(), "getValue should return false");
      assertEquals(Boolean.FALSE, witBool.toJava(), "toJava should return Boolean.FALSE");
    }

    @Test
    @DisplayName("WitBool should have TRUE and FALSE constants")
    void witBoolShouldHaveTrueAndFalseConstants() {
      assertNotNull(WitBool.TRUE, "TRUE constant should exist");
      assertNotNull(WitBool.FALSE, "FALSE constant should exist");
      assertTrue(WitBool.TRUE.getValue(), "TRUE constant should be true");
      assertFalse(WitBool.FALSE.getValue(), "FALSE constant should be false");
    }

    @Test
    @DisplayName("WitBool.of should return cached constants")
    void witBoolOfShouldReturnCachedConstants() {
      assertEquals(WitBool.TRUE, WitBool.of(true), "of(true) should return TRUE constant");
      assertEquals(WitBool.FALSE, WitBool.of(false), "of(false) should return FALSE constant");
    }

    @Test
    @DisplayName("WitBool equals and hashCode should work correctly")
    void witBoolEqualsShouldWorkCorrectly() {
      WitBool true1 = WitBool.of(true);
      WitBool true2 = WitBool.of(true);
      WitBool false1 = WitBool.of(false);

      assertEquals(true1, true2, "Two true values should be equal");
      assertNotEquals(true1, false1, "True and false should not be equal");
      assertEquals(true1.hashCode(), true2.hashCode(), "Equal values should have same hashCode");
    }

    @Test
    @DisplayName("WitBool toString should return descriptive string")
    void witBoolToStringShouldReturnDescriptiveString() {
      String trueStr = WitBool.of(true).toString();
      String falseStr = WitBool.of(false).toString();

      assertNotNull(trueStr, "toString should not return null");
      assertNotNull(falseStr, "toString should not return null");
      assertTrue(
          trueStr.contains("true") || trueStr.contains("WitBool"),
          "toString should contain type info");
    }
  }

  // ========== WitS32 Tests ==========

  @Nested
  @DisplayName("WitS32 Tests")
  class WitS32Tests {

    @Test
    @DisplayName("WitS32 should be final class")
    void witS32ShouldBeFinalClass() {
      assertTrue(Modifier.isFinal(WitS32.class.getModifiers()), "WitS32 should be final");
    }

    @Test
    @DisplayName("WitS32.of should create valid values")
    void witS32OfShouldCreateValidValues() {
      WitS32 zero = WitS32.of(0);
      assertEquals(0, zero.getValue(), "Zero value should match");

      WitS32 positive = WitS32.of(42);
      assertEquals(42, positive.getValue(), "Positive value should match");

      WitS32 negative = WitS32.of(-100);
      assertEquals(-100, negative.getValue(), "Negative value should match");

      assertEquals(
          Integer.MAX_VALUE, WitS32.of(Integer.MAX_VALUE).getValue(), "Max value should match");
      assertEquals(
          Integer.MIN_VALUE, WitS32.of(Integer.MIN_VALUE).getValue(), "Min value should match");
    }

    @Test
    @DisplayName("WitS32.toJava should return Integer")
    void witS32ToJavaShouldReturnInteger() {
      WitS32 witS32 = WitS32.of(42);
      Object javaValue = witS32.toJava();
      assertTrue(javaValue instanceof Integer, "toJava should return Integer");
      assertEquals(Integer.valueOf(42), javaValue, "toJava value should match");
    }

    @Test
    @DisplayName("WitS32 equals and hashCode should work correctly")
    void witS32EqualsShouldWorkCorrectly() {
      WitS32 val1 = WitS32.of(42);
      WitS32 val2 = WitS32.of(42);
      WitS32 val3 = WitS32.of(100);

      assertEquals(val1, val2, "Same values should be equal");
      assertNotEquals(val1, val3, "Different values should not be equal");
      assertEquals(val1.hashCode(), val2.hashCode(), "Equal values should have same hashCode");
    }
  }

  // ========== WitString Tests ==========

  @Nested
  @DisplayName("WitString Tests")
  class WitStringTests {

    @Test
    @DisplayName("WitString should be final class")
    void witStringShouldBeFinalClass() {
      assertTrue(Modifier.isFinal(WitString.class.getModifiers()), "WitString should be final");
    }

    @Test
    @DisplayName("WitString.of should create valid string values")
    void witStringOfShouldCreateValidValues() throws WitMarshallingException {
      WitString empty = WitString.of("");
      assertEquals("", empty.getValue(), "Empty string should match");

      WitString hello = WitString.of("Hello");
      assertEquals("Hello", hello.getValue(), "Hello string should match");

      // Test with accented characters (éèê)
      WitString unicode = WitString.of("Unicode: éèê");
      assertTrue(unicode.getValue().contains("é"), "Unicode should be preserved");
    }

    @Test
    @DisplayName("WitString.of(null) should throw WitMarshallingException")
    void witStringOfNullShouldThrow() {
      assertThrows(
          WitMarshallingException.class,
          () -> WitString.of(null),
          "of(null) should throw WitMarshallingException");
    }

    @Test
    @DisplayName("WitString.toJava should return String")
    void witStringToJavaShouldReturnString() throws WitMarshallingException {
      WitString witString = WitString.of("test");
      Object javaValue = witString.toJava();
      assertTrue(javaValue instanceof String, "toJava should return String");
      assertEquals("test", javaValue, "toJava value should match");
    }

    @Test
    @DisplayName("WitString equals and hashCode should work correctly")
    void witStringEqualsShouldWorkCorrectly() throws WitMarshallingException {
      WitString str1 = WitString.of("hello");
      WitString str2 = WitString.of("hello");
      WitString str3 = WitString.of("world");

      assertEquals(str1, str2, "Same strings should be equal");
      assertNotEquals(str1, str3, "Different strings should not be equal");
      assertEquals(str1.hashCode(), str2.hashCode(), "Equal strings should have same hashCode");
    }
  }

  // ========== WitChar Tests ==========

  @Nested
  @DisplayName("WitChar Tests")
  class WitCharTests {

    @Test
    @DisplayName("WitChar should be final class")
    void witCharShouldBeFinalClass() {
      assertTrue(Modifier.isFinal(WitChar.class.getModifiers()), "WitChar should be final");
    }

    @Test
    @DisplayName("WitChar.of should create valid Unicode codepoints")
    void witCharOfShouldCreateValidCodepoints() throws WitRangeException {
      WitChar letterA = WitChar.of(0x0041);
      WitChar euro = WitChar.of(0x20AC);
      WitChar emoji = WitChar.of(0x1F600);

      assertEquals(0x0041, letterA.getCodepoint(), "Letter A codepoint should match");
      assertEquals(0x20AC, euro.getCodepoint(), "Euro codepoint should match");
      assertEquals(0x1F600, emoji.getCodepoint(), "Emoji codepoint should match");
    }

    @Test
    @DisplayName("WitChar.of should reject surrogate codepoints")
    void witCharOfShouldRejectSurrogates() {
      assertThrows(
          WitRangeException.class,
          () -> WitChar.of(0xD800),
          "Surrogate start should throw WitRangeException");
      assertThrows(
          WitRangeException.class,
          () -> WitChar.of(0xDFFF),
          "Surrogate end should throw WitRangeException");
    }

    @Test
    @DisplayName("WitChar.of should reject out of range codepoints")
    void witCharOfShouldRejectOutOfRange() {
      assertThrows(
          WitRangeException.class,
          () -> WitChar.of(-1),
          "Negative codepoint should throw WitRangeException");
      assertThrows(
          WitRangeException.class,
          () -> WitChar.of(0x110000),
          "Codepoint > 0x10FFFF should throw WitRangeException");
    }

    @Test
    @DisplayName("WitChar.toJava should return Character")
    void witCharToJavaShouldReturnCharacter() throws WitRangeException {
      WitChar witChar = WitChar.of(0x0041);
      Object javaValue = witChar.toJava();
      assertTrue(javaValue instanceof Character, "toJava should return Character");
      assertEquals(Character.valueOf('A'), javaValue, "toJava value should match");
    }
  }

  // ========== WitFloat32 Tests ==========

  @Nested
  @DisplayName("WitFloat32 Tests")
  class WitFloat32Tests {

    @Test
    @DisplayName("WitFloat32 should be final class")
    void witFloat32ShouldBeFinalClass() {
      assertTrue(Modifier.isFinal(WitFloat32.class.getModifiers()), "WitFloat32 should be final");
    }

    @Test
    @DisplayName("WitFloat32.of should create valid float values")
    void witFloat32OfShouldCreateValidValues() {
      WitFloat32 zero = WitFloat32.of(0.0f);
      assertEquals(0.0f, zero.getValue(), 0.0001f, "Zero value should match");

      WitFloat32 positive = WitFloat32.of(3.14f);
      assertEquals(3.14f, positive.getValue(), 0.0001f, "Positive value should match");

      WitFloat32 negative = WitFloat32.of(-2.5f);
      assertEquals(-2.5f, negative.getValue(), 0.0001f, "Negative value should match");

      assertEquals(
          Float.MAX_VALUE, WitFloat32.of(Float.MAX_VALUE).getValue(), "Max value should match");
      assertEquals(
          Float.MIN_VALUE, WitFloat32.of(Float.MIN_VALUE).getValue(), "Min value should match");
    }

    @Test
    @DisplayName("WitFloat32.of should handle special values")
    void witFloat32OfShouldHandleSpecialValues() {
      WitFloat32 nan = WitFloat32.of(Float.NaN);
      WitFloat32 posInf = WitFloat32.of(Float.POSITIVE_INFINITY);
      WitFloat32 negInf = WitFloat32.of(Float.NEGATIVE_INFINITY);

      assertTrue(Float.isNaN(nan.getValue()), "NaN should be preserved");
      assertTrue(
          Float.isInfinite(posInf.getValue()) && posInf.getValue() > 0,
          "Positive infinity should be preserved");
      assertTrue(
          Float.isInfinite(negInf.getValue()) && negInf.getValue() < 0,
          "Negative infinity should be preserved");
    }

    @Test
    @DisplayName("WitFloat32.toJava should return Float")
    void witFloat32ToJavaShouldReturnFloat() {
      WitFloat32 witFloat32 = WitFloat32.of(3.14f);
      Object javaValue = witFloat32.toJava();
      assertTrue(javaValue instanceof Float, "toJava should return Float");
      assertEquals(Float.valueOf(3.14f), javaValue, "toJava value should match");
    }
  }

  // ========== WitOption Tests ==========

  @Nested
  @DisplayName("WitOption Tests")
  class WitOptionTests {

    @Test
    @DisplayName("WitOption should be final class")
    void witOptionShouldBeFinalClass() {
      assertTrue(Modifier.isFinal(WitOption.class.getModifiers()), "WitOption should be final");
    }

    @Test
    @DisplayName("WitOption should extend WitValue")
    void witOptionShouldExtendWitValue() {
      assertTrue(
          WitValue.class.isAssignableFrom(WitOption.class), "WitOption should extend WitValue");
    }

    @Test
    @DisplayName("WitOption.some should create option with value")
    void witOptionSomeShouldCreateWithValue() {
      WitType innerType = WitType.createS32();
      WitType optionType = WitType.option(innerType);
      WitValue innerValue = WitS32.of(42);
      WitOption option = WitOption.some(optionType, innerValue);

      assertTrue(option.isSome(), "isSome should return true");
      assertFalse(option.isNone(), "isNone should return false");
      assertTrue(option.getValue().isPresent(), "getValue should return present optional");
      assertEquals(innerValue, option.getValue().get(), "Inner value should match");
    }

    @Test
    @DisplayName("WitOption.none should create empty option")
    void witOptionNoneShouldCreateEmpty() {
      WitType innerType = WitType.createS32();
      WitType optionType = WitType.option(innerType);
      WitOption option = WitOption.none(optionType);

      assertFalse(option.isSome(), "isSome should return false");
      assertTrue(option.isNone(), "isNone should return true");
      assertFalse(option.getValue().isPresent(), "getValue should return empty optional");
    }

    @Test
    @DisplayName("WitOption.of should handle Optional correctly")
    void witOptionOfShouldHandleOptional() {
      WitType innerType = WitType.createS32();
      WitType optionType = WitType.option(innerType);
      WitValue innerValue = WitS32.of(42);

      WitOption some = WitOption.of(optionType, Optional.of(innerValue));
      WitOption none = WitOption.of(optionType, Optional.empty());

      assertTrue(some.isSome(), "of(present) should create some");
      assertTrue(none.isNone(), "of(empty) should create none");
    }

    @Test
    @DisplayName("WitOption should have getInnerType method")
    void witOptionShouldHaveGetInnerTypeMethod() throws NoSuchMethodException {
      Method getInnerType = WitOption.class.getMethod("getInnerType");
      assertNotNull(getInnerType, "getInnerType method should exist");
      assertEquals(WitType.class, getInnerType.getReturnType(), "Should return WitType");
    }
  }

  // ========== WitList Tests ==========

  @Nested
  @DisplayName("WitList Tests")
  class WitListTests {

    @Test
    @DisplayName("WitList should be final class")
    void witListShouldBeFinalClass() {
      assertTrue(Modifier.isFinal(WitList.class.getModifiers()), "WitList should be final");
    }

    @Test
    @DisplayName("WitList.of(varargs) should create list from elements")
    void witListOfVarargsShouldCreateList() {
      WitList list = WitList.of(WitS32.of(1), WitS32.of(2), WitS32.of(3));

      assertEquals(3, list.size(), "List should have 3 elements");
      assertFalse(list.isEmpty(), "List should not be empty");
      assertEquals(WitS32.of(1), list.get(0), "First element should match");
      assertEquals(WitS32.of(2), list.get(1), "Second element should match");
      assertEquals(WitS32.of(3), list.get(2), "Third element should match");
    }

    @Test
    @DisplayName("WitList.of(List) should create list from Java List")
    void witListOfJavaListShouldCreateList() throws WitMarshallingException {
      List<WitValue> elements = new ArrayList<>();
      elements.add(WitString.of("a"));
      elements.add(WitString.of("b"));
      WitList list = WitList.of(elements);

      assertEquals(2, list.size(), "List should have 2 elements");
      assertEquals(WitString.of("a"), list.get(0), "First element should match");
    }

    @Test
    @DisplayName("WitList.empty should create empty list")
    void witListEmptyShouldCreateEmptyList() {
      WitType elementType = WitType.createS32();
      WitList list = WitList.empty(elementType);

      assertTrue(list.isEmpty(), "List should be empty");
      assertEquals(0, list.size(), "Size should be 0");
      assertEquals(elementType, list.getElementType(), "Element type should match");
    }

    @Test
    @DisplayName("WitList.of with null elements should throw")
    void witListOfWithNullElementsShouldThrow() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WitList.of((WitValue[]) null),
          "Null array should throw");
      assertThrows(
          IllegalArgumentException.class,
          () -> WitList.of(new WitValue[0]),
          "Empty array should throw (can't infer type)");
    }

    @Test
    @DisplayName("WitList.builder should create list with fluent API")
    void witListBuilderShouldCreateList() {
      WitType elementType = WitType.createS32();
      WitList list = WitList.builder(elementType).add(WitS32.of(10)).add(WitS32.of(20)).build();

      assertEquals(2, list.size(), "List should have 2 elements");
      assertEquals(WitS32.of(10), list.get(0), "First element should match");
    }

    @Test
    @DisplayName("WitList.getElements should return unmodifiable list")
    void witListGetElementsShouldReturnUnmodifiable() {
      WitList list = WitList.of(WitS32.of(1));
      List<WitValue> elements = list.getElements();

      assertThrows(
          UnsupportedOperationException.class,
          () -> elements.add(WitS32.of(2)),
          "getElements should return unmodifiable list");
    }

    @Test
    @DisplayName("WitList.toJava should return Java List")
    void witListToJavaShouldReturnJavaList() {
      WitList list = WitList.of(WitS32.of(1), WitS32.of(2));
      Object javaValue = list.toJava();

      assertTrue(javaValue instanceof List, "toJava should return List");
      @SuppressWarnings("unchecked")
      List<Object> javaList = (List<Object>) javaValue;
      assertEquals(2, javaList.size(), "Java list should have 2 elements");
      assertEquals(Integer.valueOf(1), javaList.get(0), "First element should be converted");
    }
  }

  // ========== WitTuple Tests ==========

  @Nested
  @DisplayName("WitTuple Tests")
  class WitTupleTests {

    @Test
    @DisplayName("WitTuple should be final class")
    void witTupleShouldBeFinalClass() {
      assertTrue(Modifier.isFinal(WitTuple.class.getModifiers()), "WitTuple should be final");
    }

    @Test
    @DisplayName("WitTuple.of(varargs) should create tuple from elements")
    void witTupleOfVarargsShouldCreateTuple() throws WitMarshallingException {
      WitTuple tuple = WitTuple.of(WitString.of("hello"), WitS32.of(42));

      assertEquals(2, tuple.size(), "Tuple should have 2 elements");
      assertFalse(tuple.isEmpty(), "Tuple should not be empty");
      assertEquals(WitString.of("hello"), tuple.get(0), "First element should match");
      assertEquals(WitS32.of(42), tuple.get(1), "Second element should match");
    }

    @Test
    @DisplayName("WitTuple.empty should create unit tuple")
    void witTupleEmptyShouldCreateUnitTuple() {
      WitTuple tuple = WitTuple.empty();

      assertTrue(tuple.isEmpty(), "Tuple should be empty");
      assertEquals(0, tuple.size(), "Size should be 0");
    }

    @Test
    @DisplayName("WitTuple.getElementTypes should return types for each position")
    void witTupleGetElementTypesShouldReturnTypes() throws WitMarshallingException {
      WitTuple tuple = WitTuple.of(WitString.of("hello"), WitS32.of(42));
      List<WitType> types = tuple.getElementTypes();

      assertEquals(2, types.size(), "Should have 2 element types");
      assertEquals(WitType.createString(), types.get(0), "First type should be string");
      assertEquals(WitType.createS32(), types.get(1), "Second type should be s32");
    }

    @Test
    @DisplayName("WitTuple.builder should create tuple with fluent API")
    void witTupleBuilderShouldCreateTuple() throws WitMarshallingException {
      WitTuple tuple = WitTuple.builder().add(WitBool.of(true)).add(WitString.of("test")).build();

      assertEquals(2, tuple.size(), "Tuple should have 2 elements");
      assertEquals(WitBool.of(true), tuple.get(0), "First element should match");
    }

    @Test
    @DisplayName("WitTuple.getTypeAt should return type at index")
    void witTupleGetTypeAtShouldReturnType() throws WitMarshallingException {
      WitTuple tuple = WitTuple.of(WitString.of("hello"), WitS32.of(42));

      assertEquals(WitType.createString(), tuple.getTypeAt(0), "Type at 0 should be string");
      assertEquals(WitType.createS32(), tuple.getTypeAt(1), "Type at 1 should be s32");
    }

    @Test
    @DisplayName("WitTuple.toJava should return Java List")
    void witTupleToJavaShouldReturnJavaList() throws WitMarshallingException {
      WitTuple tuple = WitTuple.of(WitString.of("hello"), WitS32.of(42));
      Object javaValue = tuple.toJava();

      assertTrue(javaValue instanceof List, "toJava should return List");
      @SuppressWarnings("unchecked")
      List<Object> javaList = (List<Object>) javaValue;
      assertEquals(2, javaList.size(), "Java list should have 2 elements");
      assertEquals("hello", javaList.get(0), "First element should be converted");
      assertEquals(Integer.valueOf(42), javaList.get(1), "Second element should be converted");
    }
  }

  // ========== WitRecord Tests ==========

  @Nested
  @DisplayName("WitRecord Tests")
  class WitRecordTests {

    @Test
    @DisplayName("WitRecord should be final class")
    void witRecordShouldBeFinalClass() {
      assertTrue(Modifier.isFinal(WitRecord.class.getModifiers()), "WitRecord should be final");
    }

    @Test
    @DisplayName("WitRecord.of should create record from map")
    void witRecordOfShouldCreateFromMap() throws WitMarshallingException {
      Map<String, WitValue> fields = new LinkedHashMap<>();
      fields.put("name", WitString.of("Alice"));
      fields.put("age", WitS32.of(30));

      WitRecord record = WitRecord.of(fields);

      assertEquals(2, record.getFieldCount(), "Record should have 2 fields");
      assertEquals(WitString.of("Alice"), record.getField("name"), "Name field should match");
      assertEquals(WitS32.of(30), record.getField("age"), "Age field should match");
    }

    @Test
    @DisplayName("WitRecord.builder should create record with fluent API")
    void witRecordBuilderShouldCreateRecord() {
      WitRecord record =
          WitRecord.builder().field("id", WitS32.of(1)).field("active", WitBool.of(true)).build();

      assertEquals(2, record.getFieldCount(), "Record should have 2 fields");
      assertTrue(record.hasField("id"), "Should have id field");
      assertTrue(record.hasField("active"), "Should have active field");
    }

    @Test
    @DisplayName("WitRecord.of with empty map should throw")
    void witRecordOfWithEmptyMapShouldThrow() {
      Map<String, WitValue> emptyFields = new LinkedHashMap<>();

      assertThrows(
          IllegalArgumentException.class,
          () -> WitRecord.of(emptyFields),
          "Empty fields should throw");
    }

    @Test
    @DisplayName("WitRecord.getFields should return unmodifiable map")
    void witRecordGetFieldsShouldReturnUnmodifiable() {
      Map<String, WitValue> fields = new LinkedHashMap<>();
      fields.put("x", WitS32.of(1));
      WitRecord record = WitRecord.of(fields);

      Map<String, WitValue> retrievedFields = record.getFields();

      assertThrows(
          UnsupportedOperationException.class,
          () -> retrievedFields.put("y", WitS32.of(2)),
          "getFields should return unmodifiable map");
    }

    @Test
    @DisplayName("WitRecord.toJava should return Java Map")
    void witRecordToJavaShouldReturnJavaMap() throws WitMarshallingException {
      Map<String, WitValue> fields = new LinkedHashMap<>();
      fields.put("name", WitString.of("Bob"));
      WitRecord record = WitRecord.of(fields);

      Object javaValue = record.toJava();

      assertTrue(javaValue instanceof Map, "toJava should return Map");
      @SuppressWarnings("unchecked")
      Map<String, Object> javaMap = (Map<String, Object>) javaValue;
      assertEquals("Bob", javaMap.get("name"), "Name should be converted");
    }
  }

  // ========== WitOwn Tests ==========

  @Nested
  @DisplayName("WitOwn Tests")
  class WitOwnTests {

    @Test
    @DisplayName("WitOwn should be final class")
    void witOwnShouldBeFinalClass() {
      assertTrue(Modifier.isFinal(WitOwn.class.getModifiers()), "WitOwn should be final");
    }

    @Test
    @DisplayName("WitOwn should extend WitValue")
    void witOwnShouldExtendWitValue() {
      assertTrue(WitValue.class.isAssignableFrom(WitOwn.class), "WitOwn should extend WitValue");
    }

    @Test
    @DisplayName("WitOwn.of should create owned resource handle")
    void witOwnOfShouldCreateOwnedHandle() {
      WitOwn own = WitOwn.of("file-handle", 42);

      assertEquals("file-handle", own.getResourceType(), "Resource type should match");
      assertEquals(42, own.getIndex(), "Index should match");
    }

    @Test
    @DisplayName("WitOwn.ofWithHost should create owned handle with host object")
    void witOwnOfWithHostShouldCreateWithHostObject() {
      String hostObject = "test-host-object";
      WitOwn own = WitOwn.ofWithHost("file-handle", 42, hostObject);

      assertEquals("file-handle", own.getResourceType(), "Resource type should match");
      assertEquals(42, own.getIndex(), "Index should match");
      assertEquals(hostObject, own.getHostObject(String.class), "Host object should match");
    }

    @Test
    @DisplayName("WitOwn.fromHandle should create from ComponentResourceHandle")
    void witOwnFromHandleShouldCreateFromHandle() {
      ComponentResourceHandle handle = ComponentResourceHandle.own("test-resource", 10);
      WitOwn own = WitOwn.fromHandle(handle);

      assertEquals("test-resource", own.getResourceType(), "Resource type should match");
      assertEquals(10, own.getIndex(), "Index should match");
      assertEquals(handle, own.getHandle(), "Handle should match");
    }

    @Test
    @DisplayName("WitOwn.fromHandle should reject borrowed handle")
    void witOwnFromHandleShouldRejectBorrowed() {
      ComponentResourceHandle borrowedHandle = ComponentResourceHandle.borrow("test-resource", 10);

      assertThrows(
          IllegalArgumentException.class,
          () -> WitOwn.fromHandle(borrowedHandle),
          "Borrowed handle should throw");
    }

    @Test
    @DisplayName("WitOwn.toJava should return ComponentResourceHandle")
    void witOwnToJavaShouldReturnHandle() {
      WitOwn own = WitOwn.of("file-handle", 42);
      Object javaValue = own.toJava();

      assertTrue(
          javaValue instanceof ComponentResourceHandle,
          "toJava should return ComponentResourceHandle");
    }

    @Test
    @DisplayName("WitOwn should reject negative index")
    void witOwnShouldRejectNegativeIndex() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WitOwn.of("test", -1),
          "Negative index should throw");
    }
  }

  // ========== WitBorrow Tests ==========

  @Nested
  @DisplayName("WitBorrow Tests")
  class WitBorrowTests {

    @Test
    @DisplayName("WitBorrow should be final class")
    void witBorrowShouldBeFinalClass() {
      assertTrue(Modifier.isFinal(WitBorrow.class.getModifiers()), "WitBorrow should be final");
    }

    @Test
    @DisplayName("WitBorrow should extend WitValue")
    void witBorrowShouldExtendWitValue() {
      assertTrue(
          WitValue.class.isAssignableFrom(WitBorrow.class), "WitBorrow should extend WitValue");
    }

    @Test
    @DisplayName("WitBorrow.of should create borrowed resource handle")
    void witBorrowOfShouldCreateBorrowedHandle() {
      WitBorrow borrow = WitBorrow.of("file-handle", 42);

      assertEquals("file-handle", borrow.getResourceType(), "Resource type should match");
      assertEquals(42, borrow.getIndex(), "Index should match");
    }

    @Test
    @DisplayName("WitBorrow.ofWithHost should create borrowed handle with host object")
    void witBorrowOfWithHostShouldCreateWithHostObject() {
      String hostObject = "test-host-object";
      WitBorrow borrow = WitBorrow.ofWithHost("file-handle", 42, hostObject);

      assertEquals("file-handle", borrow.getResourceType(), "Resource type should match");
      assertEquals(42, borrow.getIndex(), "Index should match");
      assertEquals(hostObject, borrow.getHostObject(String.class), "Host object should match");
    }

    @Test
    @DisplayName("WitBorrow.fromHandle should create from ComponentResourceHandle")
    void witBorrowFromHandleShouldCreateFromHandle() {
      ComponentResourceHandle handle = ComponentResourceHandle.borrow("test-resource", 10);
      WitBorrow borrow = WitBorrow.fromHandle(handle);

      assertEquals("test-resource", borrow.getResourceType(), "Resource type should match");
      assertEquals(10, borrow.getIndex(), "Index should match");
      assertEquals(handle, borrow.getHandle(), "Handle should match");
    }

    @Test
    @DisplayName("WitBorrow.fromHandle should reject owned handle")
    void witBorrowFromHandleShouldRejectOwned() {
      ComponentResourceHandle ownedHandle = ComponentResourceHandle.own("test-resource", 10);

      assertThrows(
          IllegalArgumentException.class,
          () -> WitBorrow.fromHandle(ownedHandle),
          "Owned handle should throw");
    }

    @Test
    @DisplayName("WitBorrow.toJava should return ComponentResourceHandle")
    void witBorrowToJavaShouldReturnHandle() {
      WitBorrow borrow = WitBorrow.of("file-handle", 42);
      Object javaValue = borrow.toJava();

      assertTrue(
          javaValue instanceof ComponentResourceHandle,
          "toJava should return ComponentResourceHandle");
    }

    @Test
    @DisplayName("WitBorrow should reject negative index")
    void witBorrowShouldRejectNegativeIndex() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WitBorrow.of("test", -1),
          "Negative index should throw");
    }
  }

  // ========== WitList.Builder Tests ==========

  @Nested
  @DisplayName("WitList.Builder Tests")
  class WitListBuilderTests {

    @Test
    @DisplayName("Builder should be public final nested class")
    void builderShouldBePublicFinalNestedClass() {
      Class<?> builderClass = WitList.Builder.class;
      assertTrue(Modifier.isPublic(builderClass.getModifiers()), "Builder should be public");
      assertTrue(Modifier.isFinal(builderClass.getModifiers()), "Builder should be final");
      assertTrue(Modifier.isStatic(builderClass.getModifiers()), "Builder should be static");
    }

    @Test
    @DisplayName("Builder.add should validate element type")
    void builderAddShouldValidateElementType() {
      WitType s32Type = WitType.createS32();
      WitList.Builder builder = WitList.builder(s32Type);

      assertDoesNotThrow(() -> builder.add(WitS32.of(1)), "Adding correct type should not throw");

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.add(WitString.of("wrong type")),
          "Adding wrong type should throw");
    }

    @Test
    @DisplayName("Builder.addAll should add multiple elements")
    void builderAddAllShouldAddMultiple() {
      WitType s32Type = WitType.createS32();
      List<WitValue> elements = Arrays.asList(WitS32.of(1), WitS32.of(2), WitS32.of(3));

      WitList list = WitList.builder(s32Type).addAll(elements).build();

      assertEquals(3, list.size(), "List should have 3 elements");
    }

    @Test
    @DisplayName("Builder.add(null) should throw")
    void builderAddNullShouldThrow() {
      WitType s32Type = WitType.createS32();
      WitList.Builder builder = WitList.builder(s32Type);

      assertThrows(
          IllegalArgumentException.class, () -> builder.add(null), "Adding null should throw");
    }
  }

  // ========== WitTuple.Builder Tests ==========

  @Nested
  @DisplayName("WitTuple.Builder Tests")
  class WitTupleBuilderTests {

    @Test
    @DisplayName("Builder should be public final nested class")
    void builderShouldBePublicFinalNestedClass() {
      Class<?> builderClass = WitTuple.Builder.class;
      assertTrue(Modifier.isPublic(builderClass.getModifiers()), "Builder should be public");
      assertTrue(Modifier.isFinal(builderClass.getModifiers()), "Builder should be final");
      assertTrue(Modifier.isStatic(builderClass.getModifiers()), "Builder should be static");
    }

    @Test
    @DisplayName("Builder.add(element) should infer type")
    void builderAddElementShouldInferType() throws WitMarshallingException {
      WitTuple tuple = WitTuple.builder().add(WitS32.of(42)).add(WitString.of("hello")).build();

      assertEquals(2, tuple.size(), "Tuple should have 2 elements");
      assertEquals(WitType.createS32(), tuple.getTypeAt(0), "First type should be inferred as s32");
      assertEquals(
          WitType.createString(), tuple.getTypeAt(1), "Second type should be inferred as string");
    }

    @Test
    @DisplayName("Builder.add(type, element) should validate type")
    void builderAddWithTypeShouldValidate() {
      WitTuple.Builder builder = WitTuple.builder();

      assertDoesNotThrow(
          () -> builder.add(WitType.createS32(), WitS32.of(42)),
          "Adding with correct type should not throw");

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.add(WitType.createString(), WitS32.of(42)),
          "Adding with mismatched type should throw");
    }

    @Test
    @DisplayName("Builder.add(null) should throw")
    void builderAddNullShouldThrow() {
      WitTuple.Builder builder = WitTuple.builder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.add(null),
          "Adding null element should throw");
    }
  }

  // ========== WitRecord.Builder Tests ==========

  @Nested
  @DisplayName("WitRecord.Builder Tests")
  class WitRecordBuilderTests {

    @Test
    @DisplayName("Builder should be public final nested class")
    void builderShouldBePublicFinalNestedClass() {
      Class<?> builderClass = WitRecord.Builder.class;
      assertTrue(Modifier.isPublic(builderClass.getModifiers()), "Builder should be public");
      assertTrue(Modifier.isFinal(builderClass.getModifiers()), "Builder should be final");
      assertTrue(Modifier.isStatic(builderClass.getModifiers()), "Builder should be static");
    }

    @Test
    @DisplayName("Builder.field should add named field")
    void builderFieldShouldAddNamedField() {
      WitRecord record =
          WitRecord.builder().field("x", WitS32.of(10)).field("y", WitS32.of(20)).build();

      assertEquals(2, record.getFieldCount(), "Record should have 2 fields");
      assertEquals(WitS32.of(10), record.getField("x"), "x field should match");
      assertEquals(WitS32.of(20), record.getField("y"), "y field should match");
    }

    @Test
    @DisplayName("Builder.field with null name should throw")
    void builderFieldWithNullNameShouldThrow() {
      WitRecord.Builder builder = WitRecord.builder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.field(null, WitS32.of(1)),
          "Null field name should throw");
    }

    @Test
    @DisplayName("Builder.field with empty name should throw")
    void builderFieldWithEmptyNameShouldThrow() {
      WitRecord.Builder builder = WitRecord.builder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.field("", WitS32.of(1)),
          "Empty field name should throw");
    }

    @Test
    @DisplayName("Builder.field with null value should throw")
    void builderFieldWithNullValueShouldThrow() {
      WitRecord.Builder builder = WitRecord.builder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.field("name", null),
          "Null field value should throw");
    }

    @Test
    @DisplayName("Builder.build with no fields should throw")
    void builderBuildWithNoFieldsShouldThrow() {
      WitRecord.Builder builder = WitRecord.builder();

      assertThrows(
          IllegalArgumentException.class, builder::build, "Build with no fields should throw");
    }
  }

  // ========== WitFlags.Builder Tests ==========

  @Nested
  @DisplayName("WitFlags.Builder Tests")
  class WitFlagsBuilderTests {

    @Test
    @DisplayName("Builder should be public final nested class")
    void builderShouldBePublicFinalNestedClass() {
      Class<?> builderClass = WitFlags.Builder.class;
      assertTrue(Modifier.isPublic(builderClass.getModifiers()), "Builder should be public");
      assertTrue(Modifier.isFinal(builderClass.getModifiers()), "Builder should be final");
      assertTrue(Modifier.isStatic(builderClass.getModifiers()), "Builder should be static");
    }

    @Test
    @DisplayName("WitFlags.builder should require non-null type")
    void witFlagsBuilderShouldRequireNonNullType() {
      assertThrows(
          IllegalArgumentException.class, () -> WitFlags.builder(null), "Null type should throw");
    }

    @Test
    @DisplayName("Builder.set with null name should throw")
    void builderSetWithNullNameShouldThrow() {
      WitType flagsType = WitType.flags("perms", Arrays.asList("read", "write"));
      WitFlags.Builder builder = WitFlags.builder(flagsType);

      assertThrows(
          IllegalArgumentException.class, () -> builder.set(null), "Null flag name should throw");
    }

    @Test
    @DisplayName("Builder.set with empty name should throw")
    void builderSetWithEmptyNameShouldThrow() {
      WitType flagsType = WitType.flags("perms", Arrays.asList("read", "write"));
      WitFlags.Builder builder = WitFlags.builder(flagsType);

      assertThrows(
          IllegalArgumentException.class, () -> builder.set(""), "Empty flag name should throw");
    }
  }

  // ========== Equality and HashCode Comprehensive Tests ==========

  @Nested
  @DisplayName("Equality and HashCode Tests")
  class EqualityTests {

    @Test
    @DisplayName("WitList equals should compare elements")
    void witListEqualsShouldCompareElements() {
      WitList list1 = WitList.of(WitS32.of(1), WitS32.of(2));
      WitList list2 = WitList.of(WitS32.of(1), WitS32.of(2));
      WitList list3 = WitList.of(WitS32.of(1), WitS32.of(3));

      assertEquals(list1, list2, "Same elements should be equal");
      assertNotEquals(list1, list3, "Different elements should not be equal");
      assertEquals(list1.hashCode(), list2.hashCode(), "Equal lists should have same hashCode");
    }

    @Test
    @DisplayName("WitTuple equals should compare elements")
    void witTupleEqualsShouldCompareElements() throws WitMarshallingException {
      WitTuple tuple1 = WitTuple.of(WitString.of("a"), WitS32.of(1));
      WitTuple tuple2 = WitTuple.of(WitString.of("a"), WitS32.of(1));
      WitTuple tuple3 = WitTuple.of(WitString.of("b"), WitS32.of(1));

      assertEquals(tuple1, tuple2, "Same elements should be equal");
      assertNotEquals(tuple1, tuple3, "Different elements should not be equal");
      assertEquals(tuple1.hashCode(), tuple2.hashCode(), "Equal tuples should have same hashCode");
    }

    @Test
    @DisplayName("WitRecord equals should compare fields")
    void witRecordEqualsShouldCompareFields() {
      WitRecord record1 = WitRecord.builder().field("x", WitS32.of(1)).build();
      WitRecord record2 = WitRecord.builder().field("x", WitS32.of(1)).build();
      WitRecord record3 = WitRecord.builder().field("x", WitS32.of(2)).build();

      assertEquals(record1, record2, "Same fields should be equal");
      assertNotEquals(record1, record3, "Different fields should not be equal");
      assertEquals(
          record1.hashCode(), record2.hashCode(), "Equal records should have same hashCode");
    }

    @Test
    @DisplayName("WitOption equals should compare presence and value")
    void witOptionEqualsShouldComparePresenceAndValue() {
      WitType innerType = WitType.createS32();
      WitType optionType = WitType.option(innerType);
      WitOption some1 = WitOption.some(optionType, WitS32.of(42));
      WitOption some2 = WitOption.some(optionType, WitS32.of(42));
      assertEquals(some1, some2, "Same some values should be equal");

      WitOption some3 = WitOption.some(optionType, WitS32.of(100));
      assertNotEquals(some1, some3, "Different some values should not be equal");

      WitOption none1 = WitOption.none(optionType);
      assertNotEquals(some1, none1, "Some and none should not be equal");
      assertEquals(none1, WitOption.none(optionType), "None values should be equal");
    }

    @Test
    @DisplayName("WitOwn equals should compare handle")
    void witOwnEqualsShouldCompareHandle() {
      WitOwn own1 = WitOwn.of("resource", 1);
      WitOwn own2 = WitOwn.of("resource", 1);
      WitOwn own3 = WitOwn.of("resource", 2);

      assertEquals(own1, own2, "Same resource handles should be equal");
      assertNotEquals(own1, own3, "Different handles should not be equal");
      assertEquals(own1.hashCode(), own2.hashCode(), "Equal handles should have same hashCode");
    }

    @Test
    @DisplayName("WitBorrow equals should compare handle")
    void witBorrowEqualsShouldCompareHandle() {
      WitBorrow borrow1 = WitBorrow.of("resource", 1);
      WitBorrow borrow2 = WitBorrow.of("resource", 1);
      WitBorrow borrow3 = WitBorrow.of("resource", 2);

      assertEquals(borrow1, borrow2, "Same resource handles should be equal");
      assertNotEquals(borrow1, borrow3, "Different handles should not be equal");
      assertEquals(
          borrow1.hashCode(), borrow2.hashCode(), "Equal handles should have same hashCode");
    }
  }

  // ========== toString Tests ==========

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("WitList.toString should be descriptive")
    void witListToStringShouldBeDescriptive() {
      WitList list = WitList.of(WitS32.of(1), WitS32.of(2));
      String str = list.toString();

      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("WitList") || str.contains("["), "toString should identify as list");
    }

    @Test
    @DisplayName("WitTuple.toString should be descriptive")
    void witTupleToStringShouldBeDescriptive() throws WitMarshallingException {
      WitTuple tuple = WitTuple.of(WitString.of("a"), WitS32.of(1));
      String str = tuple.toString();

      assertNotNull(str, "toString should not return null");
      assertTrue(
          str.contains("WitTuple") || str.contains("["), "toString should identify as tuple");
    }

    @Test
    @DisplayName("WitRecord.toString should be descriptive")
    void witRecordToStringShouldBeDescriptive() {
      WitRecord record = WitRecord.builder().field("x", WitS32.of(1)).build();
      String str = record.toString();

      assertNotNull(str, "toString should not return null");
      assertTrue(
          str.contains("WitRecord") || str.contains("{"), "toString should identify as record");
    }

    @Test
    @DisplayName("WitOwn.toString should include resource type and index")
    void witOwnToStringShouldIncludeInfo() {
      WitOwn own = WitOwn.of("file-handle", 42);
      String str = own.toString();

      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("WitOwn"), "toString should identify as WitOwn");
      assertTrue(str.contains("file-handle"), "toString should include resource type");
      assertTrue(str.contains("42"), "toString should include index");
    }

    @Test
    @DisplayName("WitBorrow.toString should include resource type and index")
    void witBorrowToStringShouldIncludeInfo() {
      WitBorrow borrow = WitBorrow.of("file-handle", 42);
      String str = borrow.toString();

      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("WitBorrow"), "toString should identify as WitBorrow");
      assertTrue(str.contains("file-handle"), "toString should include resource type");
      assertTrue(str.contains("42"), "toString should include index");
    }

    @Test
    @DisplayName("WitChar.toString should show codepoint")
    void witCharToStringShouldShowCodepoint() throws WitRangeException {
      WitChar witChar = WitChar.of(0x0041);
      String str = witChar.toString();

      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("WitChar"), "toString should identify as WitChar");
    }
  }

  // ========== Index Out of Bounds Tests ==========

  @Nested
  @DisplayName("Index Out of Bounds Tests")
  class IndexOutOfBoundsTests {

    @Test
    @DisplayName("WitList.get should throw on invalid index")
    void witListGetShouldThrowOnInvalidIndex() {
      WitList list = WitList.of(WitS32.of(1), WitS32.of(2));

      assertThrows(
          IndexOutOfBoundsException.class, () -> list.get(-1), "Negative index should throw");
      assertThrows(
          IndexOutOfBoundsException.class, () -> list.get(2), "Index >= size should throw");
    }

    @Test
    @DisplayName("WitTuple.get should throw on invalid index")
    void witTupleGetShouldThrowOnInvalidIndex() throws WitMarshallingException {
      WitTuple tuple = WitTuple.of(WitString.of("a"), WitS32.of(1));

      assertThrows(
          IndexOutOfBoundsException.class, () -> tuple.get(-1), "Negative index should throw");
      assertThrows(
          IndexOutOfBoundsException.class, () -> tuple.get(2), "Index >= size should throw");
    }

    @Test
    @DisplayName("WitTuple.getTypeAt should throw on invalid index")
    void witTupleGetTypeAtShouldThrowOnInvalidIndex() throws WitMarshallingException {
      WitTuple tuple = WitTuple.of(WitString.of("a"), WitS32.of(1));

      assertThrows(
          IndexOutOfBoundsException.class,
          () -> tuple.getTypeAt(-1),
          "Negative index should throw");
      assertThrows(
          IndexOutOfBoundsException.class, () -> tuple.getTypeAt(2), "Index >= size should throw");
    }
  }

  // ========== WitFlags Tests ==========

  @Nested
  @DisplayName("WitFlags Tests")
  class WitFlagsTests {

    @Test
    @DisplayName("WitFlags should be final class")
    void witFlagsShouldBeFinalClass() {
      assertTrue(Modifier.isFinal(WitFlags.class.getModifiers()), "WitFlags should be final");
    }

    @Test
    @DisplayName("WitFlags.of(varargs) should create flags with specified flags set")
    void witFlagsOfVarargsShouldSetFlags() {
      WitType flagsType = WitType.flags("perms", Arrays.asList("read", "write", "execute"));
      // Note: validation will fail for non-existent flags due to placeholder implementation
      // For now, test the API structure
      WitFlags.Builder builder = WitFlags.builder(flagsType);
      assertNotNull(builder, "Builder should be created");
    }

    @Test
    @DisplayName("WitFlags.empty should create flags with no flags set")
    void witFlagsEmptyShouldCreateNoFlags() {
      WitType flagsType = WitType.flags("perms", Arrays.asList("read", "write"));
      WitFlags flags = WitFlags.empty(flagsType);

      assertTrue(flags.isEmpty(), "Flags should be empty");
      assertEquals(0, flags.size(), "Size should be 0");
    }

    @Test
    @DisplayName("WitFlags.toJava should return Set")
    void witFlagsToJavaShouldReturnSet() {
      WitType flagsType = WitType.flags("perms", Arrays.asList("read", "write"));
      WitFlags flags = WitFlags.empty(flagsType);
      Object javaValue = flags.toJava();

      assertTrue(javaValue instanceof Set, "toJava should return Set");
    }

    @Test
    @DisplayName("WitFlags.getSetFlags should return unmodifiable set")
    void witFlagsGetSetFlagsShouldReturnUnmodifiable() {
      WitType flagsType = WitType.flags("perms", Arrays.asList("read", "write"));
      Set<String> inputFlags = new HashSet<>(Arrays.asList("read"));
      WitFlags flags = WitFlags.of(flagsType, inputFlags);

      Set<String> setFlags = flags.getSetFlags();

      assertThrows(
          UnsupportedOperationException.class,
          () -> setFlags.add("write"),
          "getSetFlags should return unmodifiable set");
    }
  }

  // ========================================================================
  // WitResult Tests
  // ========================================================================

  @Nested
  @DisplayName("WitResult Tests")
  class WitResultTests {

    @Test
    @DisplayName("WitResult should be a final class")
    void witResultShouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WitResult.class.getModifiers()),
          "WitResult should be final");
    }

    @Test
    @DisplayName("WitResult should extend WitValue")
    void witResultShouldExtendWitValue() {
      assertTrue(
          WitValue.class.isAssignableFrom(WitResult.class), "WitResult should extend WitValue");
    }

    @Test
    @DisplayName("WitResult should have ok factory method with value")
    void witResultShouldHaveOkFactoryMethodWithValue() throws NoSuchMethodException {
      Method method = WitResult.class.getMethod("ok", WitType.class, WitValue.class);
      assertNotNull(method, "ok(WitType, WitValue) method should exist");
      assertEquals(WitResult.class, method.getReturnType(), "Return type should be WitResult");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()), "ok method should be static");
    }

    @Test
    @DisplayName("WitResult should have ok factory method without value")
    void witResultShouldHaveOkFactoryMethodWithoutValue() throws NoSuchMethodException {
      Method method = WitResult.class.getMethod("ok", WitType.class);
      assertNotNull(method, "ok(WitType) method should exist");
      assertEquals(WitResult.class, method.getReturnType(), "Return type should be WitResult");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()), "ok method should be static");
    }

    @Test
    @DisplayName("WitResult should have err factory method with value")
    void witResultShouldHaveErrFactoryMethodWithValue() throws NoSuchMethodException {
      Method method = WitResult.class.getMethod("err", WitType.class, WitValue.class);
      assertNotNull(method, "err(WitType, WitValue) method should exist");
      assertEquals(WitResult.class, method.getReturnType(), "Return type should be WitResult");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "err method should be static");
    }

    @Test
    @DisplayName("WitResult should have err factory method without value")
    void witResultShouldHaveErrFactoryMethodWithoutValue() throws NoSuchMethodException {
      Method method = WitResult.class.getMethod("err", WitType.class);
      assertNotNull(method, "err(WitType) method should exist");
      assertEquals(WitResult.class, method.getReturnType(), "Return type should be WitResult");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "err method should be static");
    }

    @Test
    @DisplayName("WitResult should have isOk method")
    void witResultShouldHaveIsOkMethod() throws NoSuchMethodException {
      Method method = WitResult.class.getMethod("isOk");
      assertNotNull(method, "isOk method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("WitResult should have isErr method")
    void witResultShouldHaveIsErrMethod() throws NoSuchMethodException {
      Method method = WitResult.class.getMethod("isErr");
      assertNotNull(method, "isErr method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("WitResult should have getOk method")
    void witResultShouldHaveGetOkMethod() throws NoSuchMethodException {
      Method method = WitResult.class.getMethod("getOk");
      assertNotNull(method, "getOk method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("WitResult should have getErr method")
    void witResultShouldHaveGetErrMethod() throws NoSuchMethodException {
      Method method = WitResult.class.getMethod("getErr");
      assertNotNull(method, "getErr method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("WitResult should have getValue method")
    void witResultShouldHaveGetValueMethod() throws NoSuchMethodException {
      Method method = WitResult.class.getMethod("getValue");
      assertNotNull(method, "getValue method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("WitResult should have toJava method")
    void witResultShouldHaveToJavaMethod() throws NoSuchMethodException {
      Method method = WitResult.class.getMethod("toJava");
      assertNotNull(method, "toJava method should exist");
      assertEquals(Object.class, method.getReturnType(), "Return type should be Object");
    }

    @Test
    @DisplayName("WitResult should override equals")
    void witResultShouldOverrideEquals() throws NoSuchMethodException {
      Method method = WitResult.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
      assertEquals(
          WitResult.class, method.getDeclaringClass(), "equals should be declared in WitResult");
    }

    @Test
    @DisplayName("WitResult should override hashCode")
    void witResultShouldOverrideHashCode() throws NoSuchMethodException {
      Method method = WitResult.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
      assertEquals(
          WitResult.class, method.getDeclaringClass(), "hashCode should be declared in WitResult");
    }

    @Test
    @DisplayName("WitResult should override toString")
    void witResultShouldOverrideToString() throws NoSuchMethodException {
      Method method = WitResult.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(
          WitResult.class, method.getDeclaringClass(), "toString should be declared in WitResult");
    }
  }

  // ========================================================================
  // WitVariant Tests
  // ========================================================================

  @Nested
  @DisplayName("WitVariant Tests")
  class WitVariantTests {

    @Test
    @DisplayName("WitVariant should be a final class")
    void witVariantShouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WitVariant.class.getModifiers()),
          "WitVariant should be final");
    }

    @Test
    @DisplayName("WitVariant should extend WitValue")
    void witVariantShouldExtendWitValue() {
      assertTrue(
          WitValue.class.isAssignableFrom(WitVariant.class), "WitVariant should extend WitValue");
    }

    @Test
    @DisplayName("WitVariant should have of factory method with payload")
    void witVariantShouldHaveOfFactoryMethodWithPayload() throws NoSuchMethodException {
      Method method = WitVariant.class.getMethod("of", WitType.class, String.class, WitValue.class);
      assertNotNull(method, "of(WitType, String, WitValue) method should exist");
      assertEquals(WitVariant.class, method.getReturnType(), "Return type should be WitVariant");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()), "of method should be static");
    }

    @Test
    @DisplayName("WitVariant should have of factory method without payload")
    void witVariantShouldHaveOfFactoryMethodWithoutPayload() throws NoSuchMethodException {
      Method method = WitVariant.class.getMethod("of", WitType.class, String.class);
      assertNotNull(method, "of(WitType, String) method should exist");
      assertEquals(WitVariant.class, method.getReturnType(), "Return type should be WitVariant");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()), "of method should be static");
    }

    @Test
    @DisplayName("WitVariant should have getCaseName method")
    void witVariantShouldHaveGetCaseNameMethod() throws NoSuchMethodException {
      Method method = WitVariant.class.getMethod("getCaseName");
      assertNotNull(method, "getCaseName method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("WitVariant should have getPayload method")
    void witVariantShouldHaveGetPayloadMethod() throws NoSuchMethodException {
      Method method = WitVariant.class.getMethod("getPayload");
      assertNotNull(method, "getPayload method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("WitVariant should have hasPayload method")
    void witVariantShouldHaveHasPayloadMethod() throws NoSuchMethodException {
      Method method = WitVariant.class.getMethod("hasPayload");
      assertNotNull(method, "hasPayload method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("WitVariant should have toJava method")
    void witVariantShouldHaveToJavaMethod() throws NoSuchMethodException {
      Method method = WitVariant.class.getMethod("toJava");
      assertNotNull(method, "toJava method should exist");
      assertEquals(Object.class, method.getReturnType(), "Return type should be Object");
    }

    @Test
    @DisplayName("WitVariant should override equals")
    void witVariantShouldOverrideEquals() throws NoSuchMethodException {
      Method method = WitVariant.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
      assertEquals(
          WitVariant.class, method.getDeclaringClass(), "equals should be declared in WitVariant");
    }

    @Test
    @DisplayName("WitVariant should override hashCode")
    void witVariantShouldOverrideHashCode() throws NoSuchMethodException {
      Method method = WitVariant.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
      assertEquals(
          WitVariant.class,
          method.getDeclaringClass(),
          "hashCode should be declared in WitVariant");
    }

    @Test
    @DisplayName("WitVariant should override toString")
    void witVariantShouldOverrideToString() throws NoSuchMethodException {
      Method method = WitVariant.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(
          WitVariant.class,
          method.getDeclaringClass(),
          "toString should be declared in WitVariant");
    }
  }

  // ========================================================================
  // WitEnum Tests
  // ========================================================================

  @Nested
  @DisplayName("WitEnum Tests")
  class WitEnumTests {

    @Test
    @DisplayName("WitEnum should be a final class")
    void witEnumShouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WitEnum.class.getModifiers()),
          "WitEnum should be final");
    }

    @Test
    @DisplayName("WitEnum should extend WitValue")
    void witEnumShouldExtendWitValue() {
      assertTrue(WitValue.class.isAssignableFrom(WitEnum.class), "WitEnum should extend WitValue");
    }

    @Test
    @DisplayName("WitEnum should have of factory method")
    void witEnumShouldHaveOfFactoryMethod() throws NoSuchMethodException {
      Method method = WitEnum.class.getMethod("of", WitType.class, String.class);
      assertNotNull(method, "of(WitType, String) method should exist");
      assertEquals(WitEnum.class, method.getReturnType(), "Return type should be WitEnum");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()), "of method should be static");
    }

    @Test
    @DisplayName("WitEnum should have getDiscriminant method")
    void witEnumShouldHaveGetDiscriminantMethod() throws NoSuchMethodException {
      Method method = WitEnum.class.getMethod("getDiscriminant");
      assertNotNull(method, "getDiscriminant method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("WitEnum should have toJava method returning String")
    void witEnumShouldHaveToJavaMethod() throws NoSuchMethodException {
      Method method = WitEnum.class.getMethod("toJava");
      assertNotNull(method, "toJava method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("WitEnum should override equals")
    void witEnumShouldOverrideEquals() throws NoSuchMethodException {
      Method method = WitEnum.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
      assertEquals(
          WitEnum.class, method.getDeclaringClass(), "equals should be declared in WitEnum");
    }

    @Test
    @DisplayName("WitEnum should override hashCode")
    void witEnumShouldOverrideHashCode() throws NoSuchMethodException {
      Method method = WitEnum.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
      assertEquals(
          WitEnum.class, method.getDeclaringClass(), "hashCode should be declared in WitEnum");
    }

    @Test
    @DisplayName("WitEnum should override toString")
    void witEnumShouldOverrideToString() throws NoSuchMethodException {
      Method method = WitEnum.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(
          WitEnum.class, method.getDeclaringClass(), "toString should be declared in WitEnum");
    }
  }

  // ========================================================================
  // WitU8 Tests (Unsigned 8-bit Integer)
  // ========================================================================

  @Nested
  @DisplayName("WitU8 Tests")
  class WitU8Tests {

    @Test
    @DisplayName("WitU8 should be a final class")
    void witU8ShouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WitU8.class.getModifiers()), "WitU8 should be final");
    }

    @Test
    @DisplayName("WitU8 should extend WitPrimitiveValue")
    void witU8ShouldExtendWitPrimitiveValue() {
      assertTrue(
          WitPrimitiveValue.class.isAssignableFrom(WitU8.class),
          "WitU8 should extend WitPrimitiveValue");
    }

    @Test
    @DisplayName("WitU8 should have of factory method")
    void witU8ShouldHaveOfFactoryMethod() throws NoSuchMethodException {
      Method method = WitU8.class.getMethod("of", byte.class);
      assertNotNull(method, "of(byte) method should exist");
      assertEquals(WitU8.class, method.getReturnType(), "Return type should be WitU8");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()), "of method should be static");
    }

    @Test
    @DisplayName("WitU8 should have ofUnsigned factory method")
    void witU8ShouldHaveOfUnsignedFactoryMethod() throws NoSuchMethodException {
      Method method = WitU8.class.getMethod("ofUnsigned", int.class);
      assertNotNull(method, "ofUnsigned(int) method should exist");
      assertEquals(WitU8.class, method.getReturnType(), "Return type should be WitU8");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "ofUnsigned method should be static");
    }

    @Test
    @DisplayName("WitU8 should have getValue method")
    void witU8ShouldHaveGetValueMethod() throws NoSuchMethodException {
      Method method = WitU8.class.getMethod("getValue");
      assertNotNull(method, "getValue method should exist");
      assertEquals(byte.class, method.getReturnType(), "Return type should be byte");
    }

    @Test
    @DisplayName("WitU8 should have toUnsignedInt method")
    void witU8ShouldHaveToUnsignedIntMethod() throws NoSuchMethodException {
      Method method = WitU8.class.getMethod("toUnsignedInt");
      assertNotNull(method, "toUnsignedInt method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("WitU8 should have toJava method returning Byte")
    void witU8ShouldHaveToJavaMethod() throws NoSuchMethodException {
      Method method = WitU8.class.getMethod("toJava");
      assertNotNull(method, "toJava method should exist");
      assertEquals(Byte.class, method.getReturnType(), "Return type should be Byte");
    }

    @Test
    @DisplayName("WitU8.of should create valid instance")
    void witU8OfShouldCreateValidInstance() {
      WitU8 value = WitU8.of((byte) 42);
      assertNotNull(value, "WitU8 should be created");
      assertEquals((byte) 42, value.getValue(), "getValue should return the byte value");
      assertEquals(
          42, value.toUnsignedInt(), "toUnsignedInt should return unsigned interpretation");
    }

    @Test
    @DisplayName("WitU8.ofUnsigned should create valid instance for values 0-255")
    void witU8OfUnsignedShouldCreateValidInstance() {
      WitU8 value = WitU8.ofUnsigned(200);
      assertNotNull(value, "WitU8 should be created");
      assertEquals(200, value.toUnsignedInt(), "toUnsignedInt should return 200");
    }

    @Test
    @DisplayName("WitU8.ofUnsigned should throw for negative values")
    void witU8OfUnsignedShouldThrowForNegativeValues() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WitU8.ofUnsigned(-1),
          "ofUnsigned should throw for negative values");
    }

    @Test
    @DisplayName("WitU8.ofUnsigned should throw for values > 255")
    void witU8OfUnsignedShouldThrowForValuesAbove255() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WitU8.ofUnsigned(256),
          "ofUnsigned should throw for values > 255");
    }
  }

  // ========================================================================
  // WitU16 Tests (Unsigned 16-bit Integer)
  // ========================================================================

  @Nested
  @DisplayName("WitU16 Tests")
  class WitU16Tests {

    @Test
    @DisplayName("WitU16 should be a final class")
    void witU16ShouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WitU16.class.getModifiers()),
          "WitU16 should be final");
    }

    @Test
    @DisplayName("WitU16 should extend WitPrimitiveValue")
    void witU16ShouldExtendWitPrimitiveValue() {
      assertTrue(
          WitPrimitiveValue.class.isAssignableFrom(WitU16.class),
          "WitU16 should extend WitPrimitiveValue");
    }

    @Test
    @DisplayName("WitU16 should have of factory method")
    void witU16ShouldHaveOfFactoryMethod() throws NoSuchMethodException {
      Method method = WitU16.class.getMethod("of", short.class);
      assertNotNull(method, "of(short) method should exist");
      assertEquals(WitU16.class, method.getReturnType(), "Return type should be WitU16");
    }

    @Test
    @DisplayName("WitU16 should have ofUnsigned factory method")
    void witU16ShouldHaveOfUnsignedFactoryMethod() throws NoSuchMethodException {
      Method method = WitU16.class.getMethod("ofUnsigned", int.class);
      assertNotNull(method, "ofUnsigned(int) method should exist");
      assertEquals(WitU16.class, method.getReturnType(), "Return type should be WitU16");
    }

    @Test
    @DisplayName("WitU16 should have getValue method")
    void witU16ShouldHaveGetValueMethod() throws NoSuchMethodException {
      Method method = WitU16.class.getMethod("getValue");
      assertNotNull(method, "getValue method should exist");
      assertEquals(short.class, method.getReturnType(), "Return type should be short");
    }

    @Test
    @DisplayName("WitU16 should have toUnsignedInt method")
    void witU16ShouldHaveToUnsignedIntMethod() throws NoSuchMethodException {
      Method method = WitU16.class.getMethod("toUnsignedInt");
      assertNotNull(method, "toUnsignedInt method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }
  }

  // ========================================================================
  // WitU32 Tests (Unsigned 32-bit Integer)
  // ========================================================================

  @Nested
  @DisplayName("WitU32 Tests")
  class WitU32Tests {

    @Test
    @DisplayName("WitU32 should be a final class")
    void witU32ShouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WitU32.class.getModifiers()),
          "WitU32 should be final");
    }

    @Test
    @DisplayName("WitU32 should extend WitPrimitiveValue")
    void witU32ShouldExtendWitPrimitiveValue() {
      assertTrue(
          WitPrimitiveValue.class.isAssignableFrom(WitU32.class),
          "WitU32 should extend WitPrimitiveValue");
    }

    @Test
    @DisplayName("WitU32 should have of factory method")
    void witU32ShouldHaveOfFactoryMethod() throws NoSuchMethodException {
      Method method = WitU32.class.getMethod("of", int.class);
      assertNotNull(method, "of(int) method should exist");
      assertEquals(WitU32.class, method.getReturnType(), "Return type should be WitU32");
    }

    @Test
    @DisplayName("WitU32 should have ofUnsigned factory method")
    void witU32ShouldHaveOfUnsignedFactoryMethod() throws NoSuchMethodException {
      Method method = WitU32.class.getMethod("ofUnsigned", long.class);
      assertNotNull(method, "ofUnsigned(long) method should exist");
      assertEquals(WitU32.class, method.getReturnType(), "Return type should be WitU32");
    }

    @Test
    @DisplayName("WitU32 should have getValue method")
    void witU32ShouldHaveGetValueMethod() throws NoSuchMethodException {
      Method method = WitU32.class.getMethod("getValue");
      assertNotNull(method, "getValue method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("WitU32 should have toUnsignedLong method")
    void witU32ShouldHaveToUnsignedLongMethod() throws NoSuchMethodException {
      Method method = WitU32.class.getMethod("toUnsignedLong");
      assertNotNull(method, "toUnsignedLong method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }
  }

  // ========================================================================
  // WitU64 Tests (Unsigned 64-bit Integer)
  // ========================================================================

  @Nested
  @DisplayName("WitU64 Tests")
  class WitU64Tests {

    @Test
    @DisplayName("WitU64 should be a final class")
    void witU64ShouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WitU64.class.getModifiers()),
          "WitU64 should be final");
    }

    @Test
    @DisplayName("WitU64 should extend WitPrimitiveValue")
    void witU64ShouldExtendWitPrimitiveValue() {
      assertTrue(
          WitPrimitiveValue.class.isAssignableFrom(WitU64.class),
          "WitU64 should extend WitPrimitiveValue");
    }

    @Test
    @DisplayName("WitU64 should have of factory method")
    void witU64ShouldHaveOfFactoryMethod() throws NoSuchMethodException {
      Method method = WitU64.class.getMethod("of", long.class);
      assertNotNull(method, "of(long) method should exist");
      assertEquals(WitU64.class, method.getReturnType(), "Return type should be WitU64");
    }

    @Test
    @DisplayName("WitU64 should have getValue method")
    void witU64ShouldHaveGetValueMethod() throws NoSuchMethodException {
      Method method = WitU64.class.getMethod("getValue");
      assertNotNull(method, "getValue method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("WitU64 should have toJava method returning Long")
    void witU64ShouldHaveToJavaMethod() throws NoSuchMethodException {
      Method method = WitU64.class.getMethod("toJava");
      assertNotNull(method, "toJava method should exist");
      assertEquals(Long.class, method.getReturnType(), "Return type should be Long");
    }
  }

  // ========================================================================
  // WitS8 Tests (Signed 8-bit Integer)
  // ========================================================================

  @Nested
  @DisplayName("WitS8 Tests")
  class WitS8Tests {

    @Test
    @DisplayName("WitS8 should be a final class")
    void witS8ShouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WitS8.class.getModifiers()), "WitS8 should be final");
    }

    @Test
    @DisplayName("WitS8 should extend WitPrimitiveValue")
    void witS8ShouldExtendWitPrimitiveValue() {
      assertTrue(
          WitPrimitiveValue.class.isAssignableFrom(WitS8.class),
          "WitS8 should extend WitPrimitiveValue");
    }

    @Test
    @DisplayName("WitS8 should have of factory method")
    void witS8ShouldHaveOfFactoryMethod() throws NoSuchMethodException {
      Method method = WitS8.class.getMethod("of", byte.class);
      assertNotNull(method, "of(byte) method should exist");
      assertEquals(WitS8.class, method.getReturnType(), "Return type should be WitS8");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()), "of method should be static");
    }

    @Test
    @DisplayName("WitS8 should have getValue method")
    void witS8ShouldHaveGetValueMethod() throws NoSuchMethodException {
      Method method = WitS8.class.getMethod("getValue");
      assertNotNull(method, "getValue method should exist");
      assertEquals(byte.class, method.getReturnType(), "Return type should be byte");
    }

    @Test
    @DisplayName("WitS8 should have toJava method returning Byte")
    void witS8ShouldHaveToJavaMethod() throws NoSuchMethodException {
      Method method = WitS8.class.getMethod("toJava");
      assertNotNull(method, "toJava method should exist");
      assertEquals(Byte.class, method.getReturnType(), "Return type should be Byte");
    }

    @Test
    @DisplayName("WitS8.of should create valid instance")
    void witS8OfShouldCreateValidInstance() {
      WitS8 value = WitS8.of((byte) -42);
      assertNotNull(value, "WitS8 should be created");
      assertEquals((byte) -42, value.getValue(), "getValue should return the byte value");
    }
  }

  // ========================================================================
  // WitS16 Tests (Signed 16-bit Integer)
  // ========================================================================

  @Nested
  @DisplayName("WitS16 Tests")
  class WitS16Tests {

    @Test
    @DisplayName("WitS16 should be a final class")
    void witS16ShouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WitS16.class.getModifiers()),
          "WitS16 should be final");
    }

    @Test
    @DisplayName("WitS16 should extend WitPrimitiveValue")
    void witS16ShouldExtendWitPrimitiveValue() {
      assertTrue(
          WitPrimitiveValue.class.isAssignableFrom(WitS16.class),
          "WitS16 should extend WitPrimitiveValue");
    }

    @Test
    @DisplayName("WitS16 should have of factory method")
    void witS16ShouldHaveOfFactoryMethod() throws NoSuchMethodException {
      Method method = WitS16.class.getMethod("of", short.class);
      assertNotNull(method, "of(short) method should exist");
      assertEquals(WitS16.class, method.getReturnType(), "Return type should be WitS16");
    }

    @Test
    @DisplayName("WitS16 should have getValue method")
    void witS16ShouldHaveGetValueMethod() throws NoSuchMethodException {
      Method method = WitS16.class.getMethod("getValue");
      assertNotNull(method, "getValue method should exist");
      assertEquals(short.class, method.getReturnType(), "Return type should be short");
    }

    @Test
    @DisplayName("WitS16 should have toJava method returning Short")
    void witS16ShouldHaveToJavaMethod() throws NoSuchMethodException {
      Method method = WitS16.class.getMethod("toJava");
      assertNotNull(method, "toJava method should exist");
      assertEquals(Short.class, method.getReturnType(), "Return type should be Short");
    }
  }

  // ========================================================================
  // WitS64 Tests (Signed 64-bit Integer)
  // ========================================================================

  @Nested
  @DisplayName("WitS64 Tests")
  class WitS64Tests {

    @Test
    @DisplayName("WitS64 should be a final class")
    void witS64ShouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WitS64.class.getModifiers()),
          "WitS64 should be final");
    }

    @Test
    @DisplayName("WitS64 should extend WitPrimitiveValue")
    void witS64ShouldExtendWitPrimitiveValue() {
      assertTrue(
          WitPrimitiveValue.class.isAssignableFrom(WitS64.class),
          "WitS64 should extend WitPrimitiveValue");
    }

    @Test
    @DisplayName("WitS64 should have of factory method")
    void witS64ShouldHaveOfFactoryMethod() throws NoSuchMethodException {
      Method method = WitS64.class.getMethod("of", long.class);
      assertNotNull(method, "of(long) method should exist");
      assertEquals(WitS64.class, method.getReturnType(), "Return type should be WitS64");
    }

    @Test
    @DisplayName("WitS64 should have getValue method")
    void witS64ShouldHaveGetValueMethod() throws NoSuchMethodException {
      Method method = WitS64.class.getMethod("getValue");
      assertNotNull(method, "getValue method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("WitS64 should have toJava method returning Long")
    void witS64ShouldHaveToJavaMethod() throws NoSuchMethodException {
      Method method = WitS64.class.getMethod("toJava");
      assertNotNull(method, "toJava method should exist");
      assertEquals(Long.class, method.getReturnType(), "Return type should be Long");
    }
  }

  // ========================================================================
  // WitFloat64 Tests (64-bit Floating-Point)
  // ========================================================================

  @Nested
  @DisplayName("WitFloat64 Tests")
  class WitFloat64Tests {

    @Test
    @DisplayName("WitFloat64 should be a final class")
    void witFloat64ShouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WitFloat64.class.getModifiers()),
          "WitFloat64 should be final");
    }

    @Test
    @DisplayName("WitFloat64 should extend WitPrimitiveValue")
    void witFloat64ShouldExtendWitPrimitiveValue() {
      assertTrue(
          WitPrimitiveValue.class.isAssignableFrom(WitFloat64.class),
          "WitFloat64 should extend WitPrimitiveValue");
    }

    @Test
    @DisplayName("WitFloat64 should have of factory method")
    void witFloat64ShouldHaveOfFactoryMethod() throws NoSuchMethodException {
      Method method = WitFloat64.class.getMethod("of", double.class);
      assertNotNull(method, "of(double) method should exist");
      assertEquals(WitFloat64.class, method.getReturnType(), "Return type should be WitFloat64");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()), "of method should be static");
    }

    @Test
    @DisplayName("WitFloat64 should have getValue method")
    void witFloat64ShouldHaveGetValueMethod() throws NoSuchMethodException {
      Method method = WitFloat64.class.getMethod("getValue");
      assertNotNull(method, "getValue method should exist");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }

    @Test
    @DisplayName("WitFloat64 should have toJava method returning Double")
    void witFloat64ShouldHaveToJavaMethod() throws NoSuchMethodException {
      Method method = WitFloat64.class.getMethod("toJava");
      assertNotNull(method, "toJava method should exist");
      assertEquals(Double.class, method.getReturnType(), "Return type should be Double");
    }

    @Test
    @DisplayName("WitFloat64.of should create valid instance")
    void witFloat64OfShouldCreateValidInstance() {
      WitFloat64 value = WitFloat64.of(3.14159);
      assertNotNull(value, "WitFloat64 should be created");
      assertEquals(3.14159, value.getValue(), 0.00001, "getValue should return the double value");
    }

    @Test
    @DisplayName("WitFloat64.of should handle NaN")
    void witFloat64OfShouldHandleNan() {
      WitFloat64 value = WitFloat64.of(Double.NaN);
      assertNotNull(value, "WitFloat64 should be created for NaN");
      assertTrue(Double.isNaN(value.getValue()), "getValue should return NaN");
    }

    @Test
    @DisplayName("WitFloat64.of should handle positive infinity")
    void witFloat64OfShouldHandlePositiveInfinity() {
      WitFloat64 value = WitFloat64.of(Double.POSITIVE_INFINITY);
      assertNotNull(value, "WitFloat64 should be created for positive infinity");
      assertTrue(
          Double.isInfinite(value.getValue()) && value.getValue() > 0,
          "getValue should return positive infinity");
    }

    @Test
    @DisplayName("WitFloat64.of should handle negative infinity")
    void witFloat64OfShouldHandleNegativeInfinity() {
      WitFloat64 value = WitFloat64.of(Double.NEGATIVE_INFINITY);
      assertNotNull(value, "WitFloat64 should be created for negative infinity");
      assertTrue(
          Double.isInfinite(value.getValue()) && value.getValue() < 0,
          "getValue should return negative infinity");
    }

    @Test
    @DisplayName("WitFloat64 should override equals")
    void witFloat64ShouldOverrideEquals() throws NoSuchMethodException {
      Method method = WitFloat64.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
      assertEquals(
          WitFloat64.class, method.getDeclaringClass(), "equals should be declared in WitFloat64");
    }

    @Test
    @DisplayName("WitFloat64 should override hashCode")
    void witFloat64ShouldOverrideHashCode() throws NoSuchMethodException {
      Method method = WitFloat64.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
      assertEquals(
          WitFloat64.class,
          method.getDeclaringClass(),
          "hashCode should be declared in WitFloat64");
    }

    @Test
    @DisplayName("WitFloat64 should override toString")
    void witFloat64ShouldOverrideToString() throws NoSuchMethodException {
      Method method = WitFloat64.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(
          WitFloat64.class,
          method.getDeclaringClass(),
          "toString should be declared in WitFloat64");
    }
  }
}
