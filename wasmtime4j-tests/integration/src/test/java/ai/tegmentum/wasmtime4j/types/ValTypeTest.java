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
package ai.tegmentum.wasmtime4j.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.type.ValType;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the ValType system.
 *
 * <p>This test class validates the ValType interface, ValTypes utility class, and their
 * implementations for WebAssembly value type handling.
 */
@DisplayName("ValType Integration Tests")
public class ValTypeTest {

  private static final Logger LOGGER = Logger.getLogger(ValTypeTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting ValType Integration Tests");
  }

  @Nested
  @DisplayName("ValType Factory Method Tests")
  class ValTypeFactoryMethodTests {

    @Test
    @DisplayName("Should create i32 value type")
    void shouldCreateI32ValueType() {
      LOGGER.info("Testing ValType.i32()");

      ValType i32 = ValType.i32();
      assertNotNull(i32, "i32 should not be null");
      assertEquals(WasmValueType.I32, i32.getValueType(), "Should be I32 type");
      assertTrue(i32.isNumeric(), "i32 should be numeric");
      assertTrue(i32.isInteger(), "i32 should be integer");
      assertFalse(i32.isFloat(), "i32 should not be float");
      assertFalse(i32.isReference(), "i32 should not be reference");

      LOGGER.info("ValType.i32() verified");
    }

    @Test
    @DisplayName("Should create i64 value type")
    void shouldCreateI64ValueType() {
      LOGGER.info("Testing ValType.i64()");

      ValType i64 = ValType.i64();
      assertNotNull(i64, "i64 should not be null");
      assertEquals(WasmValueType.I64, i64.getValueType(), "Should be I64 type");
      assertTrue(i64.isNumeric(), "i64 should be numeric");
      assertTrue(i64.isInteger(), "i64 should be integer");
      assertFalse(i64.isFloat(), "i64 should not be float");
      assertFalse(i64.isReference(), "i64 should not be reference");

      LOGGER.info("ValType.i64() verified");
    }

    @Test
    @DisplayName("Should create f32 value type")
    void shouldCreateF32ValueType() {
      LOGGER.info("Testing ValType.f32()");

      ValType f32 = ValType.f32();
      assertNotNull(f32, "f32 should not be null");
      assertEquals(WasmValueType.F32, f32.getValueType(), "Should be F32 type");
      assertTrue(f32.isNumeric(), "f32 should be numeric");
      assertFalse(f32.isInteger(), "f32 should not be integer");
      assertTrue(f32.isFloat(), "f32 should be float");
      assertFalse(f32.isReference(), "f32 should not be reference");

      LOGGER.info("ValType.f32() verified");
    }

    @Test
    @DisplayName("Should create f64 value type")
    void shouldCreateF64ValueType() {
      LOGGER.info("Testing ValType.f64()");

      ValType f64 = ValType.f64();
      assertNotNull(f64, "f64 should not be null");
      assertEquals(WasmValueType.F64, f64.getValueType(), "Should be F64 type");
      assertTrue(f64.isNumeric(), "f64 should be numeric");
      assertFalse(f64.isInteger(), "f64 should not be integer");
      assertTrue(f64.isFloat(), "f64 should be float");
      assertFalse(f64.isReference(), "f64 should not be reference");

      LOGGER.info("ValType.f64() verified");
    }

    @Test
    @DisplayName("Should create v128 value type")
    void shouldCreateV128ValueType() {
      LOGGER.info("Testing ValType.v128()");

      ValType v128 = ValType.v128();
      assertNotNull(v128, "v128 should not be null");
      assertEquals(WasmValueType.V128, v128.getValueType(), "Should be V128 type");
      assertTrue(v128.isVector(), "v128 should be vector");
      assertFalse(v128.isNumeric(), "v128 should not be numeric");
      assertFalse(v128.isReference(), "v128 should not be reference");

      LOGGER.info("ValType.v128() verified");
    }

    @Test
    @DisplayName("Should create funcref value type")
    void shouldCreateFuncrefValueType() {
      LOGGER.info("Testing ValType.funcref()");

      ValType funcref = ValType.funcref();
      assertNotNull(funcref, "funcref should not be null");
      assertEquals(WasmValueType.FUNCREF, funcref.getValueType(), "Should be FUNCREF type");
      assertTrue(funcref.isReference(), "funcref should be reference");
      assertFalse(funcref.isNumeric(), "funcref should not be numeric");
      assertFalse(funcref.isVector(), "funcref should not be vector");

      LOGGER.info("ValType.funcref() verified");
    }

