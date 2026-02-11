package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.func.Function;

import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;


import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the FunctionType class.
 *
 * <p>Tests verify function signature construction, parameter and return type handling, validation,
 * multi-value support, and compatibility checking.
 */
@DisplayName("FunctionType Tests")
class FunctionTypeTest {

  @Nested
  @DisplayName("Construction Tests")
  class ConstructionTests {

    @Test
    @DisplayName("should create function type with no params and no returns")
    void shouldCreateEmptyFunctionType() {
      final FunctionType funcType = new FunctionType(new WasmValueType[0], new WasmValueType[0]);
      assertNotNull(funcType, "Function type should not be null");
      assertEquals(0, funcType.getParamCount(), "Should have 0 parameters");
      assertEquals(0, funcType.getReturnCount(), "Should have 0 return values");
    }

    @Test
    @DisplayName("should create function type with params only")
    void shouldCreateFunctionTypeWithParamsOnly() {
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I64}, new WasmValueType[0]);
      assertEquals(2, funcType.getParamCount(), "Should have 2 parameters");
      assertEquals(0, funcType.getReturnCount(), "Should have 0 return values");
    }

    @Test
    @DisplayName("should create function type with returns only")
    void shouldCreateFunctionTypeWithReturnsOnly() {
      final FunctionType funcType =
          new FunctionType(new WasmValueType[0], new WasmValueType[] {WasmValueType.F32});
      assertEquals(0, funcType.getParamCount(), "Should have 0 parameters");
      assertEquals(1, funcType.getReturnCount(), "Should have 1 return value");
    }

    @Test
    @DisplayName("should create function type with params and returns")
    void shouldCreateFunctionTypeWithParamsAndReturns() {
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32});
      assertEquals(2, funcType.getParamCount(), "Should have 2 parameters");
      assertEquals(1, funcType.getReturnCount(), "Should have 1 return value");
    }

    @Test
    @DisplayName("should throw for null param types array")
    void shouldThrowForNullParamTypes() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new FunctionType(null, new WasmValueType[0]),
          "Should throw for null param types");
    }

    @Test
    @DisplayName("should throw for null return types array")
    void shouldThrowForNullReturnTypes() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new FunctionType(new WasmValueType[0], null),
          "Should throw for null return types");
    }

    @Test
    @DisplayName("should throw for null element in param types")
    void shouldThrowForNullParamTypeElement() {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new FunctionType(new WasmValueType[] {WasmValueType.I32, null}, new WasmValueType[0]),
          "Should throw for null element in param types");
    }

    @Test
    @DisplayName("should throw for null element in return types")
    void shouldThrowForNullReturnTypeElement() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new FunctionType(new WasmValueType[0], new WasmValueType[] {null}),
          "Should throw for null element in return types");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("should return correct param types")
    void shouldReturnCorrectParamTypes() {
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.F64}, new WasmValueType[0]);
      final WasmValueType[] params = funcType.getParamTypes();
      assertEquals(2, params.length, "Should have 2 param types");
      assertEquals(WasmValueType.I32, params[0], "First param should be I32");
      assertEquals(WasmValueType.F64, params[1], "Second param should be F64");
    }

    @Test
    @DisplayName("should return correct return types")
    void shouldReturnCorrectReturnTypes() {
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[0], new WasmValueType[] {WasmValueType.I64, WasmValueType.F32});
      final WasmValueType[] returns = funcType.getReturnTypes();
      assertEquals(2, returns.length, "Should have 2 return types");
      assertEquals(WasmValueType.I64, returns[0], "First return should be I64");
      assertEquals(WasmValueType.F32, returns[1], "Second return should be F32");
    }

    @Test
    @DisplayName("should return defensive copies of param types")
    void shouldReturnDefensiveCopiesOfParamTypes() {
      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[0]);
      final WasmValueType[] params1 = funcType.getParamTypes();
      final WasmValueType[] params2 = funcType.getParamTypes();
      assertNotSame(params1, params2, "Should return different array instances");
      params1[0] = WasmValueType.F64;
      assertEquals(WasmValueType.I32, funcType.getParamTypes()[0], "Original should be unchanged");
    }

    @Test
    @DisplayName("should return defensive copies of return types")
    void shouldReturnDefensiveCopiesOfReturnTypes() {
      final FunctionType funcType =
          new FunctionType(new WasmValueType[0], new WasmValueType[] {WasmValueType.I32});
      final WasmValueType[] returns1 = funcType.getReturnTypes();
      final WasmValueType[] returns2 = funcType.getReturnTypes();
      assertNotSame(returns1, returns2, "Should return different array instances");
      returns1[0] = WasmValueType.F64;
      assertEquals(WasmValueType.I32, funcType.getReturnTypes()[0], "Original should be unchanged");
    }
  }

  @Nested
  @DisplayName("Multi-Value Support Tests")
  class MultiValueSupportTests {

    @Test
    @DisplayName("should detect multiple return values")
    void shouldDetectMultipleReturnValues() {
      final FunctionType singleReturn =
          new FunctionType(new WasmValueType[0], new WasmValueType[] {WasmValueType.I32});
      assertFalse(singleReturn.hasMultipleReturns(), "Single return should not be multi-value");

      final FunctionType multiReturn =
          new FunctionType(
              new WasmValueType[0], new WasmValueType[] {WasmValueType.I32, WasmValueType.I64});
      assertTrue(multiReturn.hasMultipleReturns(), "Multiple returns should be multi-value");
    }

    @Test
    @DisplayName("should create multi-value function type")
    void shouldCreateMultiValueFunctionType() {
      final FunctionType funcType =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32, WasmValueType.F64});
      assertEquals(1, funcType.getParamCount(), "Should have 1 parameter");
      assertEquals(2, funcType.getReturnCount(), "Should have 2 return values");
      assertTrue(funcType.hasMultipleReturns(), "Should be multi-value");
    }

    @Test
    @DisplayName("should create multi-value no params function type")
    void shouldCreateMultiValueNoParamsFunctionType() {
      final FunctionType funcType =
          FunctionType.multiValueNoParams(WasmValueType.I32, WasmValueType.I64, WasmValueType.F32);
      assertEquals(0, funcType.getParamCount(), "Should have 0 parameters");
      assertEquals(3, funcType.getReturnCount(), "Should have 3 return values");
    }

    @Test
    @DisplayName("should create multi-value no returns function type")
    void shouldCreateMultiValueNoReturnsFunctionType() {
      final FunctionType funcType =
          FunctionType.multiValueNoReturns(WasmValueType.I32, WasmValueType.I64);
      assertEquals(2, funcType.getParamCount(), "Should have 2 parameters");
      assertEquals(0, funcType.getReturnCount(), "Should have 0 return values");
    }

    @Test
    @DisplayName("should validate multi-value limits")
    void shouldValidateMultiValueLimits() {
      final FunctionType validFunc =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});
      assertDoesNotThrow(
          () -> validFunc.validateMultiValueLimits(), "Valid function should not throw");
    }

    @Test
    @DisplayName("should get max value count")
    void shouldGetMaxValueCount() {
      assertEquals(16, FunctionType.getMaxValueCount(), "Max value count should be 16");
    }
  }

  @Nested
  @DisplayName("Compatibility Tests")
  class CompatibilityTests {

    @Test
    @DisplayName("should be compatible with identical function type")
    void shouldBeCompatibleWithIdentical() {
      final FunctionType funcType1 =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I64});
      final FunctionType funcType2 =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I64});
      assertTrue(
          funcType1.isCompatibleWith(funcType2), "Identical function types should be compatible");
    }

    @Test
    @DisplayName("should not be compatible with different param types")
    void shouldNotBeCompatibleWithDifferentParamTypes() {
      final FunctionType funcType1 =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});
      final FunctionType funcType2 =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I64}, new WasmValueType[] {WasmValueType.I32});
      assertFalse(
          funcType1.isCompatibleWith(funcType2), "Different param types should not be compatible");
    }

    @Test
    @DisplayName("should not be compatible with different return types")
    void shouldNotBeCompatibleWithDifferentReturnTypes() {
      final FunctionType funcType1 =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});
      final FunctionType funcType2 =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.F64});
      assertFalse(
          funcType1.isCompatibleWith(funcType2), "Different return types should not be compatible");
    }

    @Test
    @DisplayName("should not be compatible with different param count")
    void shouldNotBeCompatibleWithDifferentParamCount() {
      final FunctionType funcType1 =
          new FunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[0]);
      final FunctionType funcType2 =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32}, new WasmValueType[0]);
      assertFalse(
          funcType1.isCompatibleWith(funcType2), "Different param counts should not be compatible");
    }

    @Test
    @DisplayName("should not be compatible with null")
    void shouldNotBeCompatibleWithNull() {
      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[0]);
      assertFalse(funcType.isCompatibleWith(null), "Should not be compatible with null");
    }

    @Test
    @DisplayName("should match multi-value pattern")
    void shouldMatchMultiValuePattern() {
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I64},
              new WasmValueType[] {WasmValueType.F32, WasmValueType.F64});
      assertTrue(
          funcType.matchesMultiValuePattern(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I64},
              new WasmValueType[] {WasmValueType.F32, WasmValueType.F64}),
          "Should match identical pattern");
      assertFalse(
          funcType.matchesMultiValuePattern(
              new WasmValueType[] {WasmValueType.I64}, new WasmValueType[] {WasmValueType.F32}),
          "Should not match different pattern");
    }
  }

  @Nested
  @DisplayName("Type Kind Tests")
  class TypeKindTests {

    @Test
    @DisplayName("should return FUNCTION kind")
    void shouldReturnFunctionKind() {
      final FunctionType funcType = new FunctionType(new WasmValueType[0], new WasmValueType[0]);
      assertEquals(WasmTypeKind.FUNCTION, funcType.getKind(), "Should return FUNCTION kind");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should produce readable string representation")
    void shouldProduceReadableString() {
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I64},
              new WasmValueType[] {WasmValueType.F32});
      final String str = funcType.toString();
      assertNotNull(str, "String should not be null");
      assertTrue(str.contains("I32"), "Should contain I32");
      assertTrue(str.contains("I64"), "Should contain I64");
      assertTrue(str.contains("F32"), "Should contain F32");
    }

    @Test
    @DisplayName("should produce string for empty function type")
    void shouldProduceStringForEmptyFunctionType() {
      final FunctionType funcType = new FunctionType(new WasmValueType[0], new WasmValueType[0]);
      final String str = funcType.toString();
      assertNotNull(str, "String should not be null");
      assertTrue(str.contains("FunctionType"), "Should contain FunctionType");
    }
  }

  @Nested
  @DisplayName("Reference Type Support Tests")
  class ReferenceTypeSupportTests {

    @Test
    @DisplayName("should support FUNCREF parameter")
    void shouldSupportFuncrefParameter() {
      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {WasmValueType.FUNCREF}, new WasmValueType[0]);
      assertEquals(
          WasmValueType.FUNCREF, funcType.getParamTypes()[0], "Should support FUNCREF parameter");
    }

    @Test
    @DisplayName("should support EXTERNREF parameter")
    void shouldSupportExternrefParameter() {
      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {WasmValueType.EXTERNREF}, new WasmValueType[0]);
      assertEquals(
          WasmValueType.EXTERNREF,
          funcType.getParamTypes()[0],
          "Should support EXTERNREF parameter");
    }

    @Test
    @DisplayName("should support FUNCREF return")
    void shouldSupportFuncrefReturn() {
      final FunctionType funcType =
          new FunctionType(new WasmValueType[0], new WasmValueType[] {WasmValueType.FUNCREF});
      assertEquals(
          WasmValueType.FUNCREF, funcType.getReturnTypes()[0], "Should support FUNCREF return");
    }

    @Test
    @DisplayName("should support mixed numeric and reference types")
    void shouldSupportMixedTypes() {
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.EXTERNREF, WasmValueType.F64},
              new WasmValueType[] {WasmValueType.FUNCREF, WasmValueType.I64});
      assertEquals(3, funcType.getParamCount(), "Should have 3 params");
      assertEquals(2, funcType.getReturnCount(), "Should have 2 returns");
    }
  }
}
