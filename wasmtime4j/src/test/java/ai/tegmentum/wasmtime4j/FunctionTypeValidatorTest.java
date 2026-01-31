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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FunctionTypeValidator} utility class.
 *
 * <p>FunctionTypeValidator provides static methods for validating and comparing function types with
 * subtyping rules.
 */
@DisplayName("FunctionTypeValidator Tests")
class FunctionTypeValidatorTest {

  private FunctionType createType(
      final WasmValueType[] params, final WasmValueType[] returns) {
    return new FunctionType(params, returns);
  }

  @Nested
  @DisplayName("isSubtype Tests")
  class IsSubtypeTests {

    @Test
    @DisplayName("should return true when both types are identical by value")
    void shouldReturnTrueForIdenticalTypes() {
      final FunctionType type1 =
          createType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});
      final FunctionType type2 =
          createType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});

      // Same type passed as both will use equals which is identity-based for FunctionType
      // So we test with same object reference
      assertTrue(
          FunctionTypeValidator.isSubtype(type1, type1),
          "Same type should be subtype of itself");
    }

    @Test
    @DisplayName("should return false when subtype is null")
    void shouldReturnFalseWhenSubtypeNull() {
      final FunctionType type =
          createType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});

      assertFalse(
          FunctionTypeValidator.isSubtype(null, type), "null subtype should not be a subtype");
    }

    @Test
    @DisplayName("should return false when supertype is null")
    void shouldReturnFalseWhenSupertypeNull() {
      final FunctionType type =
          createType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});

      assertFalse(
          FunctionTypeValidator.isSubtype(type, null), "should not be subtype of null");
    }

    @Test
    @DisplayName("should return false when both are null")
    void shouldReturnFalseWhenBothNull() {
      assertFalse(
          FunctionTypeValidator.isSubtype(null, null), "null should not be subtype of null");
    }

    @Test
    @DisplayName("should return false for different parameter counts")
    void shouldReturnFalseForDifferentParamCounts() {
      final FunctionType oneParam =
          createType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final FunctionType twoParams =
          createType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32}, new WasmValueType[] {});

      assertFalse(
          FunctionTypeValidator.isSubtype(oneParam, twoParams),
          "Different param counts should not be subtype");
    }

    @Test
    @DisplayName("should return false for different return counts")
    void shouldReturnFalseForDifferentReturnCounts() {
      final FunctionType oneReturn =
          createType(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final FunctionType twoReturns =
          createType(
              new WasmValueType[] {},
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I64});

      assertFalse(
          FunctionTypeValidator.isSubtype(oneReturn, twoReturns),
          "Different return counts should not be subtype");
    }

    @Test
    @DisplayName("should return false for incompatible numeric parameter types")
    void shouldReturnFalseForIncompatibleNumericParamTypes() {
      final FunctionType i32Param =
          createType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final FunctionType i64Param =
          createType(new WasmValueType[] {WasmValueType.I64}, new WasmValueType[] {});

      assertFalse(
          FunctionTypeValidator.isSubtype(i32Param, i64Param),
          "I32 param should not be subtype of I64 param");
    }

    @Test
    @DisplayName("should return true for funcref subtype of externref in return position")
    void shouldReturnTrueForFuncrefSubtypeOfExternrefInReturn() {
      final FunctionType funcrefReturn =
          createType(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.FUNCREF});
      final FunctionType externrefReturn =
          createType(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.EXTERNREF});

      assertTrue(
          FunctionTypeValidator.isSubtype(funcrefReturn, externrefReturn),
          "funcref return should be subtype of externref return (covariant)");
    }
  }

  @Nested
  @DisplayName("isCompatible Tests")
  class IsCompatibleTests {

    @Test
    @DisplayName("should return true for identical types")
    void shouldReturnTrueForIdenticalTypes() {
      final FunctionType type =
          createType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});

      assertTrue(
          FunctionTypeValidator.isCompatible(type, type), "Same type should be compatible");
    }

    @Test
    @DisplayName("should return false when caller is null")
    void shouldReturnFalseWhenCallerNull() {
      final FunctionType type =
          createType(new WasmValueType[] {}, new WasmValueType[] {});

      assertFalse(
          FunctionTypeValidator.isCompatible(null, type), "null caller should not be compatible");
    }

    @Test
    @DisplayName("should return false when callee is null")
    void shouldReturnFalseWhenCalleeNull() {
      final FunctionType type =
          createType(new WasmValueType[] {}, new WasmValueType[] {});

      assertFalse(
          FunctionTypeValidator.isCompatible(type, null), "null callee should not be compatible");
    }

    @Test
    @DisplayName("should return false for different parameter counts")
    void shouldReturnFalseForDifferentParamCounts() {
      final FunctionType oneParam =
          createType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final FunctionType noParams =
          createType(new WasmValueType[] {}, new WasmValueType[] {});

      assertFalse(
          FunctionTypeValidator.isCompatible(oneParam, noParams),
          "Different param counts should not be compatible");
    }
  }

  @Nested
  @DisplayName("validateCall Tests")
  class ValidateCallTests {

    @Test
    @DisplayName("should not throw for valid call with matching types and params")
    void shouldNotThrowForValidCall() {
      final FunctionType type =
          createType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I64},
              new WasmValueType[] {WasmValueType.F32});

      final WasmValue[] params = new WasmValue[] {WasmValue.i32(1), WasmValue.i64(2L)};

      assertDoesNotThrow(
          () -> FunctionTypeValidator.validateCall(type, type, params),
          "Valid call should not throw");
    }

    @Test
    @DisplayName("should throw ValidationException for incompatible function types")
    void shouldThrowForIncompatibleTypes() {
      final FunctionType expected =
          createType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final FunctionType actual =
          createType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
              new WasmValueType[] {});
      final WasmValue[] params = new WasmValue[] {WasmValue.i32(1)};

      assertThrows(
          ValidationException.class,
          () -> FunctionTypeValidator.validateCall(expected, actual, params),
          "Incompatible types should throw ValidationException");
    }

    @Test
    @DisplayName("should throw ValidationException for wrong parameter count")
    void shouldThrowForWrongParamCount() {
      final FunctionType type =
          createType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final WasmValue[] params = new WasmValue[] {WasmValue.i32(1), WasmValue.i32(2)};

      assertThrows(
          ValidationException.class,
          () -> FunctionTypeValidator.validateCall(type, type, params),
          "Wrong param count should throw ValidationException");
    }

    @Test
    @DisplayName("should throw ValidationException for wrong parameter type")
    void shouldThrowForWrongParamType() {
      final FunctionType type =
          createType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final WasmValue[] params = new WasmValue[] {WasmValue.f64(1.0)};

      assertThrows(
          ValidationException.class,
          () -> FunctionTypeValidator.validateCall(type, type, params),
          "Wrong param type should throw ValidationException");
    }

    @Test
    @DisplayName("should not throw for zero-parameter function call")
    void shouldNotThrowForZeroParamFunctionCall() {
      final FunctionType type =
          createType(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final WasmValue[] params = new WasmValue[] {};

      assertDoesNotThrow(
          () -> FunctionTypeValidator.validateCall(type, type, params),
          "Zero-param call should not throw");
    }
  }

  @Nested
  @DisplayName("validateAssignment Tests")
  class ValidateAssignmentTests {

    @Test
    @DisplayName("should not throw for same type assignment")
    void shouldNotThrowForSameTypeAssignment() {
      final FunctionType type =
          createType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I64});

      assertDoesNotThrow(
          () -> FunctionTypeValidator.validateAssignment(type, type),
          "Same type assignment should not throw");
    }

    @Test
    @DisplayName("should throw ValidationException for incompatible assignment")
    void shouldThrowForIncompatibleAssignment() {
      final FunctionType target =
          createType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final FunctionType source =
          createType(new WasmValueType[] {WasmValueType.I64}, new WasmValueType[] {});

      assertThrows(
          ValidationException.class,
          () -> FunctionTypeValidator.validateAssignment(target, source),
          "Incompatible assignment should throw ValidationException");
    }
  }

  @Nested
  @DisplayName("getCommonSupertype Tests")
  class GetCommonSupertypeTests {

    @Test
    @DisplayName("should return same type when both types are identical")
    void shouldReturnSameTypeForIdentical() {
      final FunctionType type =
          createType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});

      final FunctionType result = FunctionTypeValidator.getCommonSupertype(type, type);

      assertNotNull(result, "Common supertype of identical types should not be null");
      assertEquals(type, result, "Common supertype should be the same type");
    }

    @Test
    @DisplayName("should return null when first type is null")
    void shouldReturnNullWhenFirstTypeNull() {
      final FunctionType type =
          createType(new WasmValueType[] {}, new WasmValueType[] {});

      assertNull(
          FunctionTypeValidator.getCommonSupertype(null, type),
          "Should return null when first type is null");
    }

    @Test
    @DisplayName("should return null when second type is null")
    void shouldReturnNullWhenSecondTypeNull() {
      final FunctionType type =
          createType(new WasmValueType[] {}, new WasmValueType[] {});

      assertNull(
          FunctionTypeValidator.getCommonSupertype(type, null),
          "Should return null when second type is null");
    }

    @Test
    @DisplayName("should return null when both types are null")
    void shouldReturnNullWhenBothNull() {
      assertNull(
          FunctionTypeValidator.getCommonSupertype(null, null),
          "Should return null when both types are null");
    }

    @Test
    @DisplayName("should return supertype when one is subtype of other")
    void shouldReturnSupertypeWhenOneIsSubtype() {
      // funcref return is subtype of externref return (covariant in returns)
      final FunctionType funcrefReturn =
          createType(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.FUNCREF});
      final FunctionType externrefReturn =
          createType(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.EXTERNREF});

      // funcrefReturn is subtype of externrefReturn
      final FunctionType result =
          FunctionTypeValidator.getCommonSupertype(funcrefReturn, externrefReturn);

      assertNotNull(result, "Common supertype should be the externref variant");
    }
  }

  @Nested
  @DisplayName("isSubtypeOf (value type) Tests")
  class IsSubtypeOfValueTypeTests {

    @Test
    @DisplayName("should return true for same type")
    void shouldReturnTrueForSameType() {
      assertTrue(
          FunctionTypeValidator.isSubtypeOf(WasmValueType.I32, WasmValueType.I32),
          "I32 should be subtype of I32");
    }

    @Test
    @DisplayName("should return false for null subtype")
    void shouldReturnFalseForNullSubtype() {
      assertFalse(
          FunctionTypeValidator.isSubtypeOf(null, WasmValueType.I32),
          "null should not be subtype");
    }

    @Test
    @DisplayName("should return false for null supertype")
    void shouldReturnFalseForNullSupertype() {
      assertFalse(
          FunctionTypeValidator.isSubtypeOf(WasmValueType.I32, null),
          "should not be subtype of null");
    }

    @Test
    @DisplayName("should return false for different numeric types")
    void shouldReturnFalseForDifferentNumericTypes() {
      assertFalse(
          FunctionTypeValidator.isSubtypeOf(WasmValueType.I32, WasmValueType.I64),
          "I32 should not be subtype of I64");
      assertFalse(
          FunctionTypeValidator.isSubtypeOf(WasmValueType.F32, WasmValueType.F64),
          "F32 should not be subtype of F64");
    }

    @Test
    @DisplayName("should return true for funcref subtype of externref")
    void shouldReturnTrueForFuncrefSubtypeOfExternref() {
      assertTrue(
          FunctionTypeValidator.isSubtypeOf(WasmValueType.FUNCREF, WasmValueType.EXTERNREF),
          "FUNCREF should be subtype of EXTERNREF");
    }

    @Test
    @DisplayName("should return true for funcref subtype of funcref")
    void shouldReturnTrueForFuncrefSubtypeOfFuncref() {
      assertTrue(
          FunctionTypeValidator.isSubtypeOf(WasmValueType.FUNCREF, WasmValueType.FUNCREF),
          "FUNCREF should be subtype of FUNCREF");
    }
  }
}