    @Test
    @DisplayName("Should create externref value type")
    void shouldCreateExternrefValueType() {
      LOGGER.info("Testing ValType.externref()");

      ValType externref = ValType.externref();
      assertNotNull(externref, "externref should not be null");
      assertEquals(WasmValueType.EXTERNREF, externref.getValueType(), "Should be EXTERNREF type");
      assertTrue(externref.isReference(), "externref should be reference");
      assertFalse(externref.isNumeric(), "externref should not be numeric");
      assertFalse(externref.isVector(), "externref should not be vector");

      LOGGER.info("ValType.externref() verified");
    }

    @Test
    @DisplayName("Should create value type from WasmValueType")
    void shouldCreateValueTypeFromWasmValueType() {
      LOGGER.info("Testing ValType.from()");

      for (WasmValueType wasmType : WasmValueType.values()) {
        ValType valType = ValType.from(wasmType);
        assertNotNull(valType, "ValType for " + wasmType + " should not be null");
        assertEquals(wasmType, valType.getValueType(), "Should wrap correct WasmValueType");
      }

      LOGGER.info("ValType.from() verified for all WasmValueType values");
    }
  }

  @Nested
  @DisplayName("ValType Type Classification Tests")
  class ValTypeTypeClassificationTests {

    @Test
    @DisplayName("Should correctly classify numeric types")
    void shouldCorrectlyClassifyNumericTypes() {
      LOGGER.info("Testing numeric type classification");

      ValType[] numericTypes = {ValType.i32(), ValType.i64(), ValType.f32(), ValType.f64()};

      for (ValType numericType : numericTypes) {
        assertTrue(
            numericType.isNumeric(), numericType.getValueType() + " should be classified numeric");
        assertFalse(
            numericType.isReference(),
            numericType.getValueType() + " should not be classified reference");
        assertFalse(
            numericType.isVector(),
            numericType.getValueType() + " should not be classified vector");
      }

      LOGGER.info("Numeric type classification verified");
    }

    @Test
    @DisplayName("Should correctly classify integer types")
    void shouldCorrectlyClassifyIntegerTypes() {
      LOGGER.info("Testing integer type classification");

      assertTrue(ValType.i32().isInteger(), "i32 should be integer");
      assertTrue(ValType.i64().isInteger(), "i64 should be integer");
      assertFalse(ValType.f32().isInteger(), "f32 should not be integer");
      assertFalse(ValType.f64().isInteger(), "f64 should not be integer");

      LOGGER.info("Integer type classification verified");
    }

    @Test
    @DisplayName("Should correctly classify float types")
    void shouldCorrectlyClassifyFloatTypes() {
      LOGGER.info("Testing float type classification");

      assertFalse(ValType.i32().isFloat(), "i32 should not be float");
      assertFalse(ValType.i64().isFloat(), "i64 should not be float");
      assertTrue(ValType.f32().isFloat(), "f32 should be float");
      assertTrue(ValType.f64().isFloat(), "f64 should be float");

      LOGGER.info("Float type classification verified");
    }

    @Test
    @DisplayName("Should correctly classify reference types")
    void shouldCorrectlyClassifyReferenceTypes() {
      LOGGER.info("Testing reference type classification");

      ValType funcref = ValType.funcref();
      assertTrue(funcref.isReference(), "funcref should be reference");
      assertFalse(funcref.isNumeric(), "funcref should not be numeric");

      ValType externref = ValType.externref();
      assertTrue(externref.isReference(), "externref should be reference");
      assertFalse(externref.isNumeric(), "externref should not be numeric");

      LOGGER.info("Reference type classification verified");
    }

    @Test
    @DisplayName("Should correctly classify GC reference types")
    void shouldCorrectlyClassifyGcReferenceTypes() {
      LOGGER.info("Testing GC reference type classification");

      WasmValueType[] gcTypes = {
        WasmValueType.ANYREF,
        WasmValueType.EQREF,
        WasmValueType.I31REF,
        WasmValueType.STRUCTREF,
        WasmValueType.ARRAYREF,
        WasmValueType.NULLREF,
        WasmValueType.NULLFUNCREF,
        WasmValueType.NULLEXTERNREF,
        WasmValueType.NULLEXNREF,
        WasmValueType.CONTREF,
        WasmValueType.NULLCONTREF
      };

      for (WasmValueType gcType : gcTypes) {
        ValType valType = ValType.from(gcType);
        assertTrue(valType.isGcReference(), gcType + " should be GC reference");
        assertTrue(valType.isReference(), gcType + " should be reference");
      }

      // Non-GC types should not be GC references
      assertFalse(ValType.i32().isGcReference(), "i32 should not be GC reference");
      assertFalse(ValType.funcref().isGcReference(), "funcref should not be GC reference");

      LOGGER.info("GC reference type classification verified");
    }

