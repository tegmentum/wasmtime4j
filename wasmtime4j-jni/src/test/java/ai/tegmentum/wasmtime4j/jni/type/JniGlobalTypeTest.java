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

package ai.tegmentum.wasmtime4j.jni.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.GlobalType;
import ai.tegmentum.wasmtime4j.WasmTypeKind;
import ai.tegmentum.wasmtime4j.WasmValueType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link JniGlobalType} class.
 *
 * <p>This test class verifies the JNI implementation of GlobalType interface
 * for WebAssembly global types.
 */
@DisplayName("JniGlobalType Tests")
class JniGlobalTypeTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("JniGlobalType should be final class")
    void shouldBeFinalClass() {
      assertTrue(java.lang.reflect.Modifier.isFinal(JniGlobalType.class.getModifiers()),
          "JniGlobalType should be final");
    }

    @Test
    @DisplayName("JniGlobalType should implement GlobalType")
    void shouldImplementGlobalType() {
      assertTrue(GlobalType.class.isAssignableFrom(JniGlobalType.class),
          "JniGlobalType should implement GlobalType");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should create immutable I32 global")
    void constructorShouldCreateImmutableI32Global() {
      final JniGlobalType globalType = new JniGlobalType(WasmValueType.I32, false);
      assertNotNull(globalType, "GlobalType should not be null");
      assertEquals(WasmValueType.I32, globalType.getValueType(), "Value type should be I32");
      assertFalse(globalType.isMutable(), "Global should be immutable");
    }

    @Test
    @DisplayName("Constructor should create mutable I32 global")
    void constructorShouldCreateMutableI32Global() {
      final JniGlobalType globalType = new JniGlobalType(WasmValueType.I32, true);
      assertEquals(WasmValueType.I32, globalType.getValueType(), "Value type should be I32");
      assertTrue(globalType.isMutable(), "Global should be mutable");
    }

    @Test
    @DisplayName("Constructor should create I64 global")
    void constructorShouldCreateI64Global() {
      final JniGlobalType globalType = new JniGlobalType(WasmValueType.I64, false);
      assertEquals(WasmValueType.I64, globalType.getValueType(), "Value type should be I64");
    }

    @Test
    @DisplayName("Constructor should create F32 global")
    void constructorShouldCreateF32Global() {
      final JniGlobalType globalType = new JniGlobalType(WasmValueType.F32, true);
      assertEquals(WasmValueType.F32, globalType.getValueType(), "Value type should be F32");
    }

    @Test
    @DisplayName("Constructor should create F64 global")
    void constructorShouldCreateF64Global() {
      final JniGlobalType globalType = new JniGlobalType(WasmValueType.F64, false);
      assertEquals(WasmValueType.F64, globalType.getValueType(), "Value type should be F64");
    }

    @Test
    @DisplayName("Constructor should create V128 global")
    void constructorShouldCreateV128Global() {
      final JniGlobalType globalType = new JniGlobalType(WasmValueType.V128, true);
      assertEquals(WasmValueType.V128, globalType.getValueType(), "Value type should be V128");
    }

    @Test
    @DisplayName("Constructor should create FUNCREF global")
    void constructorShouldCreateFuncrefGlobal() {
      final JniGlobalType globalType = new JniGlobalType(WasmValueType.FUNCREF, false);
      assertEquals(WasmValueType.FUNCREF, globalType.getValueType(), "Value type should be FUNCREF");
    }

    @Test
    @DisplayName("Constructor should create EXTERNREF global")
    void constructorShouldCreateExternrefGlobal() {
      final JniGlobalType globalType = new JniGlobalType(WasmValueType.EXTERNREF, true);
      assertEquals(WasmValueType.EXTERNREF, globalType.getValueType(), "Value type should be EXTERNREF");
    }

    @Test
    @DisplayName("Constructor should throw for null value type")
    void constructorShouldThrowForNullValueType() {
      assertThrows(IllegalArgumentException.class, () ->
              new JniGlobalType(null, false),
          "Should throw for null value type");
    }
  }

  @Nested
  @DisplayName("getKind Tests")
  class GetKindTests {

    @Test
    @DisplayName("getKind should return GLOBAL")
    void getKindShouldReturnGlobal() {
      final JniGlobalType globalType = new JniGlobalType(WasmValueType.I32, false);
      assertEquals(WasmTypeKind.GLOBAL, globalType.getKind(),
          "Kind should be GLOBAL");
    }
  }

  @Nested
  @DisplayName("Equals Tests")
  class EqualsTests {

    @Test
    @DisplayName("Same instance should be equal")
    void sameInstanceShouldBeEqual() {
      final JniGlobalType globalType = new JniGlobalType(WasmValueType.I32, true);
      assertEquals(globalType, globalType, "Same instance should be equal");
    }

    @Test
    @DisplayName("Equal values should be equal")
    void equalValuesShouldBeEqual() {
      final JniGlobalType globalType1 = new JniGlobalType(WasmValueType.I64, true);
      final JniGlobalType globalType2 = new JniGlobalType(WasmValueType.I64, true);
      assertEquals(globalType1, globalType2, "Equal values should be equal");
    }

    @Test
    @DisplayName("Different value type should not be equal")
    void differentValueTypeShouldNotBeEqual() {
      final JniGlobalType globalType1 = new JniGlobalType(WasmValueType.I32, false);
      final JniGlobalType globalType2 = new JniGlobalType(WasmValueType.I64, false);
      assertNotEquals(globalType1, globalType2, "Different value type should not be equal");
    }

    @Test
    @DisplayName("Different mutability should not be equal")
    void differentMutabilityShouldNotBeEqual() {
      final JniGlobalType globalType1 = new JniGlobalType(WasmValueType.I32, true);
      final JniGlobalType globalType2 = new JniGlobalType(WasmValueType.I32, false);
      assertNotEquals(globalType1, globalType2, "Different mutability should not be equal");
    }

    @Test
    @DisplayName("Should not be equal to null")
    void shouldNotBeEqualToNull() {
      final JniGlobalType globalType = new JniGlobalType(WasmValueType.I32, false);
      assertFalse(globalType.equals(null), "Should not be equal to null");
    }

    @Test
    @DisplayName("Should not be equal to different type")
    void shouldNotBeEqualToDifferentType() {
      final JniGlobalType globalType = new JniGlobalType(WasmValueType.I32, false);
      assertFalse(globalType.equals("string"), "Should not be equal to different type");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("Equal objects should have equal hashCodes")
    void equalObjectsShouldHaveEqualHashCodes() {
      final JniGlobalType globalType1 = new JniGlobalType(WasmValueType.F64, true);
      final JniGlobalType globalType2 = new JniGlobalType(WasmValueType.F64, true);
      assertEquals(globalType1.hashCode(), globalType2.hashCode(),
          "Equal objects should have equal hashCodes");
    }

    @Test
    @DisplayName("HashCode should be consistent")
    void hashCodeShouldBeConsistent() {
      final JniGlobalType globalType = new JniGlobalType(WasmValueType.V128, false);
      final int hash1 = globalType.hashCode();
      final int hash2 = globalType.hashCode();
      assertEquals(hash1, hash2, "HashCode should be consistent");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include value type")
    void toStringShouldIncludeValueType() {
      final JniGlobalType globalType = new JniGlobalType(WasmValueType.F32, false);
      final String str = globalType.toString();
      assertTrue(str.contains("F32") || str.contains("f32"),
          "toString should include value type");
    }

    @Test
    @DisplayName("toString should include mutability")
    void toStringShouldIncludeMutability() {
      final JniGlobalType globalType = new JniGlobalType(WasmValueType.I32, true);
      final String str = globalType.toString();
      assertTrue(str.contains("mutable") || str.contains("true"),
          "toString should include mutability");
    }

    @Test
    @DisplayName("toString should include GlobalType")
    void toStringShouldIncludeGlobalType() {
      final JniGlobalType globalType = new JniGlobalType(WasmValueType.I32, false);
      final String str = globalType.toString();
      assertTrue(str.contains("GlobalType"), "toString should include GlobalType");
    }
  }

  @Nested
  @DisplayName("fromNative Tests")
  class FromNativeTests {

    @Test
    @DisplayName("fromNative should throw for zero handle")
    void fromNativeShouldThrowForZeroHandle() {
      assertThrows(IllegalArgumentException.class, () ->
              JniGlobalType.fromNative(0),
          "Should throw for zero handle");
    }

    @Test
    @DisplayName("fromNative should throw for negative handle")
    void fromNativeShouldThrowForNegativeHandle() {
      assertThrows(IllegalArgumentException.class, () ->
              JniGlobalType.fromNative(-1),
          "Should throw for negative handle");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("All value types should be supported")
    void allValueTypesShouldBeSupported() {
      for (final WasmValueType valueType : WasmValueType.values()) {
        // Test both mutable and immutable
        final JniGlobalType immutable = new JniGlobalType(valueType, false);
        final JniGlobalType mutable = new JniGlobalType(valueType, true);

        assertEquals(valueType, immutable.getValueType(), "Immutable should have correct type");
        assertEquals(valueType, mutable.getValueType(), "Mutable should have correct type");
        assertFalse(immutable.isMutable(), "Should be immutable");
        assertTrue(mutable.isMutable(), "Should be mutable");
        assertEquals(WasmTypeKind.GLOBAL, immutable.getKind(), "Kind should be GLOBAL");
        assertEquals(WasmTypeKind.GLOBAL, mutable.getKind(), "Kind should be GLOBAL");
      }
    }

    @Test
    @DisplayName("Typical global type patterns should work")
    void typicalGlobalTypePatternshouldWork() {
      // Common pattern: immutable i32 global (constant)
      final JniGlobalType constant = new JniGlobalType(WasmValueType.I32, false);
      assertFalse(constant.isMutable(), "Constant should be immutable");

      // Common pattern: mutable i32 global (variable)
      final JniGlobalType variable = new JniGlobalType(WasmValueType.I32, true);
      assertTrue(variable.isMutable(), "Variable should be mutable");

      // SIMD global
      final JniGlobalType simdGlobal = new JniGlobalType(WasmValueType.V128, false);
      assertEquals(WasmValueType.V128, simdGlobal.getValueType(), "SIMD global should have V128 type");
    }
  }
}
