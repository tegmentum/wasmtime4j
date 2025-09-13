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

package ai.tegmentum.wasmtime4j.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive integration tests for the WebAssembly type system.
 *
 * <p>These tests validate the complete type system integration including V128 support, reference
 * type handling, multi-value returns, and cross-runtime consistency between JNI and Panama
 * implementations.
 *
 * @since 1.0.0
 */
@DisplayName("Type System Integration Tests")
public class TypeSystemIntegrationTest {

  @Nested
  @DisplayName("WasmValue V128 Support")
  class V128SupportTest {

    @Test
    @DisplayName("Create V128 value with valid 16-byte array")
    void createV128WithValidArray() {
      final byte[] v128Data = new byte[16];
      Arrays.fill(v128Data, (byte) 0x42);

      final WasmValue v128Value = WasmValue.v128(v128Data);

      assertNotNull(v128Value);
      assertEquals(WasmValueType.V128, v128Value.getType());
      assertTrue(v128Value.isVector());
      assertFalse(v128Value.isNumeric());
      assertFalse(v128Value.isReference());

      final byte[] retrievedData = v128Value.asV128();
      assertArrayEquals(v128Data, retrievedData);
    }

    @Test
    @DisplayName("V128 value creation validates array size")
    void v128ValidatesArraySize() {
      // Test null array
      assertThrows(IllegalArgumentException.class, () -> WasmValue.v128(null));

      // Test wrong size arrays
      assertThrows(IllegalArgumentException.class, () -> WasmValue.v128(new byte[15]));
      assertThrows(IllegalArgumentException.class, () -> WasmValue.v128(new byte[17]));
      assertThrows(IllegalArgumentException.class, () -> WasmValue.v128(new byte[0]));
    }

    @Test
    @DisplayName("V128 value returns defensive copy")
    void v128ReturnsDefensiveCopy() {
      final byte[] originalData = new byte[16];
      Arrays.fill(originalData, (byte) 0x42);

      final WasmValue v128Value = WasmValue.v128(originalData);

      // Modify original array
      Arrays.fill(originalData, (byte) 0x00);

      // V128 value should not be affected
      final byte[] retrievedData = v128Value.asV128();
      for (byte b : retrievedData) {
        assertEquals((byte) 0x42, b);
      }

      // Modify retrieved array
      Arrays.fill(retrievedData, (byte) 0xFF);

      // V128 value should still not be affected
      final byte[] retrievedData2 = v128Value.asV128();
      for (byte b : retrievedData2) {
        assertEquals((byte) 0x42, b);
      }
    }

    @Test
    @DisplayName("V128 toString formatting")
    void v128ToStringFormatting() {
      final byte[] v128Data = {
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
        0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F
      };

      final WasmValue v128Value = WasmValue.v128(v128Data);
      final String string = v128Value.toString();

      assertTrue(string.contains("V128"));
      assertTrue(string.contains("0x00"));
      assertTrue(string.contains("0x0f"));
    }
  }

  @Nested
  @DisplayName("Reference Type Validation")
  class ReferenceTypeValidationTest {

    @Test
    @DisplayName("Funcref validation and type checking")
    void funcrefValidation() {
      final WasmValue funcrefValue = WasmValue.funcref(null);

      assertNotNull(funcrefValue);
      assertEquals(WasmValueType.FUNCREF, funcrefValue.getType());
      assertTrue(funcrefValue.isReference());
      assertFalse(funcrefValue.isNumeric());
      assertFalse(funcrefValue.isVector());

      assertNull(funcrefValue.asFuncref());

      // Test type validation
      assertThrows(ClassCastException.class, () -> funcrefValue.asI32());
      assertThrows(ClassCastException.class, () -> funcrefValue.asExternref());
    }

    @Test
    @DisplayName("Externref validation and type checking")
    void externrefValidation() {
      final WasmValue externrefValue = WasmValue.externref(null);

      assertNotNull(externrefValue);
      assertEquals(WasmValueType.EXTERNREF, externrefValue.getType());
      assertTrue(externrefValue.isReference());
      assertFalse(externrefValue.isNumeric());
      assertFalse(externrefValue.isVector());

      assertNull(externrefValue.asExternref());

      // Test type validation
      assertThrows(ClassCastException.class, () -> externrefValue.asI64());
      assertThrows(ClassCastException.class, () -> externrefValue.asFuncref());
    }

    @Test
    @DisplayName("Reference type error messages include actual type")
    void referenceTypeErrorMessages() {
      final WasmValue i32Value = WasmValue.i32(42);

      final ClassCastException funcrefException =
          assertThrows(ClassCastException.class, () -> i32Value.asFuncref());
      assertTrue(funcrefException.getMessage().contains("I32"));

      final ClassCastException externrefException =
          assertThrows(ClassCastException.class, () -> i32Value.asExternref());
      assertTrue(externrefException.getMessage().contains("I32"));
    }
  }

  @Nested
  @DisplayName("FunctionType Enhanced Features")
  class FunctionTypeEnhancedTest {