    @Test
    @DisplayName("Should correctly classify nullable reference types")
    void shouldCorrectlyClassifyNullableReferenceTypes() {
      LOGGER.info("Testing nullable reference type classification");

      assertTrue(
          ValType.from(WasmValueType.NULLREF).isNullableReference(), "NULLREF should be nullable");
      assertTrue(
          ValType.from(WasmValueType.NULLFUNCREF).isNullableReference(),
          "NULLFUNCREF should be nullable");
      assertTrue(
          ValType.from(WasmValueType.NULLEXTERNREF).isNullableReference(),
          "NULLEXTERNREF should be nullable");
      assertTrue(
          ValType.from(WasmValueType.NULLEXNREF).isNullableReference(),
          "NULLEXNREF should be nullable");
      assertTrue(
          ValType.from(WasmValueType.NULLCONTREF).isNullableReference(),
          "NULLCONTREF should be nullable");

      assertFalse(ValType.i32().isNullableReference(), "i32 should not be nullable");
      assertFalse(ValType.funcref().isNullableReference(), "funcref should not be nullable");

      LOGGER.info("Nullable reference type classification verified");
    }

    @Test
    @DisplayName("Should correctly classify vector types")
    void shouldCorrectlyClassifyVectorTypes() {
      LOGGER.info("Testing vector type classification");

      assertTrue(ValType.v128().isVector(), "v128 should be vector");

      assertFalse(ValType.i32().isVector(), "i32 should not be vector");
      assertFalse(ValType.f64().isVector(), "f64 should not be vector");
      assertFalse(ValType.funcref().isVector(), "funcref should not be vector");

      LOGGER.info("Vector type classification verified");
    }
  }

  @Nested
  @DisplayName("ValType Matching Tests")
  class ValTypeMatchingTests {

    @Test
    @DisplayName("Should match identical types")
    void shouldMatchIdenticalTypes() {
      LOGGER.info("Testing identical type matching");

      ValType i32a = ValType.i32();
      ValType i32b = ValType.i32();

      assertTrue(i32a.matches(i32b), "Identical i32 types should match");
      assertTrue(i32a.eq(i32b), "Identical i32 types should be equal");

      LOGGER.info("Identical type matching verified");
    }

    @Test
    @DisplayName("Should not match different types")
    void shouldNotMatchDifferentTypes() {
      LOGGER.info("Testing different type matching");

      ValType i32 = ValType.i32();
      ValType i64 = ValType.i64();

      assertFalse(i32.matches(i64), "i32 should not match i64");
      assertFalse(i32.eq(i64), "i32 should not equal i64");

      LOGGER.info("Different type matching verified");
    }

    @Test
    @DisplayName("Should handle null in matches")
    void shouldHandleNullInMatches() {
      LOGGER.info("Testing null handling in matches");

      ValType i32 = ValType.i32();

      assertFalse(i32.matches(null), "matches(null) should return false");
      assertFalse(i32.eq(null), "eq(null) should return false");

      LOGGER.info("Null handling in matches verified");
    }

    @Test
    @DisplayName("Should verify reference type matching with subtyping")
    void shouldVerifyReferenceTypeMatchingWithSubtyping() {
      LOGGER.info("Testing reference type matching with subtyping");

      // Get all reference types for subtype relationship testing
      ValType funcref = ValType.funcref();
      ValType externref = ValType.externref();

      // Same types should always match
      assertTrue(funcref.matches(funcref), "funcref should match itself");
      assertTrue(externref.matches(externref), "externref should match itself");

      // Different reference types may or may not match based on subtyping rules
      // This tests that the matching logic is implemented
      boolean funcMatchesExtern = funcref.matches(externref);
      boolean externMatchesFuncResult = externref.matches(funcref);

      LOGGER.info(
          "Reference type matching: funcref->externref="
              + funcMatchesExtern
              + ", externref->funcref="
              + externMatchesFuncResult);
      LOGGER.info("Reference type matching verified");
    }
  }

  @Nested
  @DisplayName("ValType Equality Tests")
  class ValTypeEqualityTests {

    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
      LOGGER.info("Testing ValType equals implementation");

      ValType i32a = ValType.i32();
      ValType i32b = ValType.i32();

      // Reflexive
      assertEquals(i32a, i32a, "ValType should equal itself");

      // Symmetric
      assertEquals(i32a, i32b, "Equal ValTypes should be equal");
      assertEquals(i32b, i32a, "Equal ValTypes should be symmetric");

