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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link WitParameter}. */
@DisplayName("WitParameter Tests")
class WitParameterTest {

  @Nested
  @DisplayName("Constructor and Getter Tests")
  class ConstructorAndGetterTests {

    @Test
    @DisplayName("constructor should set all fields correctly")
    void constructorShouldSetAllFields() {
      final WitType type = WitType.createS32();
      final Optional<String> doc = Optional.of("The input value");

      final WitParameter param = new WitParameter("count", type, false, doc);

      assertEquals("count", param.getName(), "Name should be 'count'");
      assertEquals(type, param.getType(), "Type should match");
      assertFalse(param.isOptional(), "Should not be optional");
      assertTrue(param.getDocumentation().isPresent(), "Documentation should be present");
      assertEquals("The input value", param.getDocumentation().get(), "Doc should match");
    }

    @Test
    @DisplayName("isOptional should return true when set")
    void isOptionalShouldReturnTrueWhenSet() {
      final WitParameter param =
          new WitParameter("opt", WitType.createString(), true, Optional.empty());

      assertTrue(param.isOptional(), "Should be optional");
    }

    @Test
    @DisplayName("empty documentation should return Optional.empty()")
    void emptyDocumentationShouldReturnEmpty() {
      final WitParameter param = new WitParameter("x", WitType.createU8(), false, Optional.empty());

      assertNotNull(param.getDocumentation(), "Documentation optional should not be null");
      assertFalse(param.getDocumentation().isPresent(), "Documentation should be empty");
    }
  }

  @Nested
  @DisplayName("Null Rejection Tests")
  class NullRejectionTests {

    @Test
    @DisplayName("null name should throw NullPointerException")
    void nullNameShouldThrow() {
      assertThrows(
          NullPointerException.class,
          () -> new WitParameter(null, WitType.createS32(), false, Optional.empty()),
          "Null name should throw NPE");
    }

    @Test
    @DisplayName("null type should throw NullPointerException")
    void nullTypeShouldThrow() {
      assertThrows(
          NullPointerException.class,
          () -> new WitParameter("x", null, false, Optional.empty()),
          "Null type should throw NPE");
    }

    @Test
    @DisplayName("null documentation should throw NullPointerException")
    void nullDocumentationShouldThrow() {
      assertThrows(
          NullPointerException.class,
          () -> new WitParameter("x", WitType.createS32(), false, null),
          "Null documentation should throw NPE");
    }
  }
}
