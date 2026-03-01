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
package ai.tegmentum.wasmtime4j.wit;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitEnum} class.
 *
 * <p>WitEnum represents a WIT enum value (discriminated choice without payload).
 */
@DisplayName("WitEnum Tests")
class WitEnumTest {

  @Nested
  @DisplayName("Factory Method Null Validation Tests")
  class FactoryMethodNullTests {

    @Test
    @DisplayName("of with null discriminant should throw IllegalArgumentException")
    void ofWithNullDiscriminantShouldThrow() {
      final var enumType =
          WitType.enumType("color", java.util.Arrays.asList("red", "green", "blue"));
      assertThrows(
          IllegalArgumentException.class,
          () -> WitEnum.of(enumType, null),
          "of with null discriminant should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("of with empty discriminant should throw IllegalArgumentException")
    void ofWithEmptyDiscriminantShouldThrow() {
      final var enumType =
          WitType.enumType("color", java.util.Arrays.asList("red", "green", "blue"));
      assertThrows(
          IllegalArgumentException.class,
          () -> WitEnum.of(enumType, ""),
          "of with empty discriminant should throw IllegalArgumentException");
    }
  }
}