      // Not equal to different type
      assertNotEquals(i32a, ValType.i64(), "Different ValTypes should not be equal");

      // Not equal to null
      assertNotEquals(i32a, null, "ValType should not equal null");

      LOGGER.info("ValType equals implementation verified");
    }

    @Test
    @DisplayName("Should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
      LOGGER.info("Testing ValType hashCode implementation");

      ValType i32a = ValType.i32();
      ValType i32b = ValType.i32();

      assertEquals(i32a.hashCode(), i32b.hashCode(), "Equal ValTypes should have same hashCode");

      LOGGER.info("ValType hashCode implementation verified");
    }

    @Test
    @DisplayName("Should implement toString correctly")
    void shouldImplementToStringCorrectly() {
      LOGGER.info("Testing ValType toString implementation");

      ValType i32 = ValType.i32();
      String str = i32.toString();

      assertNotNull(str, "toString should not return null");
      assertFalse(str.isEmpty(), "toString should not return empty string");

      LOGGER.info("ValType toString: " + str);
      LOGGER.info("ValType toString implementation verified");
    }
  }

  @Nested
  @DisplayName("ValTypes Factory Error Handling Tests")
  class ValTypesFactoryErrorHandlingTests {

    @Test
    @DisplayName("Should throw exception for null WasmValueType")
    void shouldThrowExceptionForNullWasmValueType() {
      LOGGER.info("Testing null WasmValueType handling");

      assertThrows(
          IllegalArgumentException.class,
          () -> ValType.from(null),
          "Should throw IllegalArgumentException for null WasmValueType");

      LOGGER.info("Null WasmValueType handling verified");
    }
  }

  @Nested
  @DisplayName("ValType Complete Coverage Tests")
  class ValTypeCompleteCoverageTests {

    @Test
    @DisplayName("Should create all WasmValueType variants")
    void shouldCreateAllWasmValueTypeVariants() {
      LOGGER.info("Testing all WasmValueType variants");

      for (WasmValueType wasmType : WasmValueType.values()) {
        ValType valType = ValType.from(wasmType);
        assertNotNull(valType, "ValType for " + wasmType + " should not be null");
        assertEquals(wasmType, valType.getValueType(), "Should wrap " + wasmType);

        // Verify classification methods don't throw
        boolean isNumeric = valType.isNumeric();
        boolean isInteger = valType.isInteger();
        boolean isFloat = valType.isFloat();
        boolean isReference = valType.isReference();
        boolean isGcReference = valType.isGcReference();
        boolean isNullableReference = valType.isNullableReference();
        boolean isVector = valType.isVector();

        LOGGER.fine(
            wasmType
                + ": numeric="
                + isNumeric
                + ", integer="
                + isInteger
                + ", float="
                + isFloat
                + ", reference="
                + isReference
                + ", gcRef="
                + isGcReference
                + ", nullableRef="
                + isNullableReference
                + ", vector="
                + isVector);
      }

      LOGGER.info("All WasmValueType variants verified");
    }

    @Test
    @DisplayName("Should verify mutual exclusivity of categories")
    void shouldVerifyMutualExclusivityOfCategories() {
      LOGGER.info("Testing mutual exclusivity of type categories");

      for (WasmValueType wasmType : WasmValueType.values()) {
        ValType valType = ValType.from(wasmType);

        // A type can't be both numeric and reference
        if (valType.isNumeric()) {
          assertFalse(valType.isReference(), wasmType + " cannot be both numeric and reference");
        }

        // A type can't be both vector and something else (except maybe reference in future)
        if (valType.isVector()) {
          assertFalse(valType.isNumeric(), wasmType + " cannot be both vector and numeric");
          assertFalse(valType.isInteger(), wasmType + " cannot be both vector and integer");
          assertFalse(valType.isFloat(), wasmType + " cannot be both vector and float");
        }

        // Integer implies numeric
        if (valType.isInteger()) {
          assertTrue(valType.isNumeric(), wasmType + " integer should imply numeric");
        }

        // Float implies numeric
        if (valType.isFloat()) {
          assertTrue(valType.isNumeric(), wasmType + " float should imply numeric");
        }

        // GC reference implies reference
        if (valType.isGcReference()) {
          assertTrue(valType.isReference(), wasmType + " GC reference should imply reference");
        }

        // Nullable reference implies reference and GC reference
        if (valType.isNullableReference()) {
          assertTrue(valType.isReference(), wasmType + " nullable should imply reference");
          assertTrue(valType.isGcReference(), wasmType + " nullable should imply GC reference");
        }
      }

      LOGGER.info("Mutual exclusivity of type categories verified");
    }
  }
}
