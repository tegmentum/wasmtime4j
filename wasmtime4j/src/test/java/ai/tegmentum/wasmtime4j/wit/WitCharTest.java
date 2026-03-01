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

import ai.tegmentum.wasmtime4j.exception.ValidationException;
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
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should create WitChar from valid ASCII codepoint")
    void shouldCreateFromValidAsciiCodepoint() throws ValidationException {
      final WitChar witChar = WitChar.of(65); // 'A'
      assertNotNull(witChar, "Should create WitChar from ASCII codepoint");
      assertEquals(65, witChar.getCodepoint(), "Codepoint should be 65");
    }

    @Test
    @DisplayName("should create WitChar from valid BMP codepoint")
    void shouldCreateFromValidBmpCodepoint() throws ValidationException {
      final WitChar witChar = WitChar.of(0x4E2D); // Chinese character
      assertNotNull(witChar, "Should create WitChar from BMP codepoint");
      assertEquals(0x4E2D, witChar.getCodepoint(), "Codepoint should match");
    }

    @Test
    @DisplayName("should create WitChar from valid supplementary codepoint")
    void shouldCreateFromValidSupplementaryCodepoint() throws ValidationException {
      final WitChar witChar = WitChar.of(0x1F600); // Emoji
      assertNotNull(witChar, "Should create WitChar from supplementary codepoint");
      assertEquals(0x1F600, witChar.getCodepoint(), "Codepoint should match");
    }

    @Test
    @DisplayName("should create WitChar from minimum codepoint")
    void shouldCreateFromMinimumCodepoint() throws ValidationException {
      final WitChar witChar = WitChar.of(0x0000);
      assertNotNull(witChar, "Should create from minimum codepoint");
      assertEquals(0, witChar.getCodepoint(), "Codepoint should be 0");
    }

    @Test
    @DisplayName("should create WitChar from maximum codepoint")
    void shouldCreateFromMaximumCodepoint() throws ValidationException {
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
          ValidationException.class, () -> WitChar.of(-1), "Should reject negative codepoint");
    }

    @Test
    @DisplayName("should reject codepoint above maximum")
    void shouldRejectCodepointAboveMaximum() {
      assertThrows(
          ValidationException.class,
          () -> WitChar.of(0x110000),
          "Should reject codepoint above 0x10FFFF");
    }

    @Test
    @DisplayName("should reject low surrogate codepoint")
    void shouldRejectLowSurrogateCodepoint() {
      assertThrows(
          ValidationException.class,
          () -> WitChar.of(0xD800),
          "Should reject low surrogate codepoint");
    }

    @Test
    @DisplayName("should reject high surrogate codepoint")
    void shouldRejectHighSurrogateCodepoint() {
      assertThrows(
          ValidationException.class,
          () -> WitChar.of(0xDFFF),
          "Should reject high surrogate codepoint");
    }

    @Test
    @DisplayName("should reject middle surrogate codepoint")
    void shouldRejectMiddleSurrogateCodepoint() {
      assertThrows(
          ValidationException.class,
          () -> WitChar.of(0xDC00),
          "Should reject middle surrogate codepoint");
    }
  }

  @Nested
  @DisplayName("Value Access Tests")
  class ValueAccessTests {

    @Test
    @DisplayName("getCodepoint should return correct value")
    void getCodepointShouldReturnCorrectValue() throws ValidationException {
      final WitChar witChar = WitChar.of(97);
      assertEquals(97, witChar.getCodepoint(), "Should return correct codepoint");
    }

    @Test
    @DisplayName("toJava should return Character")
    void toJavaShouldReturnCharacter() throws ValidationException {
      final WitChar witChar = WitChar.of(65);
      assertEquals(Character.valueOf('A'), witChar.toJava(), "toJava should return Character 'A'");
    }
  }

  @Nested
  @DisplayName("Boundary Tests")
  class BoundaryTests {

    @Test
    @DisplayName("should accept codepoint just before surrogate range")
    void shouldAcceptCodepointBeforeSurrogateRange() throws ValidationException {
      final WitChar witChar = WitChar.of(0xD7FF);
      assertEquals(
          0xD7FF, witChar.getCodepoint(), "Should accept codepoint just before surrogate range");
    }

    @Test
    @DisplayName("should accept codepoint just after surrogate range")
    void shouldAcceptCodepointAfterSurrogateRange() throws ValidationException {
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
    void sameCodepointShouldBeEqual() throws ValidationException {
      final WitChar char1 = WitChar.of(65);
      final WitChar char2 = WitChar.of(65);
      assertEquals(char1, char2, "Same codepoints should be equal");
    }

    @Test
    @DisplayName("different codepoints should not be equal")
    void differentCodepointsShouldNotBeEqual() throws ValidationException {
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
    void sameCodepointShouldHaveSameHashCode() throws ValidationException {
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
    void toStringShouldContainCodepoint() throws ValidationException {
      final WitChar witChar = WitChar.of(65);
      final String str = witChar.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(
          str.contains("0041") || str.contains("U+"), "toString should contain hex codepoint");
    }
  }

  @Nested
  @DisplayName("Boundary Mutation Killing Tests")
  class BoundaryMutationKillingTests {

    // Constants matching WitChar's internal values
    private static final int MIN_CODEPOINT = 0x0000;
    private static final int MAX_CODEPOINT = 0x10FFFF;
    private static final int SURROGATE_MIN = 0xD800;
    private static final int SURROGATE_MAX = 0xDFFF;

    @Test
    @DisplayName("MIN_CODEPOINT boundary - exactly MIN must be accepted, MIN-1 must be rejected")
    void minCodepointBoundaryMutationTest() throws ValidationException {
      // If mutation changes < to <=, MIN_CODEPOINT would be rejected when it should be accepted
      final WitChar atMin = WitChar.of(MIN_CODEPOINT);
      assertEquals(
          MIN_CODEPOINT,
          atMin.getCodepoint(),
          "Codepoint exactly at MIN_CODEPOINT (0) must be accepted");

      // Verify MIN-1 is rejected (tests the < condition)
      assertThrows(
          ValidationException.class,
          () -> WitChar.of(MIN_CODEPOINT - 1),
          "Codepoint at MIN_CODEPOINT-1 (-1) must be rejected");
    }

    @Test
    @DisplayName("MAX_CODEPOINT boundary - exactly MAX must be accepted, MAX+1 must be rejected")
    void maxCodepointBoundaryMutationTest() throws ValidationException {
      // If mutation changes > to >=, MAX_CODEPOINT would be rejected when it should be accepted
      final WitChar atMax = WitChar.of(MAX_CODEPOINT);
      assertEquals(
          MAX_CODEPOINT,
          atMax.getCodepoint(),
          "Codepoint exactly at MAX_CODEPOINT (0x10FFFF) must be accepted");

      // Verify MAX+1 is rejected (tests the > condition)
      assertThrows(
          ValidationException.class,
          () -> WitChar.of(MAX_CODEPOINT + 1),
          "Codepoint at MAX_CODEPOINT+1 (0x110000) must be rejected");
    }

    @Test
    @DisplayName("SURROGATE_MIN boundary - exactly MIN must be rejected, MIN-1 must be accepted")
    void surrogateMinBoundaryMutationTest() throws ValidationException {
      // If mutation changes >= to >, SURROGATE_MIN would be accepted when it should be rejected
      assertThrows(
          ValidationException.class,
          () -> WitChar.of(SURROGATE_MIN),
          "Codepoint exactly at SURROGATE_MIN (0xD800) must be rejected");

      // Verify SURROGATE_MIN-1 is accepted (tests the >= condition boundary)
      final WitChar beforeSurrogate = WitChar.of(SURROGATE_MIN - 1);
      assertEquals(
          SURROGATE_MIN - 1,
          beforeSurrogate.getCodepoint(),
          "Codepoint at SURROGATE_MIN-1 (0xD7FF) must be accepted");
    }

    @Test
    @DisplayName("SURROGATE_MAX boundary - exactly MAX must be rejected, MAX+1 must be accepted")
    void surrogateMaxBoundaryMutationTest() throws ValidationException {
      // If mutation changes <= to <, SURROGATE_MAX would be accepted when it should be rejected
      assertThrows(
          ValidationException.class,
          () -> WitChar.of(SURROGATE_MAX),
          "Codepoint exactly at SURROGATE_MAX (0xDFFF) must be rejected");

      // Verify SURROGATE_MAX+1 is accepted (tests the <= condition boundary)
      final WitChar afterSurrogate = WitChar.of(SURROGATE_MAX + 1);
      assertEquals(
          SURROGATE_MAX + 1,
          afterSurrogate.getCodepoint(),
          "Codepoint at SURROGATE_MAX+1 (0xE000) must be accepted");
    }

    @Test
    @DisplayName("Combined boundary test - all four boundary conditions in single test")
    void allBoundaryConditionsCombinedTest() throws ValidationException {
      // This test explicitly verifies both sides of each boundary for mutation killing
      // MIN boundary: 0 accepted, -1 rejected
      assertEquals(0, WitChar.of(0).getCodepoint(), "0 must be accepted");
      assertThrows(ValidationException.class, () -> WitChar.of(-1), "-1 must be rejected");

      // MAX boundary: 0x10FFFF accepted, 0x110000 rejected
      assertEquals(0x10FFFF, WitChar.of(0x10FFFF).getCodepoint(), "0x10FFFF must be accepted");
      assertThrows(
          ValidationException.class, () -> WitChar.of(0x110000), "0x110000 must be rejected");

      // SURROGATE_MIN boundary: 0xD800 rejected, 0xD7FF accepted
      assertThrows(ValidationException.class, () -> WitChar.of(0xD800), "0xD800 must be rejected");
      assertEquals(0xD7FF, WitChar.of(0xD7FF).getCodepoint(), "0xD7FF must be accepted");

      // SURROGATE_MAX boundary: 0xDFFF rejected, 0xE000 accepted
      assertThrows(ValidationException.class, () -> WitChar.of(0xDFFF), "0xDFFF must be rejected");
      assertEquals(0xE000, WitChar.of(0xE000).getCodepoint(), "0xE000 must be accepted");
    }
  }
}
