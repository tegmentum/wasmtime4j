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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentType}.
 *
 * <p>Verifies enum structure, constants, boolean classification methods (isPrimitive,
 * isInteger, isSigned, isUnsigned, isFloat, isCompound, isResource), and the
 * mutual exclusivity of type categories.
 */
@DisplayName("ComponentType Tests")
class ComponentTypeTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(ComponentType.class.isEnum(), "ComponentType should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 23 values")
    void shouldHaveExactValueCount() {
      assertEquals(23, ComponentType.values().length,
          "ComponentType should have exactly 23 values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should contain all expected constants")
    void shouldContainAllExpectedConstants() {
      assertNotNull(ComponentType.BOOL, "BOOL constant should exist");
      assertNotNull(ComponentType.S8, "S8 constant should exist");
      assertNotNull(ComponentType.S16, "S16 constant should exist");
      assertNotNull(ComponentType.S32, "S32 constant should exist");
      assertNotNull(ComponentType.S64, "S64 constant should exist");
      assertNotNull(ComponentType.U8, "U8 constant should exist");
      assertNotNull(ComponentType.U16, "U16 constant should exist");
      assertNotNull(ComponentType.U32, "U32 constant should exist");
      assertNotNull(ComponentType.U64, "U64 constant should exist");
      assertNotNull(ComponentType.F32, "F32 constant should exist");
      assertNotNull(ComponentType.F64, "F64 constant should exist");
      assertNotNull(ComponentType.CHAR, "CHAR constant should exist");
      assertNotNull(ComponentType.STRING, "STRING constant should exist");
      assertNotNull(ComponentType.LIST, "LIST constant should exist");
      assertNotNull(ComponentType.RECORD, "RECORD constant should exist");
      assertNotNull(ComponentType.TUPLE, "TUPLE constant should exist");
      assertNotNull(ComponentType.VARIANT, "VARIANT constant should exist");
      assertNotNull(ComponentType.ENUM, "ENUM constant should exist");
      assertNotNull(ComponentType.OPTION, "OPTION constant should exist");
      assertNotNull(ComponentType.RESULT, "RESULT constant should exist");
      assertNotNull(ComponentType.FLAGS, "FLAGS constant should exist");
      assertNotNull(ComponentType.OWN, "OWN constant should exist");
      assertNotNull(ComponentType.BORROW, "BORROW constant should exist");
    }
  }

  @Nested
  @DisplayName("IsPrimitive Tests")
  class IsPrimitiveTests {

    @Test
    @DisplayName("BOOL should be primitive")
    void boolShouldBePrimitive() {
      assertTrue(ComponentType.BOOL.isPrimitive(), "BOOL should be primitive");
    }

    @Test
    @DisplayName("all signed integers should be primitive")
    void signedIntegersShouldBePrimitive() {
      assertTrue(ComponentType.S8.isPrimitive(), "S8 should be primitive");
      assertTrue(ComponentType.S16.isPrimitive(), "S16 should be primitive");
      assertTrue(ComponentType.S32.isPrimitive(), "S32 should be primitive");
      assertTrue(ComponentType.S64.isPrimitive(), "S64 should be primitive");
    }

    @Test
    @DisplayName("all unsigned integers should be primitive")
    void unsignedIntegersShouldBePrimitive() {
      assertTrue(ComponentType.U8.isPrimitive(), "U8 should be primitive");
      assertTrue(ComponentType.U16.isPrimitive(), "U16 should be primitive");
      assertTrue(ComponentType.U32.isPrimitive(), "U32 should be primitive");
      assertTrue(ComponentType.U64.isPrimitive(), "U64 should be primitive");
    }

    @Test
    @DisplayName("floats should be primitive")
    void floatsShouldBePrimitive() {
      assertTrue(ComponentType.F32.isPrimitive(), "F32 should be primitive");
      assertTrue(ComponentType.F64.isPrimitive(), "F64 should be primitive");
    }

    @Test
    @DisplayName("CHAR and STRING should be primitive")
    void charAndStringShouldBePrimitive() {
      assertTrue(ComponentType.CHAR.isPrimitive(), "CHAR should be primitive");
      assertTrue(ComponentType.STRING.isPrimitive(), "STRING should be primitive");
    }

    @Test
    @DisplayName("compound types should not be primitive")
    void compoundTypesShouldNotBePrimitive() {
      assertFalse(ComponentType.LIST.isPrimitive(), "LIST should not be primitive");
      assertFalse(ComponentType.RECORD.isPrimitive(), "RECORD should not be primitive");
      assertFalse(ComponentType.TUPLE.isPrimitive(), "TUPLE should not be primitive");
      assertFalse(ComponentType.VARIANT.isPrimitive(),
          "VARIANT should not be primitive");
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
  @DisplayName("IsInteger Tests")
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
  @DisplayName("IsSigned Tests")
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
  }

  @Nested
  @DisplayName("IsUnsigned Tests")
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
  }

  @Nested
  @DisplayName("IsFloat Tests")
  class IsFloatTests {

    @Test
    @DisplayName("F32 should be float")
    void f32ShouldBeFloat() {
      assertTrue(ComponentType.F32.isFloat(), "F32 should be float");
    }

    @Test
    @DisplayName("F64 should be float")
    void f64ShouldBeFloat() {
      assertTrue(ComponentType.F64.isFloat(), "F64 should be float");
    }

    @Test
    @DisplayName("non-float types should not be float")
    void nonFloatTypesShouldNotBeFloat() {
      assertFalse(ComponentType.S32.isFloat(), "S32 should not be float");
      assertFalse(ComponentType.U32.isFloat(), "U32 should not be float");
      assertFalse(ComponentType.BOOL.isFloat(), "BOOL should not be float");
      assertFalse(ComponentType.STRING.isFloat(), "STRING should not be float");
    }
  }

  @Nested
  @DisplayName("IsCompound Tests")
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
      assertFalse(ComponentType.F64.isCompound(), "F64 should not be compound");
      assertFalse(ComponentType.STRING.isCompound(), "STRING should not be compound");
    }
  }

  @Nested
  @DisplayName("IsResource Tests")
  class IsResourceTests {

    @Test
    @DisplayName("OWN should be resource")
    void ownShouldBeResource() {
      assertTrue(ComponentType.OWN.isResource(), "OWN should be resource");
    }

    @Test
    @DisplayName("BORROW should be resource")
    void borrowShouldBeResource() {
      assertTrue(ComponentType.BORROW.isResource(), "BORROW should be resource");
    }

    @Test
    @DisplayName("non-resource types should not be resource")
    void nonResourceTypesShouldNotBeResource() {
      assertFalse(ComponentType.BOOL.isResource(), "BOOL should not be resource");
      assertFalse(ComponentType.LIST.isResource(), "LIST should not be resource");
      assertFalse(ComponentType.STRING.isResource(), "STRING should not be resource");
    }
  }

  @Nested
  @DisplayName("Category Exclusivity Tests")
  class CategoryExclusivityTests {

    @Test
    @DisplayName("every type should belong to exactly one top-level category")
    void everyTypeShouldBelongToExactlyOneCategory() {
      for (final ComponentType type : ComponentType.values()) {
        int categoryCount = 0;
        if (type.isPrimitive()) {
          categoryCount++;
        }
        if (type.isCompound()) {
          categoryCount++;
        }
        if (type.isResource()) {
          categoryCount++;
        }
        assertEquals(1, categoryCount,
            type.name() + " should belong to exactly one top-level category "
                + "(primitive/compound/resource)");
      }
    }

    @Test
    @DisplayName("signed and unsigned should be mutually exclusive")
    void signedAndUnsignedShouldBeMutuallyExclusive() {
      for (final ComponentType type : ComponentType.values()) {
        assertFalse(type.isSigned() && type.isUnsigned(),
            type.name() + " should not be both signed and unsigned");
      }
    }

    @Test
    @DisplayName("all integers should be either signed or unsigned")
    void allIntegersShouldBeSignedOrUnsigned() {
      for (final ComponentType type : ComponentType.values()) {
        if (type.isInteger()) {
          assertTrue(type.isSigned() || type.isUnsigned(),
              type.name() + " integer should be either signed or unsigned");
        }
      }
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("should resolve all constants via valueOf")
    void shouldResolveAllConstantsViaValueOf() {
      for (final ComponentType value : ComponentType.values()) {
        assertEquals(value, ComponentType.valueOf(value.name()),
            "valueOf should return " + value.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowForInvalidName() {
      assertThrows(IllegalArgumentException.class,
          () -> ComponentType.valueOf("INVALID_CONSTANT"),
          "valueOf with invalid name should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("should have unique ordinals")
    void shouldHaveUniqueOrdinals() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final ComponentType value : ComponentType.values()) {
        ordinals.add(value.ordinal());
      }
      assertEquals(ComponentType.values().length, ordinals.size(),
          "All ordinals should be unique");
    }

    @Test
    @DisplayName("should have sequential ordinals starting from 0")
    void shouldHaveSequentialOrdinals() {
      final ComponentType[] values = ComponentType.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal(),
            "Ordinal of " + values[i].name() + " should be " + i);
      }
    }
  }
}
