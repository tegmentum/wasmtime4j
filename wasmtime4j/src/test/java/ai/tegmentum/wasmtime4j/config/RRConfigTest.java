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
 * Tests for {@link RRConfig} enum.
 *
 * <p>Verifies enum values, getRustName(), and fromString() parsing.
 */
@DisplayName("RRConfig Tests")
class RRConfigTest {

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have exactly three values")
    void shouldHaveThreeValues() {
      assertEquals(3, RRConfig.values().length, "RRConfig should have exactly 3 values");
    }

    @Test
    @DisplayName("should have NONE, RECORDING, and REPLAYING values")
    void shouldHaveExpectedValues() {
      assertEquals(RRConfig.NONE, RRConfig.valueOf("NONE"), "NONE should exist");
      assertEquals(RRConfig.RECORDING, RRConfig.valueOf("RECORDING"), "RECORDING should exist");
      assertEquals(RRConfig.REPLAYING, RRConfig.valueOf("REPLAYING"), "REPLAYING should exist");
    }
  }

  @Nested
  @DisplayName("getRustName Tests")
  class GetRustNameTests {

    @Test
    @DisplayName("NONE should have rust name 'none'")
    void noneShouldHaveCorrectRustName() {
      assertEquals("none", RRConfig.NONE.getRustName(), "NONE rust name should be 'none'");
    }

    @Test
    @DisplayName("RECORDING should have rust name 'recording'")
    void recordingShouldHaveCorrectRustName() {
      assertEquals(
          "recording",
          RRConfig.RECORDING.getRustName(),
          "RECORDING rust name should be 'recording'");
    }

    @Test
    @DisplayName("REPLAYING should have rust name 'replaying'")
    void replayingShouldHaveCorrectRustName() {
      assertEquals(
          "replaying",
          RRConfig.REPLAYING.getRustName(),
          "REPLAYING rust name should be 'replaying'");
    }
  }

  @Nested
  @DisplayName("fromString Tests")
  class FromStringTests {

    @Test
    @DisplayName("should parse 'none' to NONE")
    void shouldParseNone() {
      assertEquals(
          RRConfig.NONE, RRConfig.fromString("none"), "fromString('none') should return NONE");
    }

    @Test
    @DisplayName("should parse 'recording' to RECORDING")
    void shouldParseRecording() {
      assertEquals(
          RRConfig.RECORDING,
          RRConfig.fromString("recording"),
          "fromString('recording') should return RECORDING");
    }

    @Test
    @DisplayName("should parse 'replaying' to REPLAYING")
    void shouldParseReplaying() {
      assertEquals(
          RRConfig.REPLAYING,
          RRConfig.fromString("replaying"),
          "fromString('replaying') should return REPLAYING");
    }

    @Test
    @DisplayName("should throw for null")
    void shouldThrowForNull() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> RRConfig.fromString(null),
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
              () -> RRConfig.fromString("invalid"),
              "Should throw for unknown string");
      assertTrue(
          exception.getMessage().contains("Unknown"),
          "Error should mention unknown, got: " + exception.getMessage());
    }

    @Test
    @DisplayName("should be case sensitive")
    void shouldBeCaseSensitive() {
      assertThrows(
          IllegalArgumentException.class,
          () -> RRConfig.fromString("NONE"),
          "fromString should be case sensitive - 'NONE' should not match 'none'");
    }
  }
}
