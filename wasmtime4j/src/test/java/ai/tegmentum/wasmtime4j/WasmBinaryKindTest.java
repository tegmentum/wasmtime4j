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
package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasmBinaryKind} enum.
 *
 * <p>Verifies detection of WebAssembly module vs component vs unknown binary formats.
 */
@DisplayName("WasmBinaryKind Tests")
class WasmBinaryKindTest {

  /** WASM magic bytes: \0asm. */
  private static final byte[] WASM_MAGIC = {0x00, 0x61, 0x73, 0x6D};

  /** Module version bytes: 1.0.0.0. */
  private static final byte[] MODULE_VERSION = {0x01, 0x00, 0x00, 0x00};

  /** Component version bytes: 0x0d 0x00 0x01 0x00. */
  private static final byte[] COMPONENT_VERSION = {0x0d, 0x00, 0x01, 0x00};

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have exactly three values")
    void shouldHaveThreeValues() {
      final WasmBinaryKind[] values = WasmBinaryKind.values();
      assertEquals(3, values.length, "WasmBinaryKind should have exactly 3 values");
    }

    @Test
    @DisplayName("should have MODULE, COMPONENT, and UNKNOWN values")
    void shouldHaveExpectedValues() {
      assertEquals(WasmBinaryKind.MODULE, WasmBinaryKind.valueOf("MODULE"), "MODULE should exist");
      assertEquals(
          WasmBinaryKind.COMPONENT, WasmBinaryKind.valueOf("COMPONENT"), "COMPONENT should exist");
      assertEquals(
          WasmBinaryKind.UNKNOWN, WasmBinaryKind.valueOf("UNKNOWN"), "UNKNOWN should exist");
    }
  }

  @Nested
  @DisplayName("Module Detection Tests")
  class ModuleDetectionTests {

    @Test
    @DisplayName("should detect valid module header")
    void shouldDetectModule() {
      final byte[] moduleBytes = concat(WASM_MAGIC, MODULE_VERSION);
      final WasmBinaryKind result = WasmBinaryKind.detect(moduleBytes);

      assertEquals(
          WasmBinaryKind.MODULE,
          result,
          "Should detect MODULE for valid module header, got: " + result);
    }

    @Test
    @DisplayName("should detect module with extra trailing bytes")
    void shouldDetectModuleWithTrailingBytes() {
      final byte[] moduleBytes = new byte[100];
      System.arraycopy(WASM_MAGIC, 0, moduleBytes, 0, 4);
      System.arraycopy(MODULE_VERSION, 0, moduleBytes, 4, 4);

      assertEquals(
          WasmBinaryKind.MODULE,
          WasmBinaryKind.detect(moduleBytes),
          "Should detect MODULE even with trailing bytes");
    }
  }

  @Nested
  @DisplayName("Component Detection Tests")
  class ComponentDetectionTests {

    @Test
    @DisplayName("should detect valid component header")
    void shouldDetectComponent() {
      final byte[] componentBytes = concat(WASM_MAGIC, COMPONENT_VERSION);
      final WasmBinaryKind result = WasmBinaryKind.detect(componentBytes);

      assertEquals(
          WasmBinaryKind.COMPONENT,
          result,
          "Should detect COMPONENT for valid component header, got: " + result);
    }
  }

  @Nested
  @DisplayName("Unknown Detection Tests")
  class UnknownDetectionTests {

    @Test
    @DisplayName("should return UNKNOWN for short bytes")
    void shouldReturnUnknownForShortBytes() {
      final byte[] shortBytes = {0x00, 0x61, 0x73};
      assertEquals(
          WasmBinaryKind.UNKNOWN,
          WasmBinaryKind.detect(shortBytes),
          "Should return UNKNOWN for bytes shorter than 8");
    }

    @Test
    @DisplayName("should return UNKNOWN for exactly 7 bytes")
    void shouldReturnUnknownForSevenBytes() {
      final byte[] sevenBytes = {0x00, 0x61, 0x73, 0x6D, 0x01, 0x00, 0x00};
      assertEquals(
          WasmBinaryKind.UNKNOWN,
          WasmBinaryKind.detect(sevenBytes),
          "Should return UNKNOWN for exactly 7 bytes");
    }

    @Test
    @DisplayName("should return UNKNOWN for empty bytes")
    void shouldReturnUnknownForEmptyBytes() {
      assertEquals(
          WasmBinaryKind.UNKNOWN,
          WasmBinaryKind.detect(new byte[0]),
          "Should return UNKNOWN for empty byte array");
    }

    @Test
    @DisplayName("should return UNKNOWN for wrong magic bytes")
    void shouldReturnUnknownForWrongMagic() {
      final byte[] wrongMagic = {0x01, 0x02, 0x03, 0x04, 0x01, 0x00, 0x00, 0x00};
      assertEquals(
          WasmBinaryKind.UNKNOWN,
          WasmBinaryKind.detect(wrongMagic),
          "Should return UNKNOWN for wrong magic bytes");
    }

    @Test
    @DisplayName("should return UNKNOWN for correct magic but unknown version")
    void shouldReturnUnknownForUnknownVersion() {
      final byte[] unknownVersion = {
        0x00, 0x61, 0x73, 0x6D, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
      };
      assertEquals(
          WasmBinaryKind.UNKNOWN,
          WasmBinaryKind.detect(unknownVersion),
          "Should return UNKNOWN for correct magic but unrecognized version");
    }
  }

  @Nested
  @DisplayName("Null Handling Tests")
  class NullHandlingTests {

    @Test
    @DisplayName("should throw for null bytes")
    void shouldThrowForNull() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> WasmBinaryKind.detect(null),
              "Should throw for null bytes");
      assertTrue(
          exception.getMessage().contains("null"),
          "Error message should mention null, got: " + exception.getMessage());
    }
  }

  private static byte[] concat(final byte[] first, final byte[] second) {
    final byte[] result = new byte[first.length + second.length];
    System.arraycopy(first, 0, result, 0, first.length);
    System.arraycopy(second, 0, result, first.length, second.length);
    return result;
  }
}
