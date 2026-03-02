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
package ai.tegmentum.wasmtime4j.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Enabled} enum.
 *
 * <p>Verifies enum values, toJsonValue(), fromBoolean(), and fromString() methods.
 */
@DisplayName("Enabled Tests")
class EnabledTest {

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have exactly three values")
    void shouldHaveThreeValues() {
      assertEquals(3, Enabled.values().length, "Enabled should have exactly 3 values");
    }

    @Test
    @DisplayName("should have AUTO, YES, and NO values")
    void shouldHaveExpectedValues() {
      assertEquals(Enabled.AUTO, Enabled.valueOf("AUTO"), "AUTO should exist");
      assertEquals(Enabled.YES, Enabled.valueOf("YES"), "YES should exist");
      assertEquals(Enabled.NO, Enabled.valueOf("NO"), "NO should exist");
    }
  }

  @Nested
  @DisplayName("toJsonValue Tests")
  class ToJsonValueTests {

    @Test
    @DisplayName("AUTO should return 'auto'")
    void autoShouldReturnAuto() {
      assertEquals("auto", Enabled.AUTO.toJsonValue(), "AUTO.toJsonValue() should return 'auto'");
    }

    @Test
    @DisplayName("YES should return 'yes'")
    void yesShouldReturnYes() {
      assertEquals("yes", Enabled.YES.toJsonValue(), "YES.toJsonValue() should return 'yes'");
    }

    @Test
    @DisplayName("NO should return 'no'")
    void noShouldReturnNo() {
      assertEquals("no", Enabled.NO.toJsonValue(), "NO.toJsonValue() should return 'no'");
    }
  }

  @Nested
  @DisplayName("fromBoolean Tests")
  class FromBooleanTests {

    @Test
    @DisplayName("true should return YES")
    void trueShouldReturnYes() {
      assertEquals(Enabled.YES, Enabled.fromBoolean(true), "fromBoolean(true) should return YES");
    }

    @Test
    @DisplayName("false should return NO")
    void falseShouldReturnNo() {
      assertEquals(Enabled.NO, Enabled.fromBoolean(false), "fromBoolean(false) should return NO");
    }
  }

  @Nested
  @DisplayName("fromString Tests")
  class FromStringTests {

    @Test
    @DisplayName("should parse 'auto' to AUTO")
    void shouldParseAuto() {
      assertEquals(
          Enabled.AUTO, Enabled.fromString("auto"), "fromString('auto') should return AUTO");
    }

    @Test
    @DisplayName("should parse 'yes' to YES")
    void shouldParseYes() {
      assertEquals(Enabled.YES, Enabled.fromString("yes"), "fromString('yes') should return YES");
    }

    @Test
    @DisplayName("should parse 'no' to NO")
    void shouldParseNo() {
      assertEquals(Enabled.NO, Enabled.fromString("no"), "fromString('no') should return NO");
    }

    @Test
    @DisplayName("should parse 'true' to YES")
    void shouldParseTrue() {
      assertEquals(Enabled.YES, Enabled.fromString("true"), "fromString('true') should return YES");
    }

    @Test
    @DisplayName("should parse 'false' to NO")
    void shouldParseFalse() {
      assertEquals(Enabled.NO, Enabled.fromString("false"), "fromString('false') should return NO");
    }

    @Test
    @DisplayName("should be case insensitive")
    void shouldBeCaseInsensitive() {
      assertEquals(Enabled.AUTO, Enabled.fromString("AUTO"), "Should parse 'AUTO' as AUTO");
      assertEquals(Enabled.YES, Enabled.fromString("YES"), "Should parse 'YES' as YES");
      assertEquals(Enabled.NO, Enabled.fromString("No"), "Should parse 'No' as NO");
      assertEquals(Enabled.YES, Enabled.fromString("TRUE"), "Should parse 'TRUE' as YES");
    }

    @Test
    @DisplayName("should throw for null")
    void shouldThrowForNull() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> Enabled.fromString(null),
              "Should throw for null");
      assertTrue(
          exception.getMessage().contains("null"),
          "Error should mention null, got: " + exception.getMessage());
    }

    @Test
    @DisplayName("should throw for unknown string")
    void shouldThrowForUnknownString() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> Enabled.fromString("maybe"),
              "Should throw for unknown string");
      assertTrue(
          exception.getMessage().contains("Unknown"),
          "Error should mention unknown, got: " + exception.getMessage());
    }
  }
}
