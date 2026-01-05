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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the FunctionContext interface.
 *
 * <p>FunctionContext represents the execution context of a WebAssembly function.
 */
@DisplayName("FunctionContext Interface Tests")
class FunctionContextTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(FunctionContext.class.isInterface(), "FunctionContext should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(FunctionContext.class.getModifiers()),
          "FunctionContext should be public");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getFunctionName method")
    void shouldHaveGetFunctionNameMethod() throws NoSuchMethodException {
      Method method = FunctionContext.class.getMethod("getFunctionName");
      assertNotNull(method, "getFunctionName should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
    }

    @Test
    @DisplayName("should have getFunctionIndex method")
    void shouldHaveGetFunctionIndexMethod() throws NoSuchMethodException {
      Method method = FunctionContext.class.getMethod("getFunctionIndex");
      assertNotNull(method, "getFunctionIndex should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getFunctionType method")
    void shouldHaveGetFunctionTypeMethod() throws NoSuchMethodException {
      Method method = FunctionContext.class.getMethod("getFunctionType");
      assertNotNull(method, "getFunctionType should exist");
      assertEquals(FunctionType.class, method.getReturnType(), "Should return FunctionType");
    }

    @Test
    @DisplayName("should have getParameterTypes default method")
    void shouldHaveGetParameterTypesMethod() throws NoSuchMethodException {
      Method method = FunctionContext.class.getMethod("getParameterTypes");
      assertNotNull(method, "getParameterTypes should exist");
      assertEquals(WasmValueType[].class, method.getReturnType(), "Should return WasmValueType[]");
      assertTrue(method.isDefault(), "getParameterTypes should be a default method");
    }

    @Test
    @DisplayName("should have getReturnTypes default method")
    void shouldHaveGetReturnTypesMethod() throws NoSuchMethodException {
      Method method = FunctionContext.class.getMethod("getReturnTypes");
      assertNotNull(method, "getReturnTypes should exist");
      assertEquals(WasmValueType[].class, method.getReturnType(), "Should return WasmValueType[]");
      assertTrue(method.isDefault(), "getReturnTypes should be a default method");
    }

    @Test
    @DisplayName("should have supportsTailCalls method")
    void shouldHaveSupportsTailCallsMethod() throws NoSuchMethodException {
      Method method = FunctionContext.class.getMethod("supportsTailCalls");
      assertNotNull(method, "supportsTailCalls should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getStackFrameSize method")
    void shouldHaveGetStackFrameSizeMethod() throws NoSuchMethodException {
      Method method = FunctionContext.class.getMethod("getStackFrameSize");
      assertNotNull(method, "getStackFrameSize should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getLocalCount method")
    void shouldHaveGetLocalCountMethod() throws NoSuchMethodException {
      Method method = FunctionContext.class.getMethod("getLocalCount");
      assertNotNull(method, "getLocalCount should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getLocalTypes method")
    void shouldHaveGetLocalTypesMethod() throws NoSuchMethodException {
      Method method = FunctionContext.class.getMethod("getLocalTypes");
      assertNotNull(method, "getLocalTypes should exist");
      assertEquals(WasmValueType[].class, method.getReturnType(), "Should return WasmValueType[]");
    }

    @Test
    @DisplayName("should have isRecursive method")
    void shouldHaveIsRecursiveMethod() throws NoSuchMethodException {
      Method method = FunctionContext.class.getMethod("isRecursive");
      assertNotNull(method, "isRecursive should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isTailRecursive method")
    void shouldHaveIsTailRecursiveMethod() throws NoSuchMethodException {
      Method method = FunctionContext.class.getMethod("isTailRecursive");
      assertNotNull(method, "isTailRecursive should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getRecursionDepthLimit method")
    void shouldHaveGetRecursionDepthLimitMethod() throws NoSuchMethodException {
      Method method = FunctionContext.class.getMethod("getRecursionDepthLimit");
      assertNotNull(method, "getRecursionDepthLimit should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getCurrentCallDepth method")
    void shouldHaveGetCurrentCallDepthMethod() throws NoSuchMethodException {
      Method method = FunctionContext.class.getMethod("getCurrentCallDepth");
      assertNotNull(method, "getCurrentCallDepth should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getOptimization method")
    void shouldHaveGetOptimizationMethod() throws NoSuchMethodException {
      Method method = FunctionContext.class.getMethod("getOptimization");
      assertNotNull(method, "getOptimization should exist");
      assertEquals(
          FunctionContext.FunctionOptimization.class,
          method.getReturnType(),
          "Should return FunctionOptimization");
    }

    @Test
    @DisplayName("should have getMetrics method")
    void shouldHaveGetMetricsMethod() throws NoSuchMethodException {
      Method method = FunctionContext.class.getMethod("getMetrics");
      assertNotNull(method, "getMetrics should exist");
      assertEquals(
          FunctionContext.FunctionMetrics.class,
          method.getReturnType(),
          "Should return FunctionMetrics");
    }
  }

  // ========================================================================
  // Static Factory Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have create static method with 3 parameters")
    void shouldHaveCreateWith3Parameters() throws NoSuchMethodException {
      Method method =
          FunctionContext.class.getMethod("create", String.class, int.class, FunctionType.class);
      assertNotNull(method, "create(3 params) should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be static");
      assertEquals(FunctionContext.class, method.getReturnType(), "Should return FunctionContext");
    }

    @Test
    @DisplayName("should have create static method with 5 parameters")
    void shouldHaveCreateWith5Parameters() throws NoSuchMethodException {
      Method method =
          FunctionContext.class.getMethod(
              "create", String.class, int.class, FunctionType.class, boolean.class, int.class);
      assertNotNull(method, "create(5 params) should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be static");
      assertEquals(FunctionContext.class, method.getReturnType(), "Should return FunctionContext");
    }
  }

  // ========================================================================
  // Nested Interfaces Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Interfaces Tests")
  class NestedInterfacesTests {

    @Test
    @DisplayName("should have FunctionOptimization nested interface")
    void shouldHaveFunctionOptimizationNestedInterface() {
      Class<?>[] nestedClasses = FunctionContext.class.getDeclaredClasses();
      boolean hasInterface =
          Arrays.stream(nestedClasses)
              .anyMatch(c -> c.getSimpleName().equals("FunctionOptimization") && c.isInterface());
      assertTrue(hasInterface, "Should have FunctionOptimization nested interface");
    }

    @Test
    @DisplayName("should have FunctionMetrics nested interface")
    void shouldHaveFunctionMetricsNestedInterface() {
      Class<?>[] nestedClasses = FunctionContext.class.getDeclaredClasses();
      boolean hasInterface =
          Arrays.stream(nestedClasses)
              .anyMatch(c -> c.getSimpleName().equals("FunctionMetrics") && c.isInterface());
      assertTrue(hasInterface, "Should have FunctionMetrics nested interface");
    }

    @Test
    @DisplayName("should have FunctionOptimizationImpl nested class")
    void shouldHaveFunctionOptimizationImplNestedClass() {
      Class<?>[] nestedClasses = FunctionContext.class.getDeclaredClasses();
      boolean hasClass =
          Arrays.stream(nestedClasses)
              .anyMatch(
                  c -> c.getSimpleName().equals("FunctionOptimizationImpl") && !c.isInterface());
      assertTrue(hasClass, "Should have FunctionOptimizationImpl nested class");
    }

    @Test
    @DisplayName("should have FunctionMetricsImpl nested class")
    void shouldHaveFunctionMetricsImplNestedClass() {
      Class<?>[] nestedClasses = FunctionContext.class.getDeclaredClasses();
      boolean hasClass =
          Arrays.stream(nestedClasses)
              .anyMatch(c -> c.getSimpleName().equals("FunctionMetricsImpl") && !c.isInterface());
      assertTrue(hasClass, "Should have FunctionMetricsImpl nested class");
    }
  }

  // ========================================================================
  // FunctionOptimization Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("FunctionOptimization Interface Tests")
  class FunctionOptimizationTests {

    @Test
    @DisplayName("should have isJitCompiled method")
    void shouldHaveIsJitCompiledMethod() throws NoSuchMethodException {
      Method method = FunctionContext.FunctionOptimization.class.getMethod("isJitCompiled");
      assertNotNull(method, "isJitCompiled should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isInlined method")
    void shouldHaveIsInlinedMethod() throws NoSuchMethodException {
      Method method = FunctionContext.FunctionOptimization.class.getMethod("isInlined");
      assertNotNull(method, "isInlined should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have hasTailCallOptimization method")
    void shouldHaveHasTailCallOptimizationMethod() throws NoSuchMethodException {
      Method method =
          FunctionContext.FunctionOptimization.class.getMethod("hasTailCallOptimization");
      assertNotNull(method, "hasTailCallOptimization should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have hasFramePointerElimination method")
    void shouldHaveHasFramePointerEliminationMethod() throws NoSuchMethodException {
      Method method =
          FunctionContext.FunctionOptimization.class.getMethod("hasFramePointerElimination");
      assertNotNull(method, "hasFramePointerElimination should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getOptimizationLevel method")
    void shouldHaveGetOptimizationLevelMethod() throws NoSuchMethodException {
      Method method = FunctionContext.FunctionOptimization.class.getMethod("getOptimizationLevel");
      assertNotNull(method, "getOptimizationLevel should exist");
      assertEquals(
          WasmExecutionContext.OptimizationLevel.class,
          method.getReturnType(),
          "Should return OptimizationLevel");
    }

    @Test
    @DisplayName("should have getCompilationCost method")
    void shouldHaveGetCompilationCostMethod() throws NoSuchMethodException {
      Method method = FunctionContext.FunctionOptimization.class.getMethod("getCompilationCost");
      assertNotNull(method, "getCompilationCost should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getPerformanceGain method")
    void shouldHaveGetPerformanceGainMethod() throws NoSuchMethodException {
      Method method = FunctionContext.FunctionOptimization.class.getMethod("getPerformanceGain");
      assertNotNull(method, "getPerformanceGain should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }
  }

  // ========================================================================
  // FunctionMetrics Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("FunctionMetrics Interface Tests")
  class FunctionMetricsTests {

    @Test
    @DisplayName("should have getCallCount method")
    void shouldHaveGetCallCountMethod() throws NoSuchMethodException {
      Method method = FunctionContext.FunctionMetrics.class.getMethod("getCallCount");
      assertNotNull(method, "getCallCount should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getTotalExecutionTime method")
    void shouldHaveGetTotalExecutionTimeMethod() throws NoSuchMethodException {
      Method method = FunctionContext.FunctionMetrics.class.getMethod("getTotalExecutionTime");
      assertNotNull(method, "getTotalExecutionTime should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getAverageExecutionTime method")
    void shouldHaveGetAverageExecutionTimeMethod() throws NoSuchMethodException {
      Method method = FunctionContext.FunctionMetrics.class.getMethod("getAverageExecutionTime");
      assertNotNull(method, "getAverageExecutionTime should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }

    @Test
    @DisplayName("should have getPeakMemoryUsage method")
    void shouldHaveGetPeakMemoryUsageMethod() throws NoSuchMethodException {
      Method method = FunctionContext.FunctionMetrics.class.getMethod("getPeakMemoryUsage");
      assertNotNull(method, "getPeakMemoryUsage should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getTailCallCount method")
    void shouldHaveGetTailCallCountMethod() throws NoSuchMethodException {
      Method method = FunctionContext.FunctionMetrics.class.getMethod("getTailCallCount");
      assertNotNull(method, "getTailCallCount should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getOptimizedTailCallCount method")
    void shouldHaveGetOptimizedTailCallCountMethod() throws NoSuchMethodException {
      Method method = FunctionContext.FunctionMetrics.class.getMethod("getOptimizedTailCallCount");
      assertNotNull(method, "getOptimizedTailCallCount should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getStackSpaceSaved method")
    void shouldHaveGetStackSpaceSavedMethod() throws NoSuchMethodException {
      Method method = FunctionContext.FunctionMetrics.class.getMethod("getStackSpaceSaved");
      assertNotNull(method, "getStackSpaceSaved should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected methods")
    void shouldHaveAllExpectedMethods() {
      Set<String> expectedMethods =
          Set.of(
              "getFunctionName",
              "getFunctionIndex",
              "getFunctionType",
              "getParameterTypes",
              "getReturnTypes",
              "supportsTailCalls",
              "getStackFrameSize",
              "getLocalCount",
              "getLocalTypes",
              "isRecursive",
              "isTailRecursive",
              "getRecursionDepthLimit",
              "getCurrentCallDepth",
              "getOptimization",
              "getMetrics",
              "create");

      Set<String> actualMethods =
          Arrays.stream(FunctionContext.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Should have method: " + expected);
      }
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should not extend any interface")
    void shouldNotExtendAnyInterface() {
      assertEquals(
          0,
          FunctionContext.class.getInterfaces().length,
          "FunctionContext should not extend any interface");
    }
  }
}
