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

import ai.tegmentum.wasmtime4j.FuncType;
import ai.tegmentum.wasmtime4j.WasmTypeKind;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link JniFuncType} class.
 *
 * <p>This test class verifies the JNI implementation of FuncType interface for WebAssembly function
 * types.
 */
@DisplayName("JniFuncType Tests")
class JniFuncTypeTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("JniFuncType should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(JniFuncType.class.getModifiers()),
          "JniFuncType should be final");
    }

    @Test
    @DisplayName("JniFuncType should implement FuncType")
    void shouldImplementFuncType() {
      assertTrue(
          FuncType.class.isAssignableFrom(JniFuncType.class),
          "JniFuncType should implement FuncType");
    }
  }

  @Nested
  @DisplayName("Constructor Tests - List Parameters")
  class ConstructorListTests {

    @Test
    @DisplayName("Constructor should accept empty params and results")
    void constructorShouldAcceptEmptyParamsAndResults() {
      final JniFuncType funcType =
          new JniFuncType(Collections.emptyList(), Collections.emptyList());
      assertNotNull(funcType, "FuncType should not be null");
      assertTrue(funcType.getParams().isEmpty(), "Params should be empty");
      assertTrue(funcType.getResults().isEmpty(), "Results should be empty");
    }

    @Test
    @DisplayName("Constructor should accept single param")
    void constructorShouldAcceptSingleParam() {
      final List<WasmValueType> params = Arrays.asList(WasmValueType.I32);
      final JniFuncType funcType = new JniFuncType(params, Collections.emptyList());
      assertEquals(1, funcType.getParams().size(), "Should have 1 param");
      assertEquals(WasmValueType.I32, funcType.getParams().get(0), "Param should be I32");
    }

    @Test
    @DisplayName("Constructor should accept single result")
    void constructorShouldAcceptSingleResult() {
      final List<WasmValueType> results = Arrays.asList(WasmValueType.I64);
      final JniFuncType funcType = new JniFuncType(Collections.emptyList(), results);
      assertEquals(1, funcType.getResults().size(), "Should have 1 result");
      assertEquals(WasmValueType.I64, funcType.getResults().get(0), "Result should be I64");
    }

    @Test
    @DisplayName("Constructor should accept multiple params and results")
    void constructorShouldAcceptMultipleParamsAndResults() {
      final List<WasmValueType> params = Arrays.asList(WasmValueType.I32, WasmValueType.I64);
      final List<WasmValueType> results = Arrays.asList(WasmValueType.F32, WasmValueType.F64);
      final JniFuncType funcType = new JniFuncType(params, results);
      assertEquals(2, funcType.getParams().size(), "Should have 2 params");
      assertEquals(2, funcType.getResults().size(), "Should have 2 results");
    }

    @Test
    @DisplayName("Constructor should throw for null params list")
    void constructorShouldThrowForNullParamsList() {
      assertThrows(
          JniException.class,
          () -> new JniFuncType(null, Collections.emptyList()),
          "Should throw for null params");
    }

    @Test
    @DisplayName("Constructor should throw for null results list")
    void constructorShouldThrowForNullResultsList() {
      assertThrows(
          JniException.class,
          () -> new JniFuncType(Collections.emptyList(), null),
          "Should throw for null results");
    }

    @Test
    @DisplayName("Constructor should throw for null element in params")
    void constructorShouldThrowForNullElementInParams() {
      final List<WasmValueType> params = new ArrayList<>();
      params.add(WasmValueType.I32);
      params.add(null);
      assertThrows(
          IllegalArgumentException.class,
          () -> new JniFuncType(params, Collections.emptyList()),
          "Should throw for null element in params");
    }

    @Test
    @DisplayName("Constructor should throw for null element in results")
    void constructorShouldThrowForNullElementInResults() {
      final List<WasmValueType> results = new ArrayList<>();
      results.add(null);
      assertThrows(
          IllegalArgumentException.class,
          () -> new JniFuncType(Collections.emptyList(), results),
          "Should throw for null element in results");
    }

    @Test
    @DisplayName("Params should be unmodifiable")
    void paramsShouldBeUnmodifiable() {
      final List<WasmValueType> params = Arrays.asList(WasmValueType.I32);
      final JniFuncType funcType = new JniFuncType(params, Collections.emptyList());
      assertThrows(
          UnsupportedOperationException.class,
          () -> funcType.getParams().add(WasmValueType.I64),
          "Params should be unmodifiable");
    }

    @Test
    @DisplayName("Results should be unmodifiable")
    void resultsShouldBeUnmodifiable() {
      final List<WasmValueType> results = Arrays.asList(WasmValueType.I32);
      final JniFuncType funcType = new JniFuncType(Collections.emptyList(), results);
      assertThrows(
          UnsupportedOperationException.class,
          () -> funcType.getResults().add(WasmValueType.I64),
          "Results should be unmodifiable");
    }
  }

  @Nested
  @DisplayName("Constructor Tests - Array Parameters")
  class ConstructorArrayTests {

    @Test
    @DisplayName("Array constructor should accept empty arrays")
    void arrayConstructorShouldAcceptEmptyArrays() {
      final JniFuncType funcType = new JniFuncType(new WasmValueType[0], new WasmValueType[0]);
      assertNotNull(funcType, "FuncType should not be null");
      assertTrue(funcType.getParams().isEmpty(), "Params should be empty");
      assertTrue(funcType.getResults().isEmpty(), "Results should be empty");
    }

    @Test
    @DisplayName("Array constructor should accept params and results")
    void arrayConstructorShouldAcceptParamsAndResults() {
      final WasmValueType[] params = {WasmValueType.I32, WasmValueType.I64};
      final WasmValueType[] results = {WasmValueType.F32};
      final JniFuncType funcType = new JniFuncType(params, results);
      assertEquals(2, funcType.getParams().size(), "Should have 2 params");
      assertEquals(1, funcType.getResults().size(), "Should have 1 result");
    }
  }

  @Nested
  @DisplayName("getKind Tests")
  class GetKindTests {

    @Test
    @DisplayName("getKind should return FUNCTION")
    void getKindShouldReturnFunction() {
      final JniFuncType funcType =
          new JniFuncType(Collections.emptyList(), Collections.emptyList());
      assertEquals(WasmTypeKind.FUNCTION, funcType.getKind(), "Kind should be FUNCTION");
    }
  }

  @Nested
  @DisplayName("Equals Tests")
  class EqualsTests {

    @Test
    @DisplayName("Same instance should be equal")
    void sameInstanceShouldBeEqual() {
      final JniFuncType funcType =
          new JniFuncType(Arrays.asList(WasmValueType.I32), Arrays.asList(WasmValueType.I64));
      assertEquals(funcType, funcType, "Same instance should be equal");
    }

    @Test
    @DisplayName("Equal params and results should be equal")
    void equalParamsAndResultsShouldBeEqual() {
      final JniFuncType funcType1 =
          new JniFuncType(Arrays.asList(WasmValueType.I32), Arrays.asList(WasmValueType.I64));
      final JniFuncType funcType2 =
          new JniFuncType(Arrays.asList(WasmValueType.I32), Arrays.asList(WasmValueType.I64));
      assertEquals(funcType1, funcType2, "Equal params and results should be equal");
    }

    @Test
    @DisplayName("Different params should not be equal")
    void differentParamsShouldNotBeEqual() {
      final JniFuncType funcType1 =
          new JniFuncType(Arrays.asList(WasmValueType.I32), Arrays.asList(WasmValueType.I64));
      final JniFuncType funcType2 =
          new JniFuncType(Arrays.asList(WasmValueType.F32), Arrays.asList(WasmValueType.I64));
      assertNotEquals(funcType1, funcType2, "Different params should not be equal");
    }

    @Test
    @DisplayName("Different results should not be equal")
    void differentResultsShouldNotBeEqual() {
      final JniFuncType funcType1 =
          new JniFuncType(Arrays.asList(WasmValueType.I32), Arrays.asList(WasmValueType.I64));
      final JniFuncType funcType2 =
          new JniFuncType(Arrays.asList(WasmValueType.I32), Arrays.asList(WasmValueType.F64));
      assertNotEquals(funcType1, funcType2, "Different results should not be equal");
    }

    @Test
    @DisplayName("Should not be equal to null")
    void shouldNotBeEqualToNull() {
      final JniFuncType funcType =
          new JniFuncType(Collections.emptyList(), Collections.emptyList());
      assertFalse(funcType.equals(null), "Should not be equal to null");
    }

    @Test
    @DisplayName("Should not be equal to different type")
    void shouldNotBeEqualToDifferentType() {
      final JniFuncType funcType =
          new JniFuncType(Collections.emptyList(), Collections.emptyList());
      assertFalse(funcType.equals("string"), "Should not be equal to different type");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("Equal objects should have equal hashCodes")
    void equalObjectsShouldHaveEqualHashCodes() {
      final JniFuncType funcType1 =
          new JniFuncType(Arrays.asList(WasmValueType.I32), Arrays.asList(WasmValueType.I64));
      final JniFuncType funcType2 =
          new JniFuncType(Arrays.asList(WasmValueType.I32), Arrays.asList(WasmValueType.I64));
      assertEquals(
          funcType1.hashCode(), funcType2.hashCode(), "Equal objects should have equal hashCodes");
    }

    @Test
    @DisplayName("HashCode should be consistent")
    void hashCodeShouldBeConsistent() {
      final JniFuncType funcType =
          new JniFuncType(Arrays.asList(WasmValueType.I32), Arrays.asList(WasmValueType.I64));
      final int hash1 = funcType.hashCode();
      final int hash2 = funcType.hashCode();
      assertEquals(hash1, hash2, "HashCode should be consistent");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include params")
    void toStringShouldIncludeParams() {
      final JniFuncType funcType =
          new JniFuncType(Arrays.asList(WasmValueType.I32), Collections.emptyList());
      final String str = funcType.toString();
      assertTrue(str.contains("I32"), "toString should include param type");
    }

    @Test
    @DisplayName("toString should include results")
    void toStringShouldIncludeResults() {
      final JniFuncType funcType =
          new JniFuncType(Collections.emptyList(), Arrays.asList(WasmValueType.F64));
      final String str = funcType.toString();
      assertTrue(str.contains("F64"), "toString should include result type");
    }

    @Test
    @DisplayName("toString should include FuncType")
    void toStringShouldIncludeFuncType() {
      final JniFuncType funcType =
          new JniFuncType(Collections.emptyList(), Collections.emptyList());
      final String str = funcType.toString();
      assertTrue(str.contains("FuncType"), "toString should include FuncType");
    }
  }

  @Nested
  @DisplayName("fromNative Tests")
  class FromNativeTests {

    @Test
    @DisplayName("fromNative should throw for zero handle")
    void fromNativeShouldThrowForZeroHandle() {
      assertThrows(
          JniException.class, () -> JniFuncType.fromNative(0), "Should throw for zero handle");
    }

    @Test
    @DisplayName("fromNative should throw for negative handle")
    void fromNativeShouldThrowForNegativeHandle() {
      assertThrows(
          JniException.class, () -> JniFuncType.fromNative(-1), "Should throw for negative handle");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("All value types should be supported as params")
    void allValueTypesShouldBeSupportedAsParams() {
      final List<WasmValueType> params =
          Arrays.asList(
              WasmValueType.I32,
              WasmValueType.I64,
              WasmValueType.F32,
              WasmValueType.F64,
              WasmValueType.V128,
              WasmValueType.FUNCREF,
              WasmValueType.EXTERNREF);
      final JniFuncType funcType = new JniFuncType(params, Collections.emptyList());
      assertEquals(7, funcType.getParams().size(), "Should support all value types as params");
    }

    @Test
    @DisplayName("All value types should be supported as results")
    void allValueTypesShouldBeSupportedAsResults() {
      final List<WasmValueType> results =
          Arrays.asList(
              WasmValueType.I32,
              WasmValueType.I64,
              WasmValueType.F32,
              WasmValueType.F64,
              WasmValueType.V128,
              WasmValueType.FUNCREF,
              WasmValueType.EXTERNREF);
      final JniFuncType funcType = new JniFuncType(Collections.emptyList(), results);
      assertEquals(7, funcType.getResults().size(), "Should support all value types as results");
    }

    @Test
    @DisplayName("Complex function signature should work")
    void complexFunctionSignatureShouldWork() {
      final List<WasmValueType> params =
          Arrays.asList(
              WasmValueType.I32, WasmValueType.I32, WasmValueType.I64, WasmValueType.EXTERNREF);
      final List<WasmValueType> results = Arrays.asList(WasmValueType.I64, WasmValueType.F64);
      final JniFuncType funcType = new JniFuncType(params, results);

      assertEquals(4, funcType.getParams().size(), "Should have 4 params");
      assertEquals(2, funcType.getResults().size(), "Should have 2 results");
      assertEquals(WasmValueType.I32, funcType.getParams().get(0), "First param should be I32");
      assertEquals(
          WasmValueType.EXTERNREF, funcType.getParams().get(3), "Fourth param should be EXTERNREF");
      assertEquals(WasmValueType.I64, funcType.getResults().get(0), "First result should be I64");
    }
  }
}