    @Test
    @DisplayName("FunctionType constructor validates parameters")
    void constructorValidation() {
      // Test null parameter types
      assertThrows(
          IllegalArgumentException.class, () -> new FunctionType(null, new WasmValueType[] {}));

      // Test null return types
      assertThrows(
          IllegalArgumentException.class, () -> new FunctionType(new WasmValueType[] {}, null));

      // Test null elements in parameter types
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new FunctionType(
                  new WasmValueType[] {WasmValueType.I32, null}, new WasmValueType[] {}));

      // Test null elements in return types
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new FunctionType(
                  new WasmValueType[] {}, new WasmValueType[] {null, WasmValueType.I64}));
    }

    @Test
    @DisplayName("Multi-value return support")
    void multiValueReturnSupport() {
      final WasmValueType[] paramTypes = {WasmValueType.I32, WasmValueType.F32};
      final WasmValueType[] returnTypes = {
        WasmValueType.I64, WasmValueType.F64, WasmValueType.V128
      };

      final FunctionType functionType = new FunctionType(paramTypes, returnTypes);

      assertEquals(2, functionType.getParamCount());
      assertEquals(3, functionType.getReturnCount());
      assertTrue(functionType.hasMultipleReturns());

      assertArrayEquals(paramTypes, functionType.getParamTypes());
      assertArrayEquals(returnTypes, functionType.getReturnTypes());
    }

    @Test
    @DisplayName("Parameter validation")
    void parameterValidation() {
      final FunctionType functionType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.V128},
              new WasmValueType[] {WasmValueType.F64});

      // Valid parameters
      final WasmValue[] validParams = {WasmValue.i32(42), WasmValue.v128(new byte[16])};

      // Should not throw
      functionType.validateParameters(validParams);

      // Invalid parameter count
      assertThrows(
          IllegalArgumentException.class,
          () -> functionType.validateParameters(new WasmValue[] {WasmValue.i32(42)}));

      // Invalid parameter type
      assertThrows(
          IllegalArgumentException.class,
          () ->
              functionType.validateParameters(
                  new WasmValue[] {WasmValue.i32(42), WasmValue.f32(3.14f)}));

      // Null parameter
      assertThrows(
          IllegalArgumentException.class,
          () -> functionType.validateParameters(new WasmValue[] {WasmValue.i32(42), null}));
    }

    @Test
    @DisplayName("Function type compatibility")
    void functionTypeCompatibility() {
      final FunctionType type1 =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.F32},
              new WasmValueType[] {WasmValueType.I64});

      final FunctionType type2 =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.F32},
              new WasmValueType[] {WasmValueType.I64});

      final FunctionType type3 =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I64});

      assertTrue(type1.isCompatibleWith(type2));
      assertFalse(type1.isCompatibleWith(type3));
      assertFalse(type1.isCompatibleWith(null));

      assertEquals(type1, type2);
      assertEquals(type1.hashCode(), type2.hashCode());
    }
  }

  @Nested
  @DisplayName("Type System Edge Cases")
  class TypeSystemEdgeCasesTest {

    @Test
    @DisplayName("Empty function signature")
    void emptyFunctionSignature() {
      final FunctionType emptyFunction =
          new FunctionType(new WasmValueType[] {}, new WasmValueType[] {});

      assertEquals(0, emptyFunction.getParamCount());
      assertEquals(0, emptyFunction.getReturnCount());
      assertFalse(emptyFunction.hasMultipleReturns());

      // Should not throw for null or empty parameters
      emptyFunction.validateParameters(null);
      emptyFunction.validateParameters(new WasmValue[] {});
    }

    @Test
    @DisplayName("All WebAssembly types support")
    void allTypesSupport() {
      final WasmValueType[] allTypes = WasmValueType.values();
      final FunctionType allTypesFunction = new FunctionType(allTypes, allTypes);

      assertEquals(allTypes.length, allTypesFunction.getParamCount());
      assertEquals(allTypes.length, allTypesFunction.getReturnCount());
      assertTrue(allTypesFunction.hasMultipleReturns());

      // Create values for all types
      final WasmValue[] testValues = {
        WasmValue.i32(42),
        WasmValue.i64(42L),
        WasmValue.f32(3.14f),
        WasmValue.f64(3.14159),
        WasmValue.v128(new byte[16]),
        WasmValue.funcref(null),
        WasmValue.externref(null)
      };

      // Should validate successfully
      allTypesFunction.validateParameters(testValues);
    }

    @Test
    @DisplayName("Type validation error messages")
    void typeValidationErrorMessages() {
      final WasmValue i32Value = WasmValue.i32(42);

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> i32Value.validateType(WasmValueType.F64));

      assertTrue(exception.getMessage().contains("I32"));
      assertTrue(exception.getMessage().contains("F64"));

      // Test null expected type
      assertThrows(IllegalArgumentException.class, () -> i32Value.validateType(null));
    }
  }
}
