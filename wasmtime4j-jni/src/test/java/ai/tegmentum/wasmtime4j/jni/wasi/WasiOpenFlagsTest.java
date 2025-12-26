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
 * Tests for {@link WasiOpenFlags} enum.
 *
 * <p>WasiOpenFlags defines file open flags for WASI path_open operations.
 */
@DisplayName("WasiOpenFlags Enum Tests")
class WasiOpenFlagsTest {

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have all expected open flags")
    void shouldHaveAllExpectedOpenFlags() {
      assertNotNull(WasiOpenFlags.CREAT, "Should have CREAT");
      assertNotNull(WasiOpenFlags.DIRECTORY, "Should have DIRECTORY");
      assertNotNull(WasiOpenFlags.EXCL, "Should have EXCL");
      assertNotNull(WasiOpenFlags.TRUNC, "Should have TRUNC");
    }

    @Test
    @DisplayName("should have exactly 4 open flags")
    void shouldHaveExactlyFourOpenFlags() {
      final WasiOpenFlags[] values = WasiOpenFlags.values();
      assertEquals(4, values.length, "Should have exactly 4 open flags");
    }
  }

  @Nested
  @DisplayName("Numeric Value Tests")
  class NumericValueTests {

    @Test
    @DisplayName("CREAT should have value 1")
    void creatShouldHaveValueOne() {
      assertEquals(1, WasiOpenFlags.CREAT.getValue());
    }

    @Test
    @DisplayName("DIRECTORY should have value 2")
    void directoryShouldHaveValueTwo() {
      assertEquals(2, WasiOpenFlags.DIRECTORY.getValue());
    }

    @Test
    @DisplayName("EXCL should have value 4")
    void exclShouldHaveValueFour() {
      assertEquals(4, WasiOpenFlags.EXCL.getValue());
    }

    @Test
    @DisplayName("TRUNC should have value 8")
    void truncShouldHaveValueEight() {
      assertEquals(8, WasiOpenFlags.TRUNC.getValue());
    }

    @Test
    @DisplayName("values should be powers of 2 for bitwise operations")
    void valuesShouldBePowersOfTwo() {
      for (final WasiOpenFlags flag : WasiOpenFlags.values()) {
        final int value = flag.getValue();
        assertTrue(value > 0 && (value & (value - 1)) == 0,
            flag.name() + " value should be a power of 2");
      }
    }
  }

  @Nested
  @DisplayName("Combine Method Tests")
  class CombineMethodTests {

    @Test
    @DisplayName("combine with no flags should return 0")
    void combineWithNoFlagsShouldReturnZero() {
      assertEquals(0, WasiOpenFlags.combine());
    }

    @Test
    @DisplayName("combine with single flag should return that flag's value")
    void combineWithSingleFlagShouldReturnFlagValue() {
      assertEquals(1, WasiOpenFlags.combine(WasiOpenFlags.CREAT));
      assertEquals(2, WasiOpenFlags.combine(WasiOpenFlags.DIRECTORY));
      assertEquals(4, WasiOpenFlags.combine(WasiOpenFlags.EXCL));
      assertEquals(8, WasiOpenFlags.combine(WasiOpenFlags.TRUNC));
    }

    @Test
    @DisplayName("combine with multiple flags should OR their values")
    void combineWithMultipleFlagsShouldOrValues() {
      final int combined = WasiOpenFlags.combine(WasiOpenFlags.CREAT, WasiOpenFlags.EXCL);
      assertEquals(5, combined, "CREAT(1) | EXCL(4) should be 5");
    }

    @Test
    @DisplayName("combine with all flags should OR all values")
    void combineWithAllFlagsShouldOrAllValues() {
      final int combined = WasiOpenFlags.combine(
          WasiOpenFlags.CREAT,
          WasiOpenFlags.DIRECTORY,
          WasiOpenFlags.EXCL,
          WasiOpenFlags.TRUNC);
      assertEquals(15, combined, "All flags combined should be 15");
    }
  }

  @Nested
  @DisplayName("Contains Method Tests")
  class ContainsMethodTests {

    @Test
    @DisplayName("contains should return true when flag is present")
    void containsShouldReturnTrueWhenFlagPresent() {
      final int mask = WasiOpenFlags.combine(WasiOpenFlags.CREAT, WasiOpenFlags.TRUNC);
      assertTrue(WasiOpenFlags.contains(mask, WasiOpenFlags.CREAT));
      assertTrue(WasiOpenFlags.contains(mask, WasiOpenFlags.TRUNC));
    }

    @Test
    @DisplayName("contains should return false when flag is absent")
    void containsShouldReturnFalseWhenFlagAbsent() {
      final int mask = WasiOpenFlags.combine(WasiOpenFlags.CREAT, WasiOpenFlags.TRUNC);
      assertFalse(WasiOpenFlags.contains(mask, WasiOpenFlags.DIRECTORY));
      assertFalse(WasiOpenFlags.contains(mask, WasiOpenFlags.EXCL));
    }

    @Test
    @DisplayName("contains should return false for empty mask")
    void containsShouldReturnFalseForEmptyMask() {
      assertFalse(WasiOpenFlags.contains(0, WasiOpenFlags.CREAT));
      assertFalse(WasiOpenFlags.contains(0, WasiOpenFlags.DIRECTORY));
    }

    @Test
    @DisplayName("contains should work with all flags mask")
    void containsShouldWorkWithAllFlagsMask() {
      final int allFlags = 15; // All flags combined
      for (final WasiOpenFlags flag : WasiOpenFlags.values()) {
        assertTrue(WasiOpenFlags.contains(allFlags, flag),
            "All flags mask should contain " + flag.name());
      }
    }
  }

  @Nested
  @DisplayName("Round Trip Tests")
  class RoundTripTests {

    @Test
    @DisplayName("combine and contains should round-trip correctly")
    void combineAndContainsShouldRoundTripCorrectly() {
      for (final WasiOpenFlags flag : WasiOpenFlags.values()) {
        final int mask = WasiOpenFlags.combine(flag);
        assertTrue(WasiOpenFlags.contains(mask, flag),
            "Round trip should work for " + flag.name());
      }
    }
  }
}
