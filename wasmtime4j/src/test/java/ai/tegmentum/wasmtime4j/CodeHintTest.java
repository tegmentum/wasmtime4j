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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CodeHint} enum.
 *
 * <p>CodeHint indicates the expected type of WebAssembly code (module or component).
 */
@DisplayName("CodeHint Enum Tests")
class CodeHintTest {

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have MODULE and COMPONENT values")
    void shouldHaveExpectedValues() {
      assertNotNull(CodeHint.MODULE, "Should have MODULE");
      assertNotNull(CodeHint.COMPONENT, "Should have COMPONENT");
      assertEquals(2, CodeHint.values().length, "Should have exactly 2 values");
    }

    @Test
    @DisplayName("MODULE should have ordinal 0")
    void moduleShouldHaveOrdinal0() {
      assertEquals(0, CodeHint.MODULE.ordinal());
    }

    @Test
    @DisplayName("COMPONENT should have ordinal 1")
    void componentShouldHaveOrdinal1() {
      assertEquals(1, CodeHint.COMPONENT.ordinal());
    }
  }

  @Nested
  @DisplayName("fromWasmBinaryKind Tests")
  class FromWasmBinaryKindTests {

    @Test
    @DisplayName("MODULE WasmBinaryKind should map to MODULE CodeHint")
    void moduleShouldMap() {
      assertEquals(CodeHint.MODULE, CodeHint.fromWasmBinaryKind(WasmBinaryKind.MODULE));
    }

    @Test
    @DisplayName("COMPONENT WasmBinaryKind should map to COMPONENT CodeHint")
    void componentShouldMap() {
      assertEquals(CodeHint.COMPONENT, CodeHint.fromWasmBinaryKind(WasmBinaryKind.COMPONENT));
    }

    @Test
    @DisplayName("null should throw IllegalArgumentException")
    void nullShouldThrow() {
      assertThrows(IllegalArgumentException.class, () -> CodeHint.fromWasmBinaryKind(null));
    }

    @Test
    @DisplayName("UNKNOWN should throw IllegalArgumentException")
    void unknownShouldThrow() {
      assertThrows(
          IllegalArgumentException.class,
          () -> CodeHint.fromWasmBinaryKind(WasmBinaryKind.UNKNOWN));
    }
  }
}
