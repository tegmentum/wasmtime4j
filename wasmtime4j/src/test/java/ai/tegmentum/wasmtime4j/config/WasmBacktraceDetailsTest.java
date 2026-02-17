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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the WasmBacktraceDetails enum.
 *
 * <p>This test class verifies the enum structure, values, and methods for WasmBacktraceDetails
 * using reflection-based testing.
 */
@DisplayName("WasmBacktraceDetails Tests")
class WasmBacktraceDetailsTest {

  // ========================================================================
  // Enum Values Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("WasmBacktraceDetails should have 3 values")
    void shouldHaveThreeValues() {
      WasmBacktraceDetails[] values = WasmBacktraceDetails.values();
      assertEquals(3, values.length, "WasmBacktraceDetails should have 3 values");
    }

    @Test
    @DisplayName("WasmBacktraceDetails should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames = Set.of("DISABLE", "ENABLE", "ENVIRONMENT");
      Set<String> actualNames = new HashSet<>();
      for (WasmBacktraceDetails details : WasmBacktraceDetails.values()) {
        actualNames.add(details.name());
      }
      assertEquals(expectedNames, actualNames, "WasmBacktraceDetails should have expected values");
    }

    @Test
    @DisplayName("DISABLE value should exist")
    void shouldHaveDisableValue() {
      WasmBacktraceDetails disable = WasmBacktraceDetails.valueOf("DISABLE");
      assertNotNull(disable, "DISABLE value should exist");
      assertEquals("DISABLE", disable.name(), "Name should be DISABLE");
    }

    @Test
    @DisplayName("ENABLE value should exist")
    void shouldHaveEnableValue() {
      WasmBacktraceDetails enable = WasmBacktraceDetails.valueOf("ENABLE");
      assertNotNull(enable, "ENABLE value should exist");
      assertEquals("ENABLE", enable.name(), "Name should be ENABLE");
    }

    @Test
    @DisplayName("ENVIRONMENT value should exist")
    void shouldHaveEnvironmentValue() {
      WasmBacktraceDetails environment = WasmBacktraceDetails.valueOf("ENVIRONMENT");
      assertNotNull(environment, "ENVIRONMENT value should exist");
      assertEquals("ENVIRONMENT", environment.name(), "Name should be ENVIRONMENT");
    }
  }

  // ========================================================================
  // getValue Tests
  // ========================================================================

  @Nested
  @DisplayName("getValue Tests")
  class GetValueTests {

    @Test
    @DisplayName("DISABLE should return 0")
    void disableShouldReturnZero() {
      assertEquals(0, WasmBacktraceDetails.DISABLE.getValue(), "Value should be 0");
    }

    @Test
    @DisplayName("ENABLE should return 1")
    void enableShouldReturnOne() {
      assertEquals(1, WasmBacktraceDetails.ENABLE.getValue(), "Value should be 1");
    }

    @Test
    @DisplayName("ENVIRONMENT should return 2")
    void environmentShouldReturnTwo() {
      assertEquals(2, WasmBacktraceDetails.ENVIRONMENT.getValue(), "Value should be 2");
    }
  }

  // ========================================================================
  // fromValue Tests
  // ========================================================================

  @Nested
  @DisplayName("fromValue Tests")
  class FromValueTests {

    @Test
    @DisplayName("fromValue with 0 should return DISABLE")
    void fromValueZeroShouldReturnDisable() {
      WasmBacktraceDetails details = WasmBacktraceDetails.fromValue(0);
      assertEquals(WasmBacktraceDetails.DISABLE, details, "Should return DISABLE");
    }

    @Test
    @DisplayName("fromValue with 1 should return ENABLE")
    void fromValueOneShouldReturnEnable() {
      WasmBacktraceDetails details = WasmBacktraceDetails.fromValue(1);
      assertEquals(WasmBacktraceDetails.ENABLE, details, "Should return ENABLE");
    }

    @Test
    @DisplayName("fromValue with 2 should return ENVIRONMENT")
    void fromValueTwoShouldReturnEnvironment() {
      WasmBacktraceDetails details = WasmBacktraceDetails.fromValue(2);
      assertEquals(WasmBacktraceDetails.ENVIRONMENT, details, "Should return ENVIRONMENT");
    }

    @Test
    @DisplayName("fromValue with unknown value should throw IllegalArgumentException")
    void fromValueUnknownShouldThrow() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> WasmBacktraceDetails.fromValue(99),
              "Should throw IllegalArgumentException for unknown value");
      assertTrue(
          exception.getMessage().contains("Unknown backtrace details value"),
          "Exception message should mention 'Unknown backtrace details value'");
    }
  }

  // ========================================================================
  // isEnabled Tests
  // ========================================================================

  @Nested
  @DisplayName("isEnabled Tests")
  class IsEnabledTests {

    @Test
    @DisplayName("DISABLE isEnabled should return false")
    void disableIsEnabledShouldReturnFalse() {
      assertFalse(WasmBacktraceDetails.DISABLE.isEnabled(), "DISABLE should not be enabled");
    }

    @Test
    @DisplayName("ENABLE isEnabled should return true")
    void enableIsEnabledShouldReturnTrue() {
      assertTrue(WasmBacktraceDetails.ENABLE.isEnabled(), "ENABLE should be enabled");
    }

    @Test
    @DisplayName("ENVIRONMENT isEnabled should return true")
    void environmentIsEnabledShouldReturnTrue() {
      assertTrue(
          WasmBacktraceDetails.ENVIRONMENT.isEnabled(),
          "ENVIRONMENT should be enabled (backtraces may be collected)");
    }
  }

  // ========================================================================
  // Ordinal Tests
  // ========================================================================

  @Nested
  @DisplayName("Ordinal Tests")
  class OrdinalTests {

    @Test
    @DisplayName("DISABLE should have ordinal 0")
    void disableShouldHaveOrdinalZero() {
      assertEquals(0, WasmBacktraceDetails.DISABLE.ordinal(), "DISABLE should have ordinal 0");
    }

    @Test
    @DisplayName("ENABLE should have ordinal 1")
    void enableShouldHaveOrdinalOne() {
      assertEquals(1, WasmBacktraceDetails.ENABLE.ordinal(), "ENABLE should have ordinal 1");
    }

    @Test
    @DisplayName("ENVIRONMENT should have ordinal 2")
    void environmentShouldHaveOrdinalTwo() {
      assertEquals(
          2, WasmBacktraceDetails.ENVIRONMENT.ordinal(), "ENVIRONMENT should have ordinal 2");
    }
  }
}
