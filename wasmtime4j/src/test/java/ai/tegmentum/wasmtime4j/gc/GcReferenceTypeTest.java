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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GcReferenceType} enum.
 *
 * <p>GcReferenceType represents the hierarchy of reference types in the WebAssembly GC proposal.
 */
@DisplayName("GcReferenceType Enum Tests")
class GcReferenceTypeTest {

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have ALL expected enum values")
    void shouldHaveAllExpectedEnumValues() {
      assertNotNull(GcReferenceType.ANY_REF, "Should have ANY_REF");
      assertNotNull(GcReferenceType.EQ_REF, "Should have EQ_REF");
      assertNotNull(GcReferenceType.I31_REF, "Should have I31_REF");
      assertNotNull(GcReferenceType.STRUCT_REF, "Should have STRUCT_REF");
      assertNotNull(GcReferenceType.ARRAY_REF, "Should have ARRAY_REF");
      assertNotNull(GcReferenceType.NULL_REF, "Should have NULL_REF");
      assertNotNull(GcReferenceType.NULL_FUNC_REF, "Should have NULL_FUNC_REF");
      assertNotNull(GcReferenceType.NULL_EXTERN_REF, "Should have NULL_EXTERN_REF");
    }

    @Test
    @DisplayName("should have correct total count of enum values")
    void shouldHaveCorrectTotalCountOfEnumValues() {
      final GcReferenceType[] values = GcReferenceType.values();
      assertEquals(9, values.length, "Should have exactly 9 reference types");
    }
  }

  @Nested
  @DisplayName("WebAssembly Name Tests")
  class WasmNameTests {

    @Test
    @DisplayName("ANY_REF should have correct wasm name")
    void anyRefShouldHaveCorrectWasmName() {
      assertEquals("anyref", GcReferenceType.ANY_REF.getWasmName());
    }

    @Test
    @DisplayName("EQ_REF should have correct wasm name")
    void eqRefShouldHaveCorrectWasmName() {
      assertEquals("eqref", GcReferenceType.EQ_REF.getWasmName());
    }

    @Test
    @DisplayName("I31_REF should have correct wasm name")
    void i31RefShouldHaveCorrectWasmName() {
      assertEquals("i31ref", GcReferenceType.I31_REF.getWasmName());
    }

    @Test
    @DisplayName("STRUCT_REF should have correct wasm name")
    void structRefShouldHaveCorrectWasmName() {
      assertEquals("structref", GcReferenceType.STRUCT_REF.getWasmName());
    }

    @Test
    @DisplayName("ARRAY_REF should have correct wasm name")
    void arrayRefShouldHaveCorrectWasmName() {
      assertEquals("arrayref", GcReferenceType.ARRAY_REF.getWasmName());
    }

    @Test
    @DisplayName("NULL_REF should have correct wasm name")
    void nullRefShouldHaveCorrectWasmName() {
      assertEquals("nullref", GcReferenceType.NULL_REF.getWasmName());
    }

    @Test
    @DisplayName("toString should return wasm name")
    void toStringShouldReturnWasmName() {
      for (final GcReferenceType type : GcReferenceType.values()) {
        assertEquals(
            type.getWasmName(),
            type.toString(),
            "toString should return wasm name for " + type.name());
      }
    }
  }

  @Nested
  @DisplayName("Subtype Relationship Tests")
  class SubtypeRelationshipTests {

    @Test
    @DisplayName("every type should be subtype of itself")
    void everyTypeShouldBeSubtypeOfItself() {
      for (final GcReferenceType type : GcReferenceType.values()) {
        assertTrue(type.isSubtypeOf(type), type + " should be subtype of itself");
      }
    }

    @Test
    @DisplayName("EQ_REF should be subtype of ANY_REF")
    void eqRefShouldBeSubtypeOfAnyRef() {
      assertTrue(
          GcReferenceType.EQ_REF.isSubtypeOf(GcReferenceType.ANY_REF),
          "EQ_REF should be subtype of ANY_REF");
    }

    @Test
    @DisplayName("I31_REF should be subtype of EQ_REF and ANY_REF")
    void i31RefShouldBeSubtypeOfEqRefAndAnyRef() {
      assertTrue(
          GcReferenceType.I31_REF.isSubtypeOf(GcReferenceType.EQ_REF),
          "I31_REF should be subtype of EQ_REF");
      assertTrue(
          GcReferenceType.I31_REF.isSubtypeOf(GcReferenceType.ANY_REF),
          "I31_REF should be subtype of ANY_REF");
    }

    @Test
    @DisplayName("STRUCT_REF should be subtype of EQ_REF and ANY_REF")
    void structRefShouldBeSubtypeOfEqRefAndAnyRef() {
      assertTrue(
          GcReferenceType.STRUCT_REF.isSubtypeOf(GcReferenceType.EQ_REF),
          "STRUCT_REF should be subtype of EQ_REF");
      assertTrue(
          GcReferenceType.STRUCT_REF.isSubtypeOf(GcReferenceType.ANY_REF),
          "STRUCT_REF should be subtype of ANY_REF");
    }

    @Test
    @DisplayName("ARRAY_REF should be subtype of EQ_REF and ANY_REF")
    void arrayRefShouldBeSubtypeOfEqRefAndAnyRef() {
      assertTrue(
          GcReferenceType.ARRAY_REF.isSubtypeOf(GcReferenceType.EQ_REF),
          "ARRAY_REF should be subtype of EQ_REF");
      assertTrue(
          GcReferenceType.ARRAY_REF.isSubtypeOf(GcReferenceType.ANY_REF),
          "ARRAY_REF should be subtype of ANY_REF");
    }

    @Test
    @DisplayName("NULL_REF should be subtype of all nullable GC types")
    void nullRefShouldBeSubtypeOfAllNullableGcTypes() {
      assertTrue(
          GcReferenceType.NULL_REF.isSubtypeOf(GcReferenceType.ANY_REF),
          "NULL_REF should be subtype of ANY_REF");
      assertTrue(
          GcReferenceType.NULL_REF.isSubtypeOf(GcReferenceType.EQ_REF),
          "NULL_REF should be subtype of EQ_REF");
      assertTrue(
          GcReferenceType.NULL_REF.isSubtypeOf(GcReferenceType.I31_REF),
          "NULL_REF should be subtype of I31_REF");
      assertTrue(
          GcReferenceType.NULL_REF.isSubtypeOf(GcReferenceType.STRUCT_REF),
          "NULL_REF should be subtype of STRUCT_REF");
      assertTrue(
          GcReferenceType.NULL_REF.isSubtypeOf(GcReferenceType.ARRAY_REF),
          "NULL_REF should be subtype of ARRAY_REF");
    }

    @Test
    @DisplayName("ANY_REF should not be subtype of EQ_REF")
    void anyRefShouldNotBeSubtypeOfEqRef() {
      assertFalse(
          GcReferenceType.ANY_REF.isSubtypeOf(GcReferenceType.EQ_REF),
          "ANY_REF should NOT be subtype of EQ_REF");
    }

    @Test
    @DisplayName("STRUCT_REF should not be subtype of ARRAY_REF")
    void structRefShouldNotBeSubtypeOfArrayRef() {
      assertFalse(
          GcReferenceType.STRUCT_REF.isSubtypeOf(GcReferenceType.ARRAY_REF),
          "STRUCT_REF should NOT be subtype of ARRAY_REF");
    }
  }

  @Nested
  @DisplayName("Equality Support Tests")
  class EqualitySupportTests {

    @Test
    @DisplayName("EQ_REF should support equality")
    void eqRefShouldSupportEquality() {
      assertTrue(GcReferenceType.EQ_REF.supportsEquality(), "EQ_REF should support equality");
    }

    @Test
    @DisplayName("I31_REF should support equality")
    void i31RefShouldSupportEquality() {
      assertTrue(GcReferenceType.I31_REF.supportsEquality(), "I31_REF should support equality");
    }

    @Test
    @DisplayName("STRUCT_REF should support equality")
    void structRefShouldSupportEquality() {
      assertTrue(
          GcReferenceType.STRUCT_REF.supportsEquality(), "STRUCT_REF should support equality");
    }

    @Test
    @DisplayName("ARRAY_REF should support equality")
    void arrayRefShouldSupportEquality() {
      assertTrue(GcReferenceType.ARRAY_REF.supportsEquality(), "ARRAY_REF should support equality");
    }

    @Test
    @DisplayName("ANY_REF should not support equality")
    void anyRefShouldNotSupportEquality() {
      assertFalse(
          GcReferenceType.ANY_REF.supportsEquality(), "ANY_REF should NOT support equality");
    }

    @Test
    @DisplayName("NULL_FUNC_REF should not support equality")
    void nullFuncRefShouldNotSupportEquality() {
      assertFalse(
          GcReferenceType.NULL_FUNC_REF.supportsEquality(),
          "NULL_FUNC_REF should NOT support equality");
    }

    @Test
    @DisplayName("NULL_EXTERN_REF should not support equality")
    void nullExternRefShouldNotSupportEquality() {
      assertFalse(
          GcReferenceType.NULL_EXTERN_REF.supportsEquality(),
          "NULL_EXTERN_REF should NOT support equality");
    }
  }
}
