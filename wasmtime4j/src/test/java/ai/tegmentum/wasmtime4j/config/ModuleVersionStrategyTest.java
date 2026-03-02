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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ModuleVersionStrategy} enum.
 *
 * <p>Verifies enum values for module version validation during deserialization.
 */
@DisplayName("ModuleVersionStrategy Tests")
class ModuleVersionStrategyTest {

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have exactly three values")
    void shouldHaveThreeValues() {
      assertEquals(
          3,
          ModuleVersionStrategy.values().length,
          "ModuleVersionStrategy should have exactly 3 values");
    }

    @Test
    @DisplayName("should have WASMTIME_VERSION value")
    void shouldHaveWasmtimeVersion() {
      assertEquals(
          ModuleVersionStrategy.WASMTIME_VERSION,
          ModuleVersionStrategy.valueOf("WASMTIME_VERSION"),
          "WASMTIME_VERSION should exist");
    }

    @Test
    @DisplayName("should have NONE value")
    void shouldHaveNone() {
      assertEquals(
          ModuleVersionStrategy.NONE, ModuleVersionStrategy.valueOf("NONE"), "NONE should exist");
    }

    @Test
    @DisplayName("should have CUSTOM value")
    void shouldHaveCustom() {
      assertEquals(
          ModuleVersionStrategy.CUSTOM,
          ModuleVersionStrategy.valueOf("CUSTOM"),
          "CUSTOM should exist");
    }

    @Test
    @DisplayName("should maintain ordinal order")
    void shouldMaintainOrdinalOrder() {
      final ModuleVersionStrategy[] values = ModuleVersionStrategy.values();
      assertEquals(
          ModuleVersionStrategy.WASMTIME_VERSION,
          values[0],
          "First value should be WASMTIME_VERSION");
      assertEquals(ModuleVersionStrategy.NONE, values[1], "Second value should be NONE");
      assertEquals(ModuleVersionStrategy.CUSTOM, values[2], "Third value should be CUSTOM");
    }
  }
}
