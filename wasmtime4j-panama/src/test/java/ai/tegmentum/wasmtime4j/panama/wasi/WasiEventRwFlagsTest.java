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

package ai.tegmentum.wasmtime4j.panama.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasiEventRwFlags} enum.
 *
 * <p>This test class verifies WasiEventRwFlags enum values and flag operations.
 */
@DisplayName("WasiEventRwFlags Tests")
class WasiEventRwFlagsTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("WasiEventRwFlags should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasiEventRwFlags.class.isEnum(), "WasiEventRwFlags should be an enum");
    }

    @Test
    @DisplayName("Should have expected number of values")
    void shouldHaveExpectedNumberOfValues() {
      assertEquals(1, WasiEventRwFlags.values().length, "Should have 1 flag");
    }

    @Test
    @DisplayName("All enum values should have unique names")
    void allEnumValuesShouldHaveUniqueNames() {
      final Set<String> names = new HashSet<>();
      for (WasiEventRwFlags flag : WasiEventRwFlags.values()) {
        assertTrue(names.add(flag.name()), "Name should be unique: " + flag.name());
      }
    }

    @Test
    @DisplayName("All enum values should have unique numeric values")
    void allEnumValuesShouldHaveUniqueValues() {
      final Set<Integer> values = new HashSet<>();
      for (WasiEventRwFlags flag : WasiEventRwFlags.values()) {
        assertTrue(values.add(flag.getValue()), "Value should be unique: " + flag.name());
      }
    }
  }

  @Nested
  @DisplayName("Value Tests")
  class ValueTests {

    @Test
    @DisplayName("FD_READWRITE_HANGUP should have value 1")
    void fdReadwriteHangupShouldHaveValue1() {
      assertEquals(
          1,
          WasiEventRwFlags.FD_READWRITE_HANGUP.getValue(),
          "FD_READWRITE_HANGUP should have value 1");
    }
  }

  @Nested
  @DisplayName("combine Tests")
  class CombineTests {

    @Test
    @DisplayName("combine with no flags should return 0")
    void combineWithNoFlagsShouldReturnZero() {
      assertEquals(0, WasiEventRwFlags.combine(), "combine() with no args should return 0");
    }

    @Test
    @DisplayName("combine with single flag should return flag value")
    void combineWithSingleFlagShouldReturnFlagValue() {
      assertEquals(
          1,
          WasiEventRwFlags.combine(WasiEventRwFlags.FD_READWRITE_HANGUP),
          "combine() with single flag should return flag value");
    }

    @Test
    @DisplayName("combine with duplicate flags should return same value")
    void combineWithDuplicateFlagsShouldReturnSameValue() {
      // Combining same flag multiple times should give same result (bitwise OR)
      final int expected = WasiEventRwFlags.FD_READWRITE_HANGUP.getValue();
      assertEquals(
          expected,
          WasiEventRwFlags.combine(
              WasiEventRwFlags.FD_READWRITE_HANGUP, WasiEventRwFlags.FD_READWRITE_HANGUP),
          "Combining same flag twice should be idempotent");
    }
  }

  @Nested
  @DisplayName("contains Tests")
  class ContainsTests {

    @Test
    @DisplayName("contains should return true when flag is present")
    void containsShouldReturnTrueWhenFlagIsPresent() {
      final int mask = WasiEventRwFlags.FD_READWRITE_HANGUP.getValue();
      assertTrue(
          WasiEventRwFlags.contains(mask, WasiEventRwFlags.FD_READWRITE_HANGUP),
          "contains should return true when flag is in mask");
    }

    @Test
    @DisplayName("contains should return false when mask is zero")
    void containsShouldReturnFalseWhenMaskIsZero() {
      assertFalse(
          WasiEventRwFlags.contains(0, WasiEventRwFlags.FD_READWRITE_HANGUP),
          "contains should return false when mask is zero");
    }

    @Test
    @DisplayName("contains should work with combined flags")
    void containsShouldWorkWithCombinedFlags() {
      final int mask = WasiEventRwFlags.combine(WasiEventRwFlags.FD_READWRITE_HANGUP);
      assertTrue(
          WasiEventRwFlags.contains(mask, WasiEventRwFlags.FD_READWRITE_HANGUP),
          "contains should find flag in combined mask");
    }
  }

  @Nested
  @DisplayName("Bitmask Pattern Tests")
  class BitmaskPatternTests {

    @Test
    @DisplayName("Flag values should be powers of two for bitmask operations")
    void flagValuesShouldBePowersOfTwo() {
      for (WasiEventRwFlags flag : WasiEventRwFlags.values()) {
        final int value = flag.getValue();
        assertTrue(value > 0, "Flag value should be positive: " + flag.name());
        assertTrue(
            (value & (value - 1)) == 0,
            "Flag value should be power of 2 for bitwise operations: " + flag.name());
      }
    }

    @Test
    @DisplayName("combine should use bitwise OR")
    void combineShouldUseBitwiseOr() {
      // With only one flag, we verify combine behaves like OR
      final int single = WasiEventRwFlags.FD_READWRITE_HANGUP.getValue();
      final int combined = WasiEventRwFlags.combine(WasiEventRwFlags.FD_READWRITE_HANGUP);
      assertEquals(single, combined, "combine should produce same result as bitwise OR");
    }

    @Test
    @DisplayName("contains should use bitwise AND")
    void containsShouldUseBitwiseAnd() {
      final int flag = WasiEventRwFlags.FD_READWRITE_HANGUP.getValue();
      // Test that contains uses bitwise AND by checking different mask values
      assertTrue(
          WasiEventRwFlags.contains(flag, WasiEventRwFlags.FD_READWRITE_HANGUP),
          "Flag & Flag should be non-zero");
      assertTrue(
          WasiEventRwFlags.contains(3, WasiEventRwFlags.FD_READWRITE_HANGUP),
          "(3 & 1) should be non-zero");
      assertFalse(
          WasiEventRwFlags.contains(2, WasiEventRwFlags.FD_READWRITE_HANGUP),
          "(2 & 1) should be zero");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should be usable in switch statement")
    void shouldBeUsableInSwitchStatement() {
      final WasiEventRwFlags flag = WasiEventRwFlags.FD_READWRITE_HANGUP;

      final String result;
      switch (flag) {
        case FD_READWRITE_HANGUP:
          result = "hangup";
          break;
        default:
          result = "unknown";
          break;
      }

      assertEquals("hangup", result, "Switch should work correctly");
    }

    @Test
    @DisplayName("All values should have non-null names")
    void allValuesShouldHaveNonNullNames() {
      for (WasiEventRwFlags flag : WasiEventRwFlags.values()) {
        assertNotNull(flag.name(), "Name should not be null: " + flag.ordinal());
        assertFalse(flag.name().isEmpty(), "Name should not be empty: " + flag.ordinal());
      }
    }

    @Test
    @DisplayName("Round trip combine/contains should work")
    void roundTripCombineContainsShouldWork() {
      final int mask = WasiEventRwFlags.combine(WasiEventRwFlags.FD_READWRITE_HANGUP);
      assertTrue(
          WasiEventRwFlags.contains(mask, WasiEventRwFlags.FD_READWRITE_HANGUP),
          "Should find flag after combining");
    }
  }
}
