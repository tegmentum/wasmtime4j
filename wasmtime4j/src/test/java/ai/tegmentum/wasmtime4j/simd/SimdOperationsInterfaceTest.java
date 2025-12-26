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

package ai.tegmentum.wasmtime4j.simd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SimdOperations} interface.
 *
 * <p>SimdOperations provides SIMD vector operations for WebAssembly.
 */
@DisplayName("SimdOperations Interface Tests")
class SimdOperationsInterfaceTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(SimdOperations.class.isInterface(), "SimdOperations should be an interface");
    }
  }

  @Nested
  @DisplayName("Arithmetic Operations Tests")
  class ArithmeticOperationsTests {

    @Test
    @DisplayName("should have add method")
    void shouldHaveAddMethod() throws NoSuchMethodException {
      final Method method =
          SimdOperations.class.getMethod("add", SimdVector.class, SimdVector.class);
      assertNotNull(method, "add method should exist");
      assertEquals(SimdVector.class, method.getReturnType(), "Should return SimdVector");
    }

    @Test
    @DisplayName("should have subtract method")
    void shouldHaveSubtractMethod() throws NoSuchMethodException {
      final Method method =
          SimdOperations.class.getMethod("subtract", SimdVector.class, SimdVector.class);
      assertNotNull(method, "subtract method should exist");
      assertEquals(SimdVector.class, method.getReturnType(), "Should return SimdVector");
    }

    @Test
    @DisplayName("should have multiply method")
    void shouldHaveMultiplyMethod() throws NoSuchMethodException {
      final Method method =
          SimdOperations.class.getMethod("multiply", SimdVector.class, SimdVector.class);
      assertNotNull(method, "multiply method should exist");
      assertEquals(SimdVector.class, method.getReturnType(), "Should return SimdVector");
    }

    @Test
    @DisplayName("should have divide method")
    void shouldHaveDivideMethod() throws NoSuchMethodException {
      final Method method =
          SimdOperations.class.getMethod("divide", SimdVector.class, SimdVector.class);
      assertNotNull(method, "divide method should exist");
      assertEquals(SimdVector.class, method.getReturnType(), "Should return SimdVector");
    }

    @Test
    @DisplayName("should have addSaturated method")
    void shouldHaveAddSaturatedMethod() throws NoSuchMethodException {
      final Method method =
          SimdOperations.class.getMethod("addSaturated", SimdVector.class, SimdVector.class);
      assertNotNull(method, "addSaturated method should exist");
      assertEquals(SimdVector.class, method.getReturnType(), "Should return SimdVector");
    }
  }

  @Nested
  @DisplayName("Bitwise Operations Tests")
  class BitwiseOperationsTests {

    @Test
    @DisplayName("should have and method")
    void shouldHaveAndMethod() throws NoSuchMethodException {
      final Method method =
          SimdOperations.class.getMethod("and", SimdVector.class, SimdVector.class);
      assertNotNull(method, "and method should exist");
      assertEquals(SimdVector.class, method.getReturnType(), "Should return SimdVector");
    }

    @Test
    @DisplayName("should have or method")
    void shouldHaveOrMethod() throws NoSuchMethodException {
      final Method method =
          SimdOperations.class.getMethod("or", SimdVector.class, SimdVector.class);
      assertNotNull(method, "or method should exist");
      assertEquals(SimdVector.class, method.getReturnType(), "Should return SimdVector");
    }

    @Test
    @DisplayName("should have xor method")
    void shouldHaveXorMethod() throws NoSuchMethodException {
      final Method method =
          SimdOperations.class.getMethod("xor", SimdVector.class, SimdVector.class);
      assertNotNull(method, "xor method should exist");
      assertEquals(SimdVector.class, method.getReturnType(), "Should return SimdVector");
    }

    @Test
    @DisplayName("should have not method")
    void shouldHaveNotMethod() throws NoSuchMethodException {
      final Method method = SimdOperations.class.getMethod("not", SimdVector.class);
      assertNotNull(method, "not method should exist");
      assertEquals(SimdVector.class, method.getReturnType(), "Should return SimdVector");
    }
  }

  @Nested
  @DisplayName("Comparison Operations Tests")
  class ComparisonOperationsTests {

    @Test
    @DisplayName("should have equals method")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method =
          SimdOperations.class.getMethod("equals", SimdVector.class, SimdVector.class);
      assertNotNull(method, "equals method should exist");
      assertEquals(SimdVector.class, method.getReturnType(), "Should return SimdVector");
    }

    @Test
    @DisplayName("should have lessThan method")
    void shouldHaveLessThanMethod() throws NoSuchMethodException {
      final Method method =
          SimdOperations.class.getMethod("lessThan", SimdVector.class, SimdVector.class);
      assertNotNull(method, "lessThan method should exist");
      assertEquals(SimdVector.class, method.getReturnType(), "Should return SimdVector");
    }

    @Test
    @DisplayName("should have greaterThan method")
    void shouldHaveGreaterThanMethod() throws NoSuchMethodException {
      final Method method =
          SimdOperations.class.getMethod("greaterThan", SimdVector.class, SimdVector.class);
      assertNotNull(method, "greaterThan method should exist");
      assertEquals(SimdVector.class, method.getReturnType(), "Should return SimdVector");
    }
  }

  @Nested
  @DisplayName("Lane Manipulation Tests")
  class LaneManipulationTests {

    @Test
    @DisplayName("should have extractLaneI32 method")
    void shouldHaveExtractLaneI32Method() throws NoSuchMethodException {
      final Method method =
          SimdOperations.class.getMethod("extractLaneI32", SimdVector.class, int.class);
      assertNotNull(method, "extractLaneI32 method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have replaceLaneI32 method")
    void shouldHaveReplaceLaneI32Method() throws NoSuchMethodException {
      final Method method =
          SimdOperations.class.getMethod("replaceLaneI32", SimdVector.class, int.class, int.class);
      assertNotNull(method, "replaceLaneI32 method should exist");
      assertEquals(SimdVector.class, method.getReturnType(), "Should return SimdVector");
    }
  }

  @Nested
  @DisplayName("Conversion Operations Tests")
  class ConversionOperationsTests {

    @Test
    @DisplayName("should have convertI32ToF32 method")
    void shouldHaveConvertI32ToF32Method() throws NoSuchMethodException {
      final Method method = SimdOperations.class.getMethod("convertI32ToF32", SimdVector.class);
      assertNotNull(method, "convertI32ToF32 method should exist");
      assertEquals(SimdVector.class, method.getReturnType(), "Should return SimdVector");
    }

    @Test
    @DisplayName("should have convertF32ToI32 method")
    void shouldHaveConvertF32ToI32Method() throws NoSuchMethodException {
      final Method method = SimdOperations.class.getMethod("convertF32ToI32", SimdVector.class);
      assertNotNull(method, "convertF32ToI32 method should exist");
      assertEquals(SimdVector.class, method.getReturnType(), "Should return SimdVector");
    }
  }

  @Nested
  @DisplayName("Advanced Operations Tests")
  class AdvancedOperationsTests {

    @Test
    @DisplayName("should have shuffle method")
    void shouldHaveShuffleMethod() throws NoSuchMethodException {
      final Method method =
          SimdOperations.class.getMethod(
              "shuffle", SimdVector.class, SimdVector.class, int[].class);
      assertNotNull(method, "shuffle method should exist");
      assertEquals(SimdVector.class, method.getReturnType(), "Should return SimdVector");
    }

    @Test
    @DisplayName("should have fma method")
    void shouldHaveFmaMethod() throws NoSuchMethodException {
      final Method method =
          SimdOperations.class.getMethod(
              "fma", SimdVector.class, SimdVector.class, SimdVector.class);
      assertNotNull(method, "fma method should exist");
      assertEquals(SimdVector.class, method.getReturnType(), "Should return SimdVector");
    }

    @Test
    @DisplayName("should have fms method")
    void shouldHaveFmsMethod() throws NoSuchMethodException {
      final Method method =
          SimdOperations.class.getMethod(
              "fms", SimdVector.class, SimdVector.class, SimdVector.class);
      assertNotNull(method, "fms method should exist");
      assertEquals(SimdVector.class, method.getReturnType(), "Should return SimdVector");
    }

    @Test
    @DisplayName("should have sqrt method")
    void shouldHaveSqrtMethod() throws NoSuchMethodException {
      final Method method = SimdOperations.class.getMethod("sqrt", SimdVector.class);
      assertNotNull(method, "sqrt method should exist");
      assertEquals(SimdVector.class, method.getReturnType(), "Should return SimdVector");
    }

    @Test
    @DisplayName("should have reciprocal method")
    void shouldHaveReciprocalMethod() throws NoSuchMethodException {
      final Method method = SimdOperations.class.getMethod("reciprocal", SimdVector.class);
      assertNotNull(method, "reciprocal method should exist");
      assertEquals(SimdVector.class, method.getReturnType(), "Should return SimdVector");
    }

    @Test
    @DisplayName("should have rsqrt method")
    void shouldHaveRsqrtMethod() throws NoSuchMethodException {
      final Method method = SimdOperations.class.getMethod("rsqrt", SimdVector.class);
      assertNotNull(method, "rsqrt method should exist");
      assertEquals(SimdVector.class, method.getReturnType(), "Should return SimdVector");
    }

    @Test
    @DisplayName("should have popcount method")
    void shouldHavePopcountMethod() throws NoSuchMethodException {
      final Method method = SimdOperations.class.getMethod("popcount", SimdVector.class);
      assertNotNull(method, "popcount method should exist");
      assertEquals(SimdVector.class, method.getReturnType(), "Should return SimdVector");
    }
  }

  @Nested
  @DisplayName("Reduction Operations Tests")
  class ReductionOperationsTests {

    @Test
    @DisplayName("should have horizontalSum method")
    void shouldHaveHorizontalSumMethod() throws NoSuchMethodException {
      final Method method = SimdOperations.class.getMethod("horizontalSum", SimdVector.class);
      assertNotNull(method, "horizontalSum method should exist");
      assertEquals(float.class, method.getReturnType(), "Should return float");
    }

    @Test
    @DisplayName("should have horizontalMin method")
    void shouldHaveHorizontalMinMethod() throws NoSuchMethodException {
      final Method method = SimdOperations.class.getMethod("horizontalMin", SimdVector.class);
      assertNotNull(method, "horizontalMin method should exist");
      assertEquals(float.class, method.getReturnType(), "Should return float");
    }

    @Test
    @DisplayName("should have horizontalMax method")
    void shouldHaveHorizontalMaxMethod() throws NoSuchMethodException {
      final Method method = SimdOperations.class.getMethod("horizontalMax", SimdVector.class);
      assertNotNull(method, "horizontalMax method should exist");
      assertEquals(float.class, method.getReturnType(), "Should return float");
    }
  }

  @Nested
  @DisplayName("Capability Methods Tests")
  class CapabilityMethodsTests {

    @Test
    @DisplayName("should have isSimdSupported method")
    void shouldHaveIsSimdSupportedMethod() throws NoSuchMethodException {
      final Method method = SimdOperations.class.getMethod("isSimdSupported");
      assertNotNull(method, "isSimdSupported method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getSimdCapabilities method")
    void shouldHaveGetSimdCapabilitiesMethod() throws NoSuchMethodException {
      final Method method = SimdOperations.class.getMethod("getSimdCapabilities");
      assertNotNull(method, "getSimdCapabilities method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("Select/Blend Operations Tests")
  class SelectBlendOperationsTests {

    @Test
    @DisplayName("should have select method")
    void shouldHaveSelectMethod() throws NoSuchMethodException {
      final Method method =
          SimdOperations.class.getMethod(
              "select", SimdVector.class, SimdVector.class, SimdVector.class);
      assertNotNull(method, "select method should exist");
      assertEquals(SimdVector.class, method.getReturnType(), "Should return SimdVector");
    }

    @Test
    @DisplayName("should have blend method")
    void shouldHaveBlendMethod() throws NoSuchMethodException {
      final Method method =
          SimdOperations.class.getMethod("blend", SimdVector.class, SimdVector.class, int.class);
      assertNotNull(method, "blend method should exist");
      assertEquals(SimdVector.class, method.getReturnType(), "Should return SimdVector");
    }
  }

  @Nested
  @DisplayName("Relaxed SIMD Operations Tests")
  class RelaxedSimdOperationsTests {

    @Test
    @DisplayName("should have relaxedAdd method")
    void shouldHaveRelaxedAddMethod() throws NoSuchMethodException {
      final Method method =
          SimdOperations.class.getMethod("relaxedAdd", SimdVector.class, SimdVector.class);
      assertNotNull(method, "relaxedAdd method should exist");
      assertEquals(SimdVector.class, method.getReturnType(), "Should return SimdVector");
    }
  }
}
