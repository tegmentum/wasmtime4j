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

package ai.tegmentum.wasmtime4j.wasi.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link EncryptionMode} enum. */
@DisplayName("EncryptionMode Tests")
class EncryptionModeTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(
          EncryptionMode.class.isEnum(),
          "EncryptionMode should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 8 enum values")
    void shouldHaveExactlyEightEnumValues() {
      final EncryptionMode[] values = EncryptionMode.values();

      assertEquals(8, values.length, "EncryptionMode should have exactly 8 enum values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have ECB value")
    void shouldHaveEcbValue() {
      assertNotNull(EncryptionMode.ECB, "ECB should not be null");
    }

    @Test
    @DisplayName("should have CBC value")
    void shouldHaveCbcValue() {
      assertNotNull(EncryptionMode.CBC, "CBC should not be null");
    }

    @Test
    @DisplayName("should have CTR value")
    void shouldHaveCtrValue() {
      assertNotNull(EncryptionMode.CTR, "CTR should not be null");
    }

    @Test
    @DisplayName("should have GCM value")
    void shouldHaveGcmValue() {
      assertNotNull(EncryptionMode.GCM, "GCM should not be null");
    }

    @Test
    @DisplayName("should have CCM value")
    void shouldHaveCcmValue() {
      assertNotNull(EncryptionMode.CCM, "CCM should not be null");
    }

    @Test
    @DisplayName("should have OCB value")
    void shouldHaveOcbValue() {
      assertNotNull(EncryptionMode.OCB, "OCB should not be null");
    }

    @Test
    @DisplayName("should have SIV value")
    void shouldHaveSivValue() {
      assertNotNull(EncryptionMode.SIV, "SIV should not be null");
    }

    @Test
    @DisplayName("should have GCM_SIV value")
    void shouldHaveGcmSivValue() {
      assertNotNull(EncryptionMode.GCM_SIV, "GCM_SIV should not be null");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("should have unique ordinals for all values")
    void shouldHaveUniqueOrdinalsForAllValues() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final EncryptionMode mode : EncryptionMode.values()) {
        ordinals.add(mode.ordinal());
      }

      assertEquals(
          EncryptionMode.values().length,
          ordinals.size(),
          "All enum values should have unique ordinals");
    }

    @Test
    @DisplayName("should have sequential ordinals starting at 0")
    void shouldHaveSequentialOrdinalsStartingAtZero() {
      final EncryptionMode[] values = EncryptionMode.values();

      for (int i = 0; i < values.length; i++) {
        assertEquals(
            i,
            values[i].ordinal(),
            "Ordinal should be " + i + " for " + values[i]);
      }
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("should round-trip all values through valueOf")
    void shouldRoundTripAllValuesThroughValueOf() {
      for (final EncryptionMode mode : EncryptionMode.values()) {
        assertEquals(
            mode,
            EncryptionMode.valueOf(mode.name()),
            "valueOf should round-trip for " + mode.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowIllegalArgumentExceptionForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> EncryptionMode.valueOf("INVALID_MODE"),
          "valueOf should throw IllegalArgumentException for invalid name");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("should return new array on each call to values()")
    void shouldReturnNewArrayOnEachCallToValues() {
      final EncryptionMode[] first = EncryptionMode.values();
      final EncryptionMode[] second = EncryptionMode.values();

      assertNotSame(first, second, "values() should return a new array on each call");
      assertArrayEquals(
          first,
          second,
          "values() arrays should contain the same elements");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return name from toString for all values")
    void shouldReturnNameFromToStringForAllValues() {
      for (final EncryptionMode mode : EncryptionMode.values()) {
        assertEquals(
            mode.name(),
            mode.toString(),
            "toString should return name for " + mode.name());
      }
    }
  }

  @Nested
  @DisplayName("GetModeName Tests")
  class GetModeNameTests {

    @Test
    @DisplayName("should return ECB for ECB")
    void shouldReturnCorrectNameForEcb() {
      assertEquals(
          "ECB",
          EncryptionMode.ECB.getModeName(),
          "ECB should have mode name ECB");
    }

    @Test
    @DisplayName("should return CBC for CBC")
    void shouldReturnCorrectNameForCbc() {
      assertEquals(
          "CBC",
          EncryptionMode.CBC.getModeName(),
          "CBC should have mode name CBC");
    }

    @Test
    @DisplayName("should return CTR for CTR")
    void shouldReturnCorrectNameForCtr() {
      assertEquals(
          "CTR",
          EncryptionMode.CTR.getModeName(),
          "CTR should have mode name CTR");
    }

    @Test
    @DisplayName("should return GCM for GCM")
    void shouldReturnCorrectNameForGcm() {
      assertEquals(
          "GCM",
          EncryptionMode.GCM.getModeName(),
          "GCM should have mode name GCM");
    }

    @Test
    @DisplayName("should return CCM for CCM")
    void shouldReturnCorrectNameForCcm() {
      assertEquals(
          "CCM",
          EncryptionMode.CCM.getModeName(),
          "CCM should have mode name CCM");
    }

    @Test
    @DisplayName("should return OCB for OCB")
    void shouldReturnCorrectNameForOcb() {
      assertEquals(
          "OCB",
          EncryptionMode.OCB.getModeName(),
          "OCB should have mode name OCB");
    }

    @Test
    @DisplayName("should return SIV for SIV")
    void shouldReturnCorrectNameForSiv() {
      assertEquals(
          "SIV",
          EncryptionMode.SIV.getModeName(),
          "SIV should have mode name SIV");
    }

    @Test
    @DisplayName("should return GCM-SIV for GCM_SIV")
    void shouldReturnCorrectNameForGcmSiv() {
      assertEquals(
          "GCM-SIV",
          EncryptionMode.GCM_SIV.getModeName(),
          "GCM_SIV should have mode name GCM-SIV");
    }
  }

  @Nested
  @DisplayName("RequiresIv Tests")
  class RequiresIvTests {

    @Test
    @DisplayName("should return false for ECB")
    void shouldReturnFalseForEcb() {
      assertFalse(
          EncryptionMode.ECB.requiresIv(),
          "ECB should not require an IV");
    }

    @Test
    @DisplayName("should return true for CBC")
    void shouldReturnTrueForCbc() {
      assertTrue(
          EncryptionMode.CBC.requiresIv(),
          "CBC should require an IV");
    }

    @Test
    @DisplayName("should return true for CTR")
    void shouldReturnTrueForCtr() {
      assertTrue(
          EncryptionMode.CTR.requiresIv(),
          "CTR should require an IV");
    }

    @Test
    @DisplayName("should return true for GCM")
    void shouldReturnTrueForGcm() {
      assertTrue(
          EncryptionMode.GCM.requiresIv(),
          "GCM should require an IV");
    }

    @Test
    @DisplayName("should return true for CCM")
    void shouldReturnTrueForCcm() {
      assertTrue(
          EncryptionMode.CCM.requiresIv(),
          "CCM should require an IV");
    }

    @Test
    @DisplayName("should return true for OCB")
    void shouldReturnTrueForOcb() {
      assertTrue(
          EncryptionMode.OCB.requiresIv(),
          "OCB should require an IV");
    }

    @Test
    @DisplayName("should return true for SIV")
    void shouldReturnTrueForSiv() {
      assertTrue(
          EncryptionMode.SIV.requiresIv(),
          "SIV should require an IV");
    }

    @Test
    @DisplayName("should return true for GCM_SIV")
    void shouldReturnTrueForGcmSiv() {
      assertTrue(
          EncryptionMode.GCM_SIV.requiresIv(),
          "GCM_SIV should require an IV");
    }
  }

  @Nested
  @DisplayName("ProvidesAuthentication Tests")
  class ProvidesAuthenticationTests {

    @Test
    @DisplayName("should return false for ECB")
    void shouldReturnFalseForEcb() {
      assertFalse(
          EncryptionMode.ECB.providesAuthentication(),
          "ECB should not provide authentication");
    }

    @Test
    @DisplayName("should return false for CBC")
    void shouldReturnFalseForCbc() {
      assertFalse(
          EncryptionMode.CBC.providesAuthentication(),
          "CBC should not provide authentication");
    }

    @Test
    @DisplayName("should return false for CTR")
    void shouldReturnFalseForCtr() {
      assertFalse(
          EncryptionMode.CTR.providesAuthentication(),
          "CTR should not provide authentication");
    }

    @Test
    @DisplayName("should return true for GCM")
    void shouldReturnTrueForGcm() {
      assertTrue(
          EncryptionMode.GCM.providesAuthentication(),
          "GCM should provide authentication");
    }

    @Test
    @DisplayName("should return true for CCM")
    void shouldReturnTrueForCcm() {
      assertTrue(
          EncryptionMode.CCM.providesAuthentication(),
          "CCM should provide authentication");
    }

    @Test
    @DisplayName("should return true for OCB")
    void shouldReturnTrueForOcb() {
      assertTrue(
          EncryptionMode.OCB.providesAuthentication(),
          "OCB should provide authentication");
    }

    @Test
    @DisplayName("should return true for SIV")
    void shouldReturnTrueForSiv() {
      assertTrue(
          EncryptionMode.SIV.providesAuthentication(),
          "SIV should provide authentication");
    }

    @Test
    @DisplayName("should return true for GCM_SIV")
    void shouldReturnTrueForGcmSiv() {
      assertTrue(
          EncryptionMode.GCM_SIV.providesAuthentication(),
          "GCM_SIV should provide authentication");
    }
  }
}
