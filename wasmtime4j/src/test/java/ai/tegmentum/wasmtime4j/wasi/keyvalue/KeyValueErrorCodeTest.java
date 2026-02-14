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

package ai.tegmentum.wasmtime4j.wasi.keyvalue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for KeyValueErrorCode enum.
 *
 * <p>Verifies enum constants, valueOf, values, and switch coverage for WASI key-value error codes.
 */
@DisplayName("KeyValueErrorCode Tests")
class KeyValueErrorCodeTest {

  private static final Logger LOGGER = Logger.getLogger(KeyValueErrorCodeTest.class.getName());

  private static final Set<String> EXPECTED_CONSTANTS =
      Set.of(
          "UNKNOWN",
          "KEY_NOT_FOUND",
          "KEY_EXISTS",
          "INVALID_KEY",
          "INVALID_VALUE",
          "CAPACITY_EXCEEDED",
          "NOT_PERMITTED",
          "CONNECTION_FAILED",
          "READ_ONLY",
          "TIMEOUT",
          "INTERNAL_ERROR");

  @Test
  @DisplayName("should have exactly 11 enum constants")
  void shouldHaveCorrectConstantCount() {
    LOGGER.info("Testing KeyValueErrorCode constant count");
    assertEquals(
        11, KeyValueErrorCode.values().length, "KeyValueErrorCode should have 11 constants");
  }

  @Test
  @DisplayName("should contain all expected constants")
  void shouldContainAllExpectedConstants() {
    LOGGER.info("Testing KeyValueErrorCode contains all expected constants");
    final Set<String> actual = new HashSet<>();
    for (final KeyValueErrorCode code : KeyValueErrorCode.values()) {
      actual.add(code.name());
    }

    assertEquals(EXPECTED_CONSTANTS, actual, "Should have exactly the expected error code set");
    LOGGER.info("All " + actual.size() + " expected constants verified");
  }

  @Test
  @DisplayName("valueOf should resolve all constants correctly")
  void valueOfShouldResolveAllConstants() {
    LOGGER.info("Testing valueOf for all constants");
    for (final String name : EXPECTED_CONSTANTS) {
      final KeyValueErrorCode code = KeyValueErrorCode.valueOf(name);
      assertNotNull(code, "valueOf('" + name + "') should not return null");
      assertEquals(name, code.name(), "Name should match for " + name);
    }
  }

  @Test
  @DisplayName("valueOf should throw for invalid name")
  void valueOfShouldThrowForInvalidName() {
    assertThrows(
        IllegalArgumentException.class,
        () -> KeyValueErrorCode.valueOf("INVALID"),
        "valueOf('INVALID') should throw IllegalArgumentException");
  }

  @Test
  @DisplayName("ordinals should be sequential starting at 0")
  void ordinalsShouldBeSequential() {
    LOGGER.info("Testing ordinal sequencing");
    final KeyValueErrorCode[] values = KeyValueErrorCode.values();
    for (int i = 0; i < values.length; i++) {
      assertEquals(i, values[i].ordinal(), "Ordinal should be " + i + " for " + values[i]);
    }
  }

  @Test
  @DisplayName("switch statement should cover all constants")
  void switchShouldCoverAllConstants() {
    LOGGER.info("Testing switch coverage");
    for (final KeyValueErrorCode code : KeyValueErrorCode.values()) {
      final String result;
      switch (code) {
        case UNKNOWN:
          result = "unknown";
          break;
        case KEY_NOT_FOUND:
          result = "key_not_found";
          break;
        case KEY_EXISTS:
          result = "key_exists";
          break;
        case INVALID_KEY:
          result = "invalid_key";
          break;
        case INVALID_VALUE:
          result = "invalid_value";
          break;
        case CAPACITY_EXCEEDED:
          result = "capacity_exceeded";
          break;
        case NOT_PERMITTED:
          result = "not_permitted";
          break;
        case CONNECTION_FAILED:
          result = "connection_failed";
          break;
        case READ_ONLY:
          result = "read_only";
          break;
        case TIMEOUT:
          result = "timeout";
          break;
        case INTERNAL_ERROR:
          result = "internal_error";
          break;
        default:
          result = "unhandled";
      }
      assertTrue(
          Arrays.asList(
                  "unknown", "key_not_found", "key_exists", "invalid_key", "invalid_value",
                  "capacity_exceeded", "not_permitted", "connection_failed", "read_only", "timeout",
                  "internal_error")
              .contains(result),
          "Switch should handle " + code + " but got: " + result);
    }
    LOGGER.info("All switch cases verified");
  }
}
