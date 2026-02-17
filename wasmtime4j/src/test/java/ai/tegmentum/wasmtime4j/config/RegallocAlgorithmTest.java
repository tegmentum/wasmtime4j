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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the RegallocAlgorithm enum.
 *
 * <p>This test class verifies the enum structure, values, and methods for RegallocAlgorithm using
 * reflection-based testing.
 */
@DisplayName("RegallocAlgorithm Tests")
class RegallocAlgorithmTest {

  // ========================================================================
  // Enum Values Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("RegallocAlgorithm should have 2 values")
    void shouldHaveTwoValues() {
      RegallocAlgorithm[] values = RegallocAlgorithm.values();
      assertEquals(2, values.length, "RegallocAlgorithm should have 2 values");
    }

    @Test
    @DisplayName("RegallocAlgorithm should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames = Set.of("SINGLE_PASS", "BACKTRACKING");
      Set<String> actualNames = new HashSet<>();
      for (RegallocAlgorithm algo : RegallocAlgorithm.values()) {
        actualNames.add(algo.name());
      }
      assertEquals(expectedNames, actualNames, "RegallocAlgorithm should have expected values");
    }

    @Test
    @DisplayName("SINGLE_PASS value should exist")
    void shouldHaveSinglePassValue() {
      RegallocAlgorithm singlePass = RegallocAlgorithm.valueOf("SINGLE_PASS");
      assertNotNull(singlePass, "SINGLE_PASS value should exist");
      assertEquals("SINGLE_PASS", singlePass.name(), "Name should be SINGLE_PASS");
    }

    @Test
    @DisplayName("BACKTRACKING value should exist")
    void shouldHaveBacktrackingValue() {
      RegallocAlgorithm backtracking = RegallocAlgorithm.valueOf("BACKTRACKING");
      assertNotNull(backtracking, "BACKTRACKING value should exist");
      assertEquals("BACKTRACKING", backtracking.name(), "Name should be BACKTRACKING");
    }
  }

  // ========================================================================
  // getValue Tests
  // ========================================================================

  @Nested
  @DisplayName("getValue Tests")
  class GetValueTests {

    @Test
    @DisplayName("SINGLE_PASS should return 'single_pass'")
    void singlePassShouldReturnCorrectValue() {
      assertEquals(
          "single_pass", RegallocAlgorithm.SINGLE_PASS.getValue(), "Value should be 'single_pass'");
    }

    @Test
    @DisplayName("BACKTRACKING should return 'backtracking'")
    void backtrackingShouldReturnCorrectValue() {
      assertEquals(
          "backtracking",
          RegallocAlgorithm.BACKTRACKING.getValue(),
          "Value should be 'backtracking'");
    }
  }

  // ========================================================================
  // fromValue Tests
  // ========================================================================

  @Nested
  @DisplayName("fromValue Tests")
  class FromValueTests {

    @Test
    @DisplayName("fromValue with 'single_pass' should return SINGLE_PASS")
    void fromValueSinglePassShouldReturnCorrectEnum() {
      RegallocAlgorithm algo = RegallocAlgorithm.fromValue("single_pass");
      assertEquals(RegallocAlgorithm.SINGLE_PASS, algo, "Should return SINGLE_PASS");
    }

    @Test
    @DisplayName("fromValue with 'backtracking' should return BACKTRACKING")
    void fromValueBacktrackingShouldReturnCorrectEnum() {
      RegallocAlgorithm algo = RegallocAlgorithm.fromValue("backtracking");
      assertEquals(RegallocAlgorithm.BACKTRACKING, algo, "Should return BACKTRACKING");
    }

    @Test
    @DisplayName("fromValue with unknown value should throw IllegalArgumentException")
    void fromValueUnknownShouldThrow() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> RegallocAlgorithm.fromValue("unknown"),
              "Should throw IllegalArgumentException for unknown value");
      assertTrue(
          exception.getMessage().contains("Unknown regalloc algorithm"),
          "Exception message should mention 'Unknown regalloc algorithm'");
    }
  }

  // ========================================================================
  // toString Tests
  // ========================================================================

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("SINGLE_PASS toString should return 'single_pass'")
    void singlePassToStringShouldReturnValue() {
      assertEquals(
          "single_pass", RegallocAlgorithm.SINGLE_PASS.toString(), "toString should return value");
    }

    @Test
    @DisplayName("BACKTRACKING toString should return 'backtracking'")
    void backtrackingToStringShouldReturnValue() {
      assertEquals(
          "backtracking",
          RegallocAlgorithm.BACKTRACKING.toString(),
          "toString should return value");
    }
  }

  // ========================================================================
  // Ordinal Tests
  // ========================================================================

  @Nested
  @DisplayName("Ordinal Tests")
  class OrdinalTests {

    @Test
    @DisplayName("SINGLE_PASS should have ordinal 0")
    void singlePassShouldHaveOrdinalZero() {
      assertEquals(0, RegallocAlgorithm.SINGLE_PASS.ordinal(), "SINGLE_PASS should have ordinal 0");
    }

    @Test
    @DisplayName("BACKTRACKING should have ordinal 1")
    void backtrackingShouldHaveOrdinalOne() {
      assertEquals(
          1, RegallocAlgorithm.BACKTRACKING.ordinal(), "BACKTRACKING should have ordinal 1");
    }
  }
}
