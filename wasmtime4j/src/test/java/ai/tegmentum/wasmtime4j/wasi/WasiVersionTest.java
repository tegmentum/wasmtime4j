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

package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasiVersion} enum.
 *
 * <p>Verifies WASI version values, capability flags, static factory methods, version string
 * parsing, compatibility checks, and string representations.
 */
@DisplayName("WasiVersion Tests")
class WasiVersionTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("WasiVersion should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasiVersion.class.isEnum(), "WasiVersion should be an enum");
    }

    @Test
    @DisplayName("WasiVersion should have exactly 2 values")
    void shouldHaveExactlyTwoValues() {
      assertEquals(2, WasiVersion.values().length, "Should have exactly 2 version values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have PREVIEW_1 value")
    void shouldHavePreview1Value() {
      assertNotNull(WasiVersion.PREVIEW_1, "PREVIEW_1 should exist");
      assertEquals("PREVIEW_1", WasiVersion.PREVIEW_1.name(), "Name should be PREVIEW_1");
    }

    @Test
    @DisplayName("should have PREVIEW_2 value")
    void shouldHavePreview2Value() {
      assertNotNull(WasiVersion.PREVIEW_2, "PREVIEW_2 should exist");
      assertEquals("PREVIEW_2", WasiVersion.PREVIEW_2.name(), "Name should be PREVIEW_2");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("ordinals should be unique")
    void ordinalsShouldBeUnique() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final WasiVersion version : WasiVersion.values()) {
        assertTrue(
            ordinals.add(version.ordinal()), "Ordinal should be unique: " + version.ordinal());
      }
    }

    @Test
    @DisplayName("ordinals should be sequential starting at 0")
    void ordinalsShouldBeSequential() {
      final WasiVersion[] values = WasiVersion.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal(), "Ordinal should match index for " + values[i]);
      }
    }
  }

  @Nested
  @DisplayName("valueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("valueOf should return correct constant for each name")
    void valueOfShouldReturnCorrectConstant() {
      assertEquals(
          WasiVersion.PREVIEW_1, WasiVersion.valueOf("PREVIEW_1"), "Should return PREVIEW_1");
      assertEquals(
          WasiVersion.PREVIEW_2, WasiVersion.valueOf("PREVIEW_2"), "Should return PREVIEW_2");
    }

    @Test
    @DisplayName("valueOf should throw for invalid name")
    void valueOfShouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiVersion.valueOf("INVALID"),
          "Should throw for invalid enum name");
    }
  }

  @Nested
  @DisplayName("values() Tests")
  class ValuesTests {

    @Test
    @DisplayName("values() should return all enum constants")
    void valuesShouldReturnAllEnumConstants() {
      final WasiVersion[] values = WasiVersion.values();
      final Set<WasiVersion> valueSet = new HashSet<>(Arrays.asList(values));

      assertTrue(valueSet.contains(WasiVersion.PREVIEW_1), "Should contain PREVIEW_1");
      assertTrue(valueSet.contains(WasiVersion.PREVIEW_2), "Should contain PREVIEW_2");
    }

    @Test
    @DisplayName("values() should return new array each time")
    void valuesShouldReturnNewArrayEachTime() {
      final WasiVersion[] first = WasiVersion.values();
      final WasiVersion[] second = WasiVersion.values();

      assertTrue(first != second, "Should return new array each time");
      assertEquals(first.length, second.length, "Arrays should have same length");
    }
  }

  @Nested
  @DisplayName("GetVersion Tests")
  class GetVersionTests {

    @Test
    @DisplayName("PREVIEW_1 should have version '0.1.0'")
    void preview1ShouldHaveCorrectVersion() {
      assertEquals(
          "0.1.0", WasiVersion.PREVIEW_1.getVersion(), "PREVIEW_1 version should be '0.1.0'");
    }

    @Test
    @DisplayName("PREVIEW_2 should have version '0.2.0'")
    void preview2ShouldHaveCorrectVersion() {
      assertEquals(
          "0.2.0", WasiVersion.PREVIEW_2.getVersion(), "PREVIEW_2 version should be '0.2.0'");
    }
  }

  @Nested
  @DisplayName("GetImportNamespace Tests")
  class GetImportNamespaceTests {

    @Test
    @DisplayName("PREVIEW_1 should have import namespace 'wasi_unstable'")
    void preview1ShouldHaveCorrectImportNamespace() {
      assertEquals(
          "wasi_unstable",
          WasiVersion.PREVIEW_1.getImportNamespace(),
          "PREVIEW_1 import namespace should be 'wasi_unstable'");
    }

    @Test
    @DisplayName("PREVIEW_2 should have import namespace 'wasi'")
    void preview2ShouldHaveCorrectImportNamespace() {
      assertEquals(
          "wasi",
          WasiVersion.PREVIEW_2.getImportNamespace(),
          "PREVIEW_2 import namespace should be 'wasi'");
    }
  }

  @Nested
  @DisplayName("Boolean Method Tests")
  class BooleanMethodTests {

    @Test
    @DisplayName("PREVIEW_1 should not support async operations")
    void preview1ShouldNotSupportAsyncOperations() {
      assertFalse(
          WasiVersion.PREVIEW_1.supportsAsyncOperations(),
          "PREVIEW_1.supportsAsyncOperations() should be false");
    }

    @Test
    @DisplayName("PREVIEW_2 should support async operations")
    void preview2ShouldSupportAsyncOperations() {
      assertTrue(
          WasiVersion.PREVIEW_2.supportsAsyncOperations(),
          "PREVIEW_2.supportsAsyncOperations() should be true");
    }

    @Test
    @DisplayName("PREVIEW_1 should not support component model")
    void preview1ShouldNotSupportComponentModel() {
      assertFalse(
          WasiVersion.PREVIEW_1.supportsComponentModel(),
          "PREVIEW_1.supportsComponentModel() should be false");
    }

    @Test
    @DisplayName("PREVIEW_2 should support component model")
    void preview2ShouldSupportComponentModel() {
      assertTrue(
          WasiVersion.PREVIEW_2.supportsComponentModel(),
          "PREVIEW_2.supportsComponentModel() should be true");
    }

    @Test
    @DisplayName("PREVIEW_1 should not support WIT interfaces")
    void preview1ShouldNotSupportWitInterfaces() {
      assertFalse(
          WasiVersion.PREVIEW_1.supportsWitInterfaces(),
          "PREVIEW_1.supportsWitInterfaces() should be false");
    }

    @Test
    @DisplayName("PREVIEW_2 should support WIT interfaces")
    void preview2ShouldSupportWitInterfaces() {
      assertTrue(
          WasiVersion.PREVIEW_2.supportsWitInterfaces(),
          "PREVIEW_2.supportsWitInterfaces() should be true");
    }

    @Test
    @DisplayName("PREVIEW_1 should not support stream operations")
    void preview1ShouldNotSupportStreamOperations() {
      assertFalse(
          WasiVersion.PREVIEW_1.supportsStreamOperations(),
          "PREVIEW_1.supportsStreamOperations() should be false");
    }

    @Test
    @DisplayName("PREVIEW_2 should support stream operations")
    void preview2ShouldSupportStreamOperations() {
      assertTrue(
          WasiVersion.PREVIEW_2.supportsStreamOperations(),
          "PREVIEW_2.supportsStreamOperations() should be true");
    }

    @Test
    @DisplayName("PREVIEW_1 should not support HTTP operations")
    void preview1ShouldNotSupportHttpOperations() {
      assertFalse(
          WasiVersion.PREVIEW_1.supportsHttpOperations(),
          "PREVIEW_1.supportsHttpOperations() should be false");
    }

    @Test
    @DisplayName("PREVIEW_2 should support HTTP operations")
    void preview2ShouldSupportHttpOperations() {
      assertTrue(
          WasiVersion.PREVIEW_2.supportsHttpOperations(),
          "PREVIEW_2.supportsHttpOperations() should be true");
    }
  }

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("getDefault should return PREVIEW_1")
    void getDefaultShouldReturnPreview1() {
      assertSame(
          WasiVersion.PREVIEW_1, WasiVersion.getDefault(), "getDefault() should return PREVIEW_1");
    }

    @Test
    @DisplayName("getLatest should return PREVIEW_2")
    void getLatestShouldReturnPreview2() {
      assertSame(
          WasiVersion.PREVIEW_2, WasiVersion.getLatest(), "getLatest() should return PREVIEW_2");
    }
  }

  @Nested
  @DisplayName("FromVersionString Tests")
  class FromVersionStringTests {

    @Test
    @DisplayName("should resolve '0.1.0' to PREVIEW_1")
    void shouldResolvePreview1VersionString() {
      assertEquals(
          WasiVersion.PREVIEW_1,
          WasiVersion.fromVersionString("0.1.0"),
          "Version string '0.1.0' should resolve to PREVIEW_1");
    }

    @Test
    @DisplayName("should resolve '0.2.0' to PREVIEW_2")
    void shouldResolvePreview2VersionString() {
      assertEquals(
          WasiVersion.PREVIEW_2,
          WasiVersion.fromVersionString("0.2.0"),
          "Version string '0.2.0' should resolve to PREVIEW_2");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid version string")
    void shouldThrowForInvalidVersionString() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiVersion.fromVersionString("0.3.0"),
          "Should throw for invalid version string '0.3.0'");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for null version string")
    void shouldThrowForNullVersionString() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiVersion.fromVersionString(null),
          "Should throw for null version string");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for empty version string")
    void shouldThrowForEmptyVersionString() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiVersion.fromVersionString(""),
          "Should throw for empty version string");
    }
  }

  @Nested
  @DisplayName("IsCompatibleWith Tests")
  class IsCompatibleWithTests {

    @Test
    @DisplayName("PREVIEW_1 should be compatible with itself")
    void preview1ShouldBeCompatibleWithItself() {
      assertTrue(
          WasiVersion.PREVIEW_1.isCompatibleWith(WasiVersion.PREVIEW_1),
          "PREVIEW_1 should be compatible with PREVIEW_1");
    }

    @Test
    @DisplayName("PREVIEW_2 should be compatible with itself")
    void preview2ShouldBeCompatibleWithItself() {
      assertTrue(
          WasiVersion.PREVIEW_2.isCompatibleWith(WasiVersion.PREVIEW_2),
          "PREVIEW_2 should be compatible with PREVIEW_2");
    }

    @Test
    @DisplayName("PREVIEW_1 should not be compatible with PREVIEW_2")
    void preview1ShouldNotBeCompatibleWithPreview2() {
      assertFalse(
          WasiVersion.PREVIEW_1.isCompatibleWith(WasiVersion.PREVIEW_2),
          "PREVIEW_1 should not be compatible with PREVIEW_2");
    }

    @Test
    @DisplayName("PREVIEW_2 should not be compatible with PREVIEW_1")
    void preview2ShouldNotBeCompatibleWithPreview1() {
      assertFalse(
          WasiVersion.PREVIEW_2.isCompatibleWith(WasiVersion.PREVIEW_1),
          "PREVIEW_2 should not be compatible with PREVIEW_1");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("PREVIEW_1 toString should follow 'WASI version (name)' format")
    void preview1ToStringShouldReturnCorrectFormat() {
      assertEquals(
          "WASI 0.1.0 (PREVIEW_1)",
          WasiVersion.PREVIEW_1.toString(),
          "PREVIEW_1 toString should be 'WASI 0.1.0 (PREVIEW_1)'");
    }

    @Test
    @DisplayName("PREVIEW_2 toString should follow 'WASI version (name)' format")
    void preview2ToStringShouldReturnCorrectFormat() {
      assertEquals(
          "WASI 0.2.0 (PREVIEW_2)",
          WasiVersion.PREVIEW_2.toString(),
          "PREVIEW_2 toString should be 'WASI 0.2.0 (PREVIEW_2)'");
    }
  }
}
