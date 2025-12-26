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

package ai.tegmentum.wasmtime4j.jni.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiEventRwFlags} enum.
 *
 * <p>WasiEventRwFlags defines event read/write flags for WASI polling operations.
 */
@DisplayName("WasiEventRwFlags Enum Tests")
class WasiEventRwFlagsTest {

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have FD_READWRITE_HANGUP flag")
    void shouldHaveFdReadwriteHangupFlag() {
      assertNotNull(WasiEventRwFlags.FD_READWRITE_HANGUP, "Should have FD_READWRITE_HANGUP");
    }

    @Test
    @DisplayName("should have exactly 1 flag")
    void shouldHaveExactlyOneFlag() {
      final WasiEventRwFlags[] values = WasiEventRwFlags.values();
      assertEquals(1, values.length, "Should have exactly 1 flag");
    }
  }

  @Nested
  @DisplayName("Numeric Value Tests")
  class NumericValueTests {

    @Test
    @DisplayName("FD_READWRITE_HANGUP should have value 1")
    void fdReadwriteHangupShouldHaveValueOne() {
      assertEquals(1, WasiEventRwFlags.FD_READWRITE_HANGUP.getValue());
    }
  }

  @Nested
  @DisplayName("Combine Method Tests")
  class CombineMethodTests {

    @Test
    @DisplayName("combine with no flags should return 0")
    void combineWithNoFlagsShouldReturnZero() {
      assertEquals(0, WasiEventRwFlags.combine());
    }

    @Test
    @DisplayName("combine with single flag should return that flag's value")
    void combineWithSingleFlagShouldReturnFlagValue() {
      assertEquals(1, WasiEventRwFlags.combine(WasiEventRwFlags.FD_READWRITE_HANGUP));
    }

    @Test
    @DisplayName("combine with same flag multiple times should still be the same value")
    void combineWithSameFlagMultipleTimesShouldBeIdempotent() {
      final int combined =
          WasiEventRwFlags.combine(
              WasiEventRwFlags.FD_READWRITE_HANGUP, WasiEventRwFlags.FD_READWRITE_HANGUP);
      assertEquals(1, combined, "OR of same flag should be idempotent");
    }
  }

  @Nested
  @DisplayName("Contains Method Tests")
  class ContainsMethodTests {

    @Test
    @DisplayName("contains should return true when flag is present")
    void containsShouldReturnTrueWhenFlagPresent() {
      final int mask = WasiEventRwFlags.combine(WasiEventRwFlags.FD_READWRITE_HANGUP);
      assertTrue(WasiEventRwFlags.contains(mask, WasiEventRwFlags.FD_READWRITE_HANGUP));
    }

    @Test
    @DisplayName("contains should return false for empty mask")
    void containsShouldReturnFalseForEmptyMask() {
      assertFalse(WasiEventRwFlags.contains(0, WasiEventRwFlags.FD_READWRITE_HANGUP));
    }

    @Test
    @DisplayName("contains should return true for mask with multiple bits including flag")
    void containsShouldReturnTrueForMaskWithFlagBit() {
      // Mask with multiple bits set, including bit 0
      final int mask = 0x0F; // Binary: 1111
      assertTrue(WasiEventRwFlags.contains(mask, WasiEventRwFlags.FD_READWRITE_HANGUP));
    }

    @Test
    @DisplayName("contains should return false for mask without flag bit")
    void containsShouldReturnFalseForMaskWithoutFlagBit() {
      // Mask with bits set, but not bit 0
      final int mask = 0x0E; // Binary: 1110
      assertFalse(WasiEventRwFlags.contains(mask, WasiEventRwFlags.FD_READWRITE_HANGUP));
    }
  }

  @Nested
  @DisplayName("Round Trip Tests")
  class RoundTripTests {

    @Test
    @DisplayName("combine and contains should round-trip correctly")
    void combineAndContainsShouldRoundTripCorrectly() {
      for (final WasiEventRwFlags flag : WasiEventRwFlags.values()) {
        final int mask = WasiEventRwFlags.combine(flag);
        assertTrue(
            WasiEventRwFlags.contains(mask, flag), "Round trip should work for " + flag.name());
      }
    }
  }

  @Nested
  @DisplayName("Usage Pattern Tests")
  class UsagePatternTests {

    @Test
    @DisplayName("should be usable in bitwise operations")
    void shouldBeUsableInBitwiseOperations() {
      int flags = 0;

      // Set flag
      flags |= WasiEventRwFlags.FD_READWRITE_HANGUP.getValue();
      assertTrue(
          WasiEventRwFlags.contains(flags, WasiEventRwFlags.FD_READWRITE_HANGUP),
          "Flag should be set");

      // Clear flag
      flags &= ~WasiEventRwFlags.FD_READWRITE_HANGUP.getValue();
      assertFalse(
          WasiEventRwFlags.contains(flags, WasiEventRwFlags.FD_READWRITE_HANGUP),
          "Flag should be cleared");
    }

    @Test
    @DisplayName("should be usable to detect hangup condition")
    void shouldBeUsableToDetectHangupCondition() {
      // Simulating a poll result with hangup
      final int pollFlags = 1;

      assertTrue(
          WasiEventRwFlags.contains(pollFlags, WasiEventRwFlags.FD_READWRITE_HANGUP),
          "Should detect hangup from poll flags");
    }
  }
}
