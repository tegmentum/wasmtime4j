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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WitRangeException;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitChar} class.
 *
 * <p>WitChar represents a WIT character value (Unicode scalar value). Valid values are U+0000 to
 * U+D7FF and U+E000 to U+10FFFF (excluding surrogate code points).
 */
@DisplayName("WitChar Tests")
class WitCharTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(WitChar.class.getModifiers()), "WitChar should be final");
    }

    @Test
    @DisplayName("should extend WitPrimitiveValue")
    void shouldExtendWitPrimitiveValue() {
      assertTrue(
          WitPrimitiveValue.class.isAssignableFrom(WitChar.class),
          "WitChar should extend WitPrimitiveValue");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should create WitChar from valid ASCII codepoint")
    void shouldCreateFromValidAsciiCodepoint() throws WitRangeException {
      final WitChar witChar = WitChar.of(65); // 'A'
      assertNotNull(witChar, "Should create WitChar from ASCII codepoint");
      assertEquals(65, witChar.getCodepoint(), "Codepoint should be 65");
    }

    @Test
    @DisplayName("should create WitChar from valid BMP codepoint")
    void shouldCreateFromValidBmpCodepoint() throws WitRangeException {
      final WitChar witChar = WitChar.of(0x4E2D); // Chinese character
      assertNotNull(witChar, "Should create WitChar from BMP codepoint");
      assertEquals(0x4E2D, witChar.getCodepoint(), "Codepoint should match");
    }

    @Test
    @DisplayName("should create WitChar from valid supplementary codepoint")
    void shouldCreateFromValidSupplementaryCodepoint() throws WitRangeException {
      final WitChar witChar = WitChar.of(0x1F600); // Emoji
      assertNotNull(witChar, "Should create WitChar from supplementary codepoint");
      assertEquals(0x1F600, witChar.getCodepoint(), "Codepoint should match");
    }

    @Test
    @DisplayName("should create WitChar from minimum codepoint")
    void shouldCreateFromMinimumCodepoint() throws WitRangeException {
      final WitChar witChar = WitChar.of(0x0000);
      assertNotNull(witChar, "Should create from minimum codepoint");
      assertEquals(0, witChar.getCodepoint(), "Codepoint should be 0");
    }

    @Test
    @DisplayName("should create WitChar from maximum codepoint")
    void shouldCreateFromMaximumCodepoint() throws WitRangeException {
      final WitChar witChar = WitChar.of(0x10FFFF);
      assertNotNull(witChar, "Should create from maximum codepoint");
      assertEquals(0x10FFFF, witChar.getCodepoint(), "Codepoint should be max");
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("should reject negative codepoint")
    void shouldRejectNegativeCodepoint() {
      assertThrows(
          WitRangeException.class, () -> WitChar.of(-1), "Should reject negative codepoint");
    }

    @Test
    @DisplayName("should reject codepoint above maximum")
    void shouldRejectCodepointAboveMaximum() {
      assertThrows(
          WitRangeException.class,
          () -> WitChar.of(0x110000),
          "Should reject codepoint above 0x10FFFF");
    }

    @Test
    @DisplayName("should reject low surrogate codepoint")
    void shouldRejectLowSurrogateCodepoint() {
      assertThrows(
          WitRangeException.class,
          () -> WitChar.of(0xD800),
          "Should reject low surrogate codepoint");
    }

    @Test
    @DisplayName("should reject high surrogate codepoint")
    void shouldRejectHighSurrogateCodepoint() {
      assertThrows(
          WitRangeException.class,
          () -> WitChar.of(0xDFFF),
          "Should reject high surrogate codepoint");
    }

    @Test
    @DisplayName("should reject middle surrogate codepoint")
    void shouldRejectMiddleSurrogateCodepoint() {
      assertThrows(
          WitRangeException.class,
          () -> WitChar.of(0xDC00),
          "Should reject middle surrogate codepoint");
    }
  }

  @Nested
  @DisplayName("Value Access Tests")
  class ValueAccessTests {

    @Test
    @DisplayName("getCodepoint should return correct value")
    void getCodepointShouldReturnCorrectValue() throws WitRangeException {
      final WitChar witChar = WitChar.of(97);
      assertEquals(97, witChar.getCodepoint(), "Should return correct codepoint");
    }

    @Test
    @DisplayName("toJava should return Character")
    void toJavaShouldReturnCharacter() throws WitRangeException {
      final WitChar witChar = WitChar.of(65);
      assertEquals(Character.valueOf('A'), witChar.toJava(), "toJava should return Character 'A'");
    }
  }

  @Nested
  @DisplayName("Boundary Tests")
  class BoundaryTests {

    @Test
    @DisplayName("should accept codepoint just before surrogate range")
    void shouldAcceptCodepointBeforeSurrogateRange() throws WitRangeException {
      final WitChar witChar = WitChar.of(0xD7FF);
      assertEquals(
          0xD7FF, witChar.getCodepoint(), "Should accept codepoint just before surrogate range");
    }

    @Test
    @DisplayName("should accept codepoint just after surrogate range")
    void shouldAcceptCodepointAfterSurrogateRange() throws WitRangeException {
      final WitChar witChar = WitChar.of(0xE000);
      assertEquals(
          0xE000, witChar.getCodepoint(), "Should accept codepoint just after surrogate range");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("same codepoint should be equal")
    void sameCodepointShouldBeEqual() throws WitRangeException {
      final WitChar char1 = WitChar.of(65);
      final WitChar char2 = WitChar.of(65);
      assertEquals(char1, char2, "Same codepoints should be equal");
    }

    @Test
    @DisplayName("different codepoints should not be equal")
    void differentCodepointsShouldNotBeEqual() throws WitRangeException {
      final WitChar char1 = WitChar.of(65);
      final WitChar char2 = WitChar.of(66);
      assertNotEquals(char1, char2, "Different codepoints should not be equal");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("same codepoint should have same hash code")
    void sameCodepointShouldHaveSameHashCode() throws WitRangeException {
      final WitChar char1 = WitChar.of(65);
      final WitChar char2 = WitChar.of(65);
      assertEquals(
          char1.hashCode(), char2.hashCode(), "Same codepoints should have same hash code");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain codepoint")
    void toStringShouldContainCodepoint() throws WitRangeException {
      final WitChar witChar = WitChar.of(65);
      final String str = witChar.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(
          str.contains("0041") || str.contains("U+"), "toString should contain hex codepoint");
    }
  }
}
