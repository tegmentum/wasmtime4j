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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiVersion} enum.
 *
 * <p>WasiVersion defines the available WASI versions with different capabilities.
 */
@DisplayName("WasiVersion Tests")
class WasiVersionTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should have exactly 2 values")
    void shouldHaveExactly2Values() {
      final WasiVersion[] values = WasiVersion.values();
      assertEquals(2, values.length, "WasiVersion should have 2 values");
    }

    @Test
    @DisplayName("should have PREVIEW_1")
    void shouldHavePreview1() {
      assertNotNull(WasiVersion.valueOf("PREVIEW_1"), "PREVIEW_1 should exist");
    }

    @Test
    @DisplayName("should have PREVIEW_2")
    void shouldHavePreview2() {
      assertNotNull(WasiVersion.valueOf("PREVIEW_2"), "PREVIEW_2 should exist");
    }

    @Test
    @DisplayName("values should be in expected order")
    void valuesShouldBeInExpectedOrder() {
      final WasiVersion[] values = WasiVersion.values();
      assertEquals(WasiVersion.PREVIEW_1, values[0], "First should be PREVIEW_1");
      assertEquals(WasiVersion.PREVIEW_2, values[1], "Second should be PREVIEW_2");
    }
  }

  @Nested
  @DisplayName("getVersion Method Tests")
  class GetVersionMethodTests {

    @Test
    @DisplayName("PREVIEW_1 should have version 0.1.0")
    void preview1ShouldHaveVersion010() {
      assertEquals(
          "0.1.0", WasiVersion.PREVIEW_1.getVersion(), "PREVIEW_1 version should be 0.1.0");
    }

    @Test
    @DisplayName("PREVIEW_2 should have version 0.2.0")
    void preview2ShouldHaveVersion020() {
      assertEquals(
          "0.2.0", WasiVersion.PREVIEW_2.getVersion(), "PREVIEW_2 version should be 0.2.0");
    }
  }

  @Nested
  @DisplayName("getImportNamespace Method Tests")
  class GetImportNamespaceMethodTests {

    @Test
    @DisplayName("PREVIEW_1 should have import namespace wasi_unstable")
    void preview1ShouldHaveImportNamespaceWasiUnstable() {
      assertEquals(
          "wasi_unstable",
          WasiVersion.PREVIEW_1.getImportNamespace(),
          "PREVIEW_1 import namespace should be wasi_unstable");
    }

    @Test
    @DisplayName("PREVIEW_2 should have import namespace wasi")
    void preview2ShouldHaveImportNamespaceWasi() {
      assertEquals(
          "wasi",
          WasiVersion.PREVIEW_2.getImportNamespace(),
          "PREVIEW_2 import namespace should be wasi");
    }
  }

  @Nested
  @DisplayName("supportsAsyncOperations Method Tests")
  class SupportsAsyncOperationsMethodTests {

    @Test
    @DisplayName("PREVIEW_1 should not support async operations")
    void preview1ShouldNotSupportAsyncOperations() {
      assertFalse(
          WasiVersion.PREVIEW_1.supportsAsyncOperations(),
          "PREVIEW_1 should not support async operations");
    }

    @Test
    @DisplayName("PREVIEW_2 should support async operations")
    void preview2ShouldSupportAsyncOperations() {
      assertTrue(
          WasiVersion.PREVIEW_2.supportsAsyncOperations(),
          "PREVIEW_2 should support async operations");
    }
  }

  @Nested
  @DisplayName("supportsComponentModel Method Tests")
  class SupportsComponentModelMethodTests {

    @Test
    @DisplayName("PREVIEW_1 should not support component model")
    void preview1ShouldNotSupportComponentModel() {
      assertFalse(
          WasiVersion.PREVIEW_1.supportsComponentModel(),
          "PREVIEW_1 should not support component model");
    }

    @Test
    @DisplayName("PREVIEW_2 should support component model")
    void preview2ShouldSupportComponentModel() {
      assertTrue(
          WasiVersion.PREVIEW_2.supportsComponentModel(),
          "PREVIEW_2 should support component model");
    }
  }

  @Nested
  @DisplayName("supportsWitInterfaces Method Tests")
  class SupportsWitInterfacesMethodTests {

    @Test
    @DisplayName("PREVIEW_1 should not support WIT interfaces")
    void preview1ShouldNotSupportWitInterfaces() {
      assertFalse(
          WasiVersion.PREVIEW_1.supportsWitInterfaces(),
          "PREVIEW_1 should not support WIT interfaces");
    }

    @Test
    @DisplayName("PREVIEW_2 should support WIT interfaces")
    void preview2ShouldSupportWitInterfaces() {
      assertTrue(
          WasiVersion.PREVIEW_2.supportsWitInterfaces(), "PREVIEW_2 should support WIT interfaces");
    }
  }

  @Nested
  @DisplayName("supportsStreamOperations Method Tests")
  class SupportsStreamOperationsMethodTests {

    @Test
    @DisplayName("PREVIEW_1 should not support stream operations")
    void preview1ShouldNotSupportStreamOperations() {
      assertFalse(
          WasiVersion.PREVIEW_1.supportsStreamOperations(),
          "PREVIEW_1 should not support stream operations");
    }

    @Test
    @DisplayName("PREVIEW_2 should support stream operations")
    void preview2ShouldSupportStreamOperations() {
      assertTrue(
          WasiVersion.PREVIEW_2.supportsStreamOperations(),
          "PREVIEW_2 should support stream operations");
    }
  }

  @Nested
  @DisplayName("supportsHttpOperations Method Tests")
  class SupportsHttpOperationsMethodTests {

    @Test
    @DisplayName("PREVIEW_1 should not support HTTP operations")
    void preview1ShouldNotSupportHttpOperations() {
      assertFalse(
          WasiVersion.PREVIEW_1.supportsHttpOperations(),
          "PREVIEW_1 should not support HTTP operations");
    }

    @Test
    @DisplayName("PREVIEW_2 should support HTTP operations")
    void preview2ShouldSupportHttpOperations() {
      assertTrue(
          WasiVersion.PREVIEW_2.supportsHttpOperations(),
          "PREVIEW_2 should support HTTP operations");
    }
  }

  @Nested
  @DisplayName("getDefault Static Method Tests")
  class GetDefaultStaticMethodTests {

    @Test
    @DisplayName("getDefault should return PREVIEW_1")
    void getDefaultShouldReturnPreview1() {
      assertEquals(
          WasiVersion.PREVIEW_1,
          WasiVersion.getDefault(),
          "Default version should be PREVIEW_1 for compatibility");
    }
  }

  @Nested
  @DisplayName("getLatest Static Method Tests")
  class GetLatestStaticMethodTests {

    @Test
    @DisplayName("getLatest should return PREVIEW_2")
    void getLatestShouldReturnPreview2() {
      assertEquals(
          WasiVersion.PREVIEW_2, WasiVersion.getLatest(), "Latest version should be PREVIEW_2");
    }
  }

  @Nested
  @DisplayName("fromVersionString Static Method Tests")
  class FromVersionStringStaticMethodTests {

    @Test
    @DisplayName("fromVersionString should parse 0.1.0")
    void fromVersionStringShouldParse010() {
      assertEquals(
          WasiVersion.PREVIEW_1,
          WasiVersion.fromVersionString("0.1.0"),
          "0.1.0 should parse to PREVIEW_1");
    }

    @Test
    @DisplayName("fromVersionString should parse 0.2.0")
    void fromVersionStringShouldParse020() {
      assertEquals(
          WasiVersion.PREVIEW_2,
          WasiVersion.fromVersionString("0.2.0"),
          "0.2.0 should parse to PREVIEW_2");
    }

    @Test
    @DisplayName("fromVersionString should trim whitespace")
    void fromVersionStringShouldTrimWhitespace() {
      assertEquals(
          WasiVersion.PREVIEW_1,
          WasiVersion.fromVersionString("  0.1.0  "),
          "Should trim whitespace");
    }

    @Test
    @DisplayName("fromVersionString should throw for null")
    void fromVersionStringShouldThrowForNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiVersion.fromVersionString(null),
          "Should throw for null");
    }

    @Test
    @DisplayName("fromVersionString should throw for empty string")
    void fromVersionStringShouldThrowForEmptyString() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiVersion.fromVersionString(""),
          "Should throw for empty string");
    }

    @Test
    @DisplayName("fromVersionString should throw for whitespace only")
    void fromVersionStringShouldThrowForWhitespaceOnly() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiVersion.fromVersionString("   "),
          "Should throw for whitespace only");
    }

    @Test
    @DisplayName("fromVersionString should throw for unknown version")
    void fromVersionStringShouldThrowForUnknownVersion() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> WasiVersion.fromVersionString("0.3.0"),
              "Should throw for unknown version");
      assertTrue(
          exception.getMessage().contains("0.3.0"), "Exception should contain the unknown version");
    }
  }

  @Nested
  @DisplayName("isCompatibleWith Method Tests")
  class IsCompatibleWithMethodTests {

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
  @DisplayName("toString Method Tests")
  class ToStringMethodTests {

    @Test
    @DisplayName("PREVIEW_1 toString should include version and name")
    void preview1ToStringShouldIncludeVersionAndName() {
      final String str = WasiVersion.PREVIEW_1.toString();
      assertTrue(str.contains("0.1.0"), "Should contain version 0.1.0");
      assertTrue(str.contains("PREVIEW_1"), "Should contain PREVIEW_1");
    }

    @Test
    @DisplayName("PREVIEW_2 toString should include version and name")
    void preview2ToStringShouldIncludeVersionAndName() {
      final String str = WasiVersion.PREVIEW_2.toString();
      assertTrue(str.contains("0.2.0"), "Should contain version 0.2.0");
      assertTrue(str.contains("PREVIEW_2"), "Should contain PREVIEW_2");
    }
  }
}
