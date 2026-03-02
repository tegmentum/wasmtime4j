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

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link WitFunction}. */
@DisplayName("WitFunction Tests")
class WitFunctionTest {

  @Nested
  @DisplayName("Constructor and Getter Tests")
  class ConstructorAndGetterTests {

    @Test
    @DisplayName("constructor should set all fields correctly")
    void constructorShouldSetAllFields() {
      final WitParameter param =
          new WitParameter("input", WitType.createS32(), false, Optional.empty());
      final List<WitParameter> params = List.of(param);
      final List<WitType> returnTypes = List.of(WitType.createString());
      final Optional<String> doc = Optional.of("Adds two numbers");

      final WitFunction func = new WitFunction("add", params, returnTypes, false, doc);

      assertEquals("add", func.getName(), "Name should be 'add'");
      assertEquals(1, func.getParameters().size(), "Should have 1 parameter");
      assertEquals("input", func.getParameters().get(0).getName(), "Param name should match");
      assertEquals(1, func.getReturnTypes().size(), "Should have 1 return type");
      assertEquals("string", func.getReturnTypes().get(0).getName(), "Return type should match");
      assertFalse(func.isAsync(), "Should not be async");
      assertTrue(func.getDocumentation().isPresent(), "Documentation should be present");
      assertEquals("Adds two numbers", func.getDocumentation().get(), "Doc should match");
    }

    @Test
    @DisplayName("isAsync should return true when set")
    void isAsyncShouldReturnTrueWhenSet() {
      final WitFunction func =
          new WitFunction("poll", List.of(), List.of(), true, Optional.empty());

      assertTrue(func.isAsync(), "Should be async");
    }

    @Test
    @DisplayName("empty parameters and return types should be valid")
    void emptyParametersAndReturnTypesShouldBeValid() {
      final WitFunction func =
          new WitFunction("noop", List.of(), List.of(), false, Optional.empty());

      assertNotNull(func.getParameters(), "Parameters should not be null");
      assertTrue(func.getParameters().isEmpty(), "Parameters should be empty");
      assertNotNull(func.getReturnTypes(), "Return types should not be null");
      assertTrue(func.getReturnTypes().isEmpty(), "Return types should be empty");
      assertFalse(func.getDocumentation().isPresent(), "Documentation should be empty");
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
          () -> new WitFunction(null, List.of(), List.of(), false, Optional.empty()),
          "Null name should throw NPE");
    }

    @Test
    @DisplayName("null parameters should throw NullPointerException")
    void nullParametersShouldThrow() {
      assertThrows(
          NullPointerException.class,
          () -> new WitFunction("f", null, List.of(), false, Optional.empty()),
          "Null parameters should throw NPE");
    }

    @Test
    @DisplayName("null return types should throw NullPointerException")
    void nullReturnTypesShouldThrow() {
      assertThrows(
          NullPointerException.class,
          () -> new WitFunction("f", List.of(), null, false, Optional.empty()),
          "Null return types should throw NPE");
    }

    @Test
    @DisplayName("null documentation should throw NullPointerException")
    void nullDocumentationShouldThrow() {
      assertThrows(
          NullPointerException.class,
          () -> new WitFunction("f", List.of(), List.of(), false, null),
          "Null documentation should throw NPE");
    }
  }
}
