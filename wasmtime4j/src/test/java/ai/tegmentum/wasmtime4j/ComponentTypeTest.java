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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentType} enum.
 *
 * <p>ComponentType represents all possible type kinds in the Component Model.
 */
@DisplayName("ComponentType Tests")
class ComponentTypeTest {

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have all 22 component types")
    void shouldHaveAllComponentTypes() {
      final var types = ComponentType.values();
      assertEquals(22, types.length, "Should have 22 component types");
    }

    @Test
    @DisplayName("should have BOOL type")
    void shouldHaveBoolType() {
      assertEquals(ComponentType.BOOL, ComponentType.valueOf("BOOL"));
    }

    @Test
    @DisplayName("should have signed integer types")
    void shouldHaveSignedIntegerTypes() {
      assertEquals(ComponentType.S8, ComponentType.valueOf("S8"));
      assertEquals(ComponentType.S16, ComponentType.valueOf("S16"));
      assertEquals(ComponentType.S32, ComponentType.valueOf("S32"));
      assertEquals(ComponentType.S64, ComponentType.valueOf("S64"));
    }

    @Test
    @DisplayName("should have unsigned integer types")
    void shouldHaveUnsignedIntegerTypes() {
      assertEquals(ComponentType.U8, ComponentType.valueOf("U8"));
      assertEquals(ComponentType.U16, ComponentType.valueOf("U16"));
      assertEquals(ComponentType.U32, ComponentType.valueOf("U32"));
      assertEquals(ComponentType.U64, ComponentType.valueOf("U64"));
    }

    @Test
    @DisplayName("should have floating point types")
    void shouldHaveFloatingPointTypes() {
      assertEquals(ComponentType.F32, ComponentType.valueOf("F32"));
      assertEquals(ComponentType.F64, ComponentType.valueOf("F64"));
    }

    @Test
    @DisplayName("should have CHAR type")
    void shouldHaveCharType() {
      assertEquals(ComponentType.CHAR, ComponentType.valueOf("CHAR"));
    }

    @Test
    @DisplayName("should have STRING type")
    void shouldHaveStringType() {
      assertEquals(ComponentType.STRING, ComponentType.valueOf("STRING"));
    }

    @Test
    @DisplayName("should have compound types")
    void shouldHaveCompoundTypes() {
      assertEquals(ComponentType.LIST, ComponentType.valueOf("LIST"));
      assertEquals(ComponentType.RECORD, ComponentType.valueOf("RECORD"));
      assertEquals(ComponentType.TUPLE, ComponentType.valueOf("TUPLE"));
      assertEquals(ComponentType.VARIANT, ComponentType.valueOf("VARIANT"));
      assertEquals(ComponentType.ENUM, ComponentType.valueOf("ENUM"));
      assertEquals(ComponentType.OPTION, ComponentType.valueOf("OPTION"));
      assertEquals(ComponentType.RESULT, ComponentType.valueOf("RESULT"));
      assertEquals(ComponentType.FLAGS, ComponentType.valueOf("FLAGS"));
    }

