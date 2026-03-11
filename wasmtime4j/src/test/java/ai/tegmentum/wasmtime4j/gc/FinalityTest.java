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
package ai.tegmentum.wasmtime4j.gc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Finality} enum.
 *
 * <p>Verifies the WebAssembly GC type finality semantics: final types cannot be subtyped, non-final
 * types may be subtyped.
 */
@DisplayName("Finality Tests")
class FinalityTest {

  @Nested
  @DisplayName("Enum Values")
  class EnumValueTests {

    @Test
    @DisplayName("should have exactly two values")
    void shouldHaveExactlyTwoValues() {
      assertEquals(2, Finality.values().length, "Finality should have exactly 2 values");
    }

    @Test
    @DisplayName("should have FINAL value")
    void shouldHaveFinalValue() {
      assertEquals(Finality.FINAL, Finality.valueOf("FINAL"), "FINAL should exist");
    }

    @Test
    @DisplayName("should have NON_FINAL value")
    void shouldHaveNonFinalValue() {
      assertEquals(Finality.NON_FINAL, Finality.valueOf("NON_FINAL"), "NON_FINAL should exist");
    }
  }

  @Nested
  @DisplayName("allowsSubtyping Method")
  class AllowsSubtypingTests {

    @Test
    @DisplayName("FINAL should not allow subtyping")
    void finalShouldNotAllowSubtyping() {
      assertFalse(Finality.FINAL.allowsSubtyping(), "FINAL types should not allow subtyping");
    }

    @Test
    @DisplayName("NON_FINAL should allow subtyping")
    void nonFinalShouldAllowSubtyping() {
      assertTrue(Finality.NON_FINAL.allowsSubtyping(), "NON_FINAL types should allow subtyping");
    }
  }

  @Nested
  @DisplayName("Integration with StructType")
  class StructTypeIntegrationTests {

    @Test
    @DisplayName("StructType should default to FINAL finality")
    void structTypeShouldDefaultToFinal() {
      final StructType structType =
          StructType.builder("TestStruct").addField("x", FieldType.i32()).build();
      assertEquals(Finality.FINAL, structType.getFinality(), "StructType should default to FINAL");
    }

    @Test
    @DisplayName("StructType should accept NON_FINAL finality")
    void structTypeShouldAcceptNonFinal() {
      final StructType structType =
          StructType.builder("TestStruct")
              .addField("x", FieldType.i32())
              .withFinality(Finality.NON_FINAL)
              .build();
      assertEquals(
          Finality.NON_FINAL, structType.getFinality(), "StructType should accept NON_FINAL");
    }
  }

  @Nested
  @DisplayName("Integration with ArrayType")
  class ArrayTypeIntegrationTests {

    @Test
    @DisplayName("ArrayType should default to FINAL finality")
    void arrayTypeShouldDefaultToFinal() {
      final ArrayType arrayType =
          ArrayType.builder("TestArray").elementType(FieldType.i32()).build();
      assertEquals(Finality.FINAL, arrayType.getFinality(), "ArrayType should default to FINAL");
    }

    @Test
    @DisplayName("ArrayType should accept NON_FINAL finality")
    void arrayTypeShouldAcceptNonFinal() {
      final ArrayType arrayType =
          ArrayType.builder("TestArray")
              .elementType(FieldType.i32())
              .withFinality(Finality.NON_FINAL)
              .build();
      assertEquals(
          Finality.NON_FINAL, arrayType.getFinality(), "ArrayType should accept NON_FINAL");
    }
  }
}