    @Test
    @DisplayName("should have resource handle types")
    void shouldHaveResourceHandleTypes() {
      assertEquals(ComponentType.OWN, ComponentType.valueOf("OWN"));
      assertEquals(ComponentType.BORROW, ComponentType.valueOf("BORROW"));
    }
  }

  @Nested
  @DisplayName("isPrimitive Tests")
  class IsPrimitiveTests {

    @Test
    @DisplayName("BOOL should be primitive")
    void boolShouldBePrimitive() {
      assertTrue(ComponentType.BOOL.isPrimitive(), "BOOL should be primitive");
    }

    @Test
    @DisplayName("signed integers should be primitive")
    void signedIntegersShouldBePrimitive() {
      assertTrue(ComponentType.S8.isPrimitive(), "S8 should be primitive");
      assertTrue(ComponentType.S16.isPrimitive(), "S16 should be primitive");
      assertTrue(ComponentType.S32.isPrimitive(), "S32 should be primitive");
      assertTrue(ComponentType.S64.isPrimitive(), "S64 should be primitive");
    }

    @Test
    @DisplayName("unsigned integers should be primitive")
    void unsignedIntegersShouldBePrimitive() {
      assertTrue(ComponentType.U8.isPrimitive(), "U8 should be primitive");
      assertTrue(ComponentType.U16.isPrimitive(), "U16 should be primitive");
      assertTrue(ComponentType.U32.isPrimitive(), "U32 should be primitive");
      assertTrue(ComponentType.U64.isPrimitive(), "U64 should be primitive");
    }

    @Test
    @DisplayName("floating point types should be primitive")
    void floatingPointTypesShouldBePrimitive() {
      assertTrue(ComponentType.F32.isPrimitive(), "F32 should be primitive");
      assertTrue(ComponentType.F64.isPrimitive(), "F64 should be primitive");
    }

    @Test
    @DisplayName("CHAR should be primitive")
    void charShouldBePrimitive() {
      assertTrue(ComponentType.CHAR.isPrimitive(), "CHAR should be primitive");
    }

    @Test
    @DisplayName("STRING should be primitive")
    void stringShouldBePrimitive() {
      assertTrue(ComponentType.STRING.isPrimitive(), "STRING should be primitive");
    }

    @Test
    @DisplayName("compound types should not be primitive")
    void compoundTypesShouldNotBePrimitive() {
      assertFalse(ComponentType.LIST.isPrimitive(), "LIST should not be primitive");
      assertFalse(ComponentType.RECORD.isPrimitive(), "RECORD should not be primitive");
      assertFalse(ComponentType.TUPLE.isPrimitive(), "TUPLE should not be primitive");
      assertFalse(ComponentType.VARIANT.isPrimitive(), "VARIANT should not be primitive");
      assertFalse(ComponentType.ENUM.isPrimitive(), "ENUM should not be primitive");
      assertFalse(ComponentType.OPTION.isPrimitive(), "OPTION should not be primitive");
      assertFalse(ComponentType.RESULT.isPrimitive(), "RESULT should not be primitive");
      assertFalse(ComponentType.FLAGS.isPrimitive(), "FLAGS should not be primitive");
    }

    @Test
    @DisplayName("resource types should not be primitive")
    void resourceTypesShouldNotBePrimitive() {
      assertFalse(ComponentType.OWN.isPrimitive(), "OWN should not be primitive");
      assertFalse(ComponentType.BORROW.isPrimitive(), "BORROW should not be primitive");
    }
  }

  @Nested
  @DisplayName("isInteger Tests")
  class IsIntegerTests {

    @Test
    @DisplayName("signed integers should be integers")
    void signedIntegersShouldBeIntegers() {
      assertTrue(ComponentType.S8.isInteger(), "S8 should be integer");
      assertTrue(ComponentType.S16.isInteger(), "S16 should be integer");
      assertTrue(ComponentType.S32.isInteger(), "S32 should be integer");
      assertTrue(ComponentType.S64.isInteger(), "S64 should be integer");
    }

    @Test
    @DisplayName("unsigned integers should be integers")
    void unsignedIntegersShouldBeIntegers() {
      assertTrue(ComponentType.U8.isInteger(), "U8 should be integer");
      assertTrue(ComponentType.U16.isInteger(), "U16 should be integer");
      assertTrue(ComponentType.U32.isInteger(), "U32 should be integer");
      assertTrue(ComponentType.U64.isInteger(), "U64 should be integer");
    }

    @Test
    @DisplayName("non-integer types should not be integers")
    void nonIntegerTypesShouldNotBeIntegers() {
      assertFalse(ComponentType.BOOL.isInteger(), "BOOL should not be integer");
      assertFalse(ComponentType.F32.isInteger(), "F32 should not be integer");
      assertFalse(ComponentType.F64.isInteger(), "F64 should not be integer");
      assertFalse(ComponentType.CHAR.isInteger(), "CHAR should not be integer");
      assertFalse(ComponentType.STRING.isInteger(), "STRING should not be integer");
      assertFalse(ComponentType.LIST.isInteger(), "LIST should not be integer");
    }
  }

  @Nested
  @DisplayName("isSigned Tests")
  class IsSignedTests {

    @Test
    @DisplayName("signed integers should be signed")
    void signedIntegersShouldBeSigned() {
      assertTrue(ComponentType.S8.isSigned(), "S8 should be signed");
      assertTrue(ComponentType.S16.isSigned(), "S16 should be signed");
      assertTrue(ComponentType.S32.isSigned(), "S32 should be signed");
      assertTrue(ComponentType.S64.isSigned(), "S64 should be signed");
    }

    @Test
    @DisplayName("unsigned integers should not be signed")
    void unsignedIntegersShouldNotBeSigned() {
      assertFalse(ComponentType.U8.isSigned(), "U8 should not be signed");
      assertFalse(ComponentType.U16.isSigned(), "U16 should not be signed");
      assertFalse(ComponentType.U32.isSigned(), "U32 should not be signed");
      assertFalse(ComponentType.U64.isSigned(), "U64 should not be signed");
    }

    @Test
    @DisplayName("non-integer types should not be signed")
    void nonIntegerTypesShouldNotBeSigned() {
      assertFalse(ComponentType.BOOL.isSigned(), "BOOL should not be signed");
      assertFalse(ComponentType.F32.isSigned(), "F32 should not be signed");
      assertFalse(ComponentType.STRING.isSigned(), "STRING should not be signed");
    }
  }

  @Nested
  @DisplayName("isUnsigned Tests")
  class IsUnsignedTests {

    @Test
    @DisplayName("unsigned integers should be unsigned")
    void unsignedIntegersShouldBeUnsigned() {
      assertTrue(ComponentType.U8.isUnsigned(), "U8 should be unsigned");
      assertTrue(ComponentType.U16.isUnsigned(), "U16 should be unsigned");
      assertTrue(ComponentType.U32.isUnsigned(), "U32 should be unsigned");
      assertTrue(ComponentType.U64.isUnsigned(), "U64 should be unsigned");
    }

    @Test
    @DisplayName("signed integers should not be unsigned")
    void signedIntegersShouldNotBeUnsigned() {
      assertFalse(ComponentType.S8.isUnsigned(), "S8 should not be unsigned");
      assertFalse(ComponentType.S16.isUnsigned(), "S16 should not be unsigned");
      assertFalse(ComponentType.S32.isUnsigned(), "S32 should not be unsigned");
      assertFalse(ComponentType.S64.isUnsigned(), "S64 should not be unsigned");
    }

    @Test
    @DisplayName("non-integer types should not be unsigned")
    void nonIntegerTypesShouldNotBeUnsigned() {
      assertFalse(ComponentType.BOOL.isUnsigned(), "BOOL should not be unsigned");
      assertFalse(ComponentType.F32.isUnsigned(), "F32 should not be unsigned");
      assertFalse(ComponentType.STRING.isUnsigned(), "STRING should not be unsigned");
    }
  }

  @Nested
  @DisplayName("isFloat Tests")
  class IsFloatTests {

    @Test
    @DisplayName("floating point types should be float")
    void floatingPointTypesShouldBeFloat() {
      assertTrue(ComponentType.F32.isFloat(), "F32 should be float");
      assertTrue(ComponentType.F64.isFloat(), "F64 should be float");
    }

    @Test
    @DisplayName("non-floating types should not be float")
    void nonFloatingTypesShouldNotBeFloat() {
      assertFalse(ComponentType.BOOL.isFloat(), "BOOL should not be float");
      assertFalse(ComponentType.S32.isFloat(), "S32 should not be float");
      assertFalse(ComponentType.U64.isFloat(), "U64 should not be float");
      assertFalse(ComponentType.CHAR.isFloat(), "CHAR should not be float");
      assertFalse(ComponentType.STRING.isFloat(), "STRING should not be float");
      assertFalse(ComponentType.LIST.isFloat(), "LIST should not be float");
    }
  }

  @Nested
  @DisplayName("isCompound Tests")
  class IsCompoundTests {

    @Test
    @DisplayName("compound types should be compound")
    void compoundTypesShouldBeCompound() {
      assertTrue(ComponentType.LIST.isCompound(), "LIST should be compound");
      assertTrue(ComponentType.RECORD.isCompound(), "RECORD should be compound");
      assertTrue(ComponentType.TUPLE.isCompound(), "TUPLE should be compound");
      assertTrue(ComponentType.VARIANT.isCompound(), "VARIANT should be compound");
      assertTrue(ComponentType.ENUM.isCompound(), "ENUM should be compound");
      assertTrue(ComponentType.OPTION.isCompound(), "OPTION should be compound");
      assertTrue(ComponentType.RESULT.isCompound(), "RESULT should be compound");
      assertTrue(ComponentType.FLAGS.isCompound(), "FLAGS should be compound");
    }

    @Test
    @DisplayName("primitive types should not be compound")
    void primitiveTypesShouldNotBeCompound() {
      assertFalse(ComponentType.BOOL.isCompound(), "BOOL should not be compound");
      assertFalse(ComponentType.S32.isCompound(), "S32 should not be compound");
      assertFalse(ComponentType.U64.isCompound(), "U64 should not be compound");
      assertFalse(ComponentType.F32.isCompound(), "F32 should not be compound");
      assertFalse(ComponentType.CHAR.isCompound(), "CHAR should not be compound");
      assertFalse(ComponentType.STRING.isCompound(), "STRING should not be compound");
    }

    @Test
    @DisplayName("resource types should not be compound")
    void resourceTypesShouldNotBeCompound() {
      assertFalse(ComponentType.OWN.isCompound(), "OWN should not be compound");
      assertFalse(ComponentType.BORROW.isCompound(), "BORROW should not be compound");
    }
  }

  @Nested
  @DisplayName("isResource Tests")
  class IsResourceTests {

    @Test
    @DisplayName("resource types should be resource")
    void resourceTypesShouldBeResource() {
      assertTrue(ComponentType.OWN.isResource(), "OWN should be resource");
      assertTrue(ComponentType.BORROW.isResource(), "BORROW should be resource");
    }

    @Test
    @DisplayName("non-resource types should not be resource")
    void nonResourceTypesShouldNotBeResource() {
      assertFalse(ComponentType.BOOL.isResource(), "BOOL should not be resource");
      assertFalse(ComponentType.S32.isResource(), "S32 should not be resource");
      assertFalse(ComponentType.F64.isResource(), "F64 should not be resource");
      assertFalse(ComponentType.STRING.isResource(), "STRING should not be resource");
      assertFalse(ComponentType.LIST.isResource(), "LIST should not be resource");
      assertFalse(ComponentType.RECORD.isResource(), "RECORD should not be resource");
      assertFalse(ComponentType.OPTION.isResource(), "OPTION should not be resource");
    }
  }

  @Nested
  @DisplayName("Type Classification Tests")
  class TypeClassificationTests {

    @Test
    @DisplayName("types should have mutually exclusive integer classification")
    void typesShouldHaveMutuallyExclusiveIntegerClassification() {
      for (final var type : ComponentType.values()) {
        if (type.isInteger()) {
          // Must be either signed or unsigned, but not both
          assertTrue(
              type.isSigned() ^ type.isUnsigned(),
              type + " should be either signed or unsigned, not both or neither");
        } else {
          // Non-integers should be neither signed nor unsigned
          assertFalse(type.isSigned(), type + " non-integer should not be signed");
          assertFalse(type.isUnsigned(), type + " non-integer should not be unsigned");
        }
      }
    }

    @Test
    @DisplayName("resource types should not overlap with other categories")
    void resourceTypesShouldNotOverlapWithOtherCategories() {
      for (final var type : ComponentType.values()) {
        if (type.isResource()) {
          assertFalse(type.isPrimitive(), type + " resource should not be primitive");
          assertFalse(type.isInteger(), type + " resource should not be integer");
          assertFalse(type.isFloat(), type + " resource should not be float");
          assertFalse(type.isCompound(), type + " resource should not be compound");
        }
      }
    }

    @Test
    @DisplayName("float types should not overlap with integer types")
    void floatTypesShouldNotOverlapWithIntegerTypes() {
      for (final var type : ComponentType.values()) {
        if (type.isFloat()) {
          assertFalse(type.isInteger(), type + " float should not be integer");
        }
      }
    }

    @Test
    @DisplayName("all types should be in at least one category")
    void allTypesShouldBeInAtLeastOneCategory() {
      for (final var type : ComponentType.values()) {
        final boolean inAnyCategory = type.isPrimitive() || type.isCompound() || type.isResource();
        assertTrue(inAnyCategory, type + " should be in at least one category");
      }
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle valueOf for all types")
    void shouldHandleValueOfForAllTypes() {
      for (final var type : ComponentType.values()) {
        assertEquals(type, ComponentType.valueOf(type.name()), "valueOf should return same type");
      }
    }

    @Test
    @DisplayName("should have correct ordinal values")
    void shouldHaveCorrectOrdinalValues() {
      assertEquals(0, ComponentType.BOOL.ordinal(), "BOOL should be first");
      // Resource types should be last
      assertEquals(
          ComponentType.values().length - 2,
          ComponentType.OWN.ordinal(),
          "OWN should be second to last");
      assertEquals(
          ComponentType.values().length - 1,
          ComponentType.BORROW.ordinal(),
          "BORROW should be last");
    }
  }
}
