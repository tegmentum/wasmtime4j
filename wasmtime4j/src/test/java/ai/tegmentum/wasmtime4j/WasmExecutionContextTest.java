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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasmExecutionContext interface.
 *
 * <p>WasmExecutionContext represents the execution context for WebAssembly operations.
 */
@DisplayName("WasmExecutionContext Interface Tests")
class WasmExecutionContextTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          WasmExecutionContext.class.isInterface(), "WasmExecutionContext should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasmExecutionContext.class.getModifiers()),
          "WasmExecutionContext should be public");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have applyBranchHint method")
    void shouldHaveApplyBranchHintMethod() throws NoSuchMethodException {
      Method method =
          WasmExecutionContext.class.getMethod(
              "applyBranchHint", BranchHintingInstructions.class, long.class, Object[].class);
      assertNotNull(method, "applyBranchHint should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getExecutionStatistics method")
    void shouldHaveGetExecutionStatisticsMethod() throws NoSuchMethodException {
      Method method = WasmExecutionContext.class.getMethod("getExecutionStatistics");
      assertNotNull(method, "getExecutionStatistics should exist");
      assertEquals(
          WasmExecutionContext.ExecutionStatistics.class,
          method.getReturnType(),
          "Should return ExecutionStatistics");
    }

    @Test
    @DisplayName("should have isProfileGuidedOptimizationEnabled method")
    void shouldHaveIsProfileGuidedOptimizationEnabledMethod() throws NoSuchMethodException {
      Method method = WasmExecutionContext.class.getMethod("isProfileGuidedOptimizationEnabled");
      assertNotNull(method, "isProfileGuidedOptimizationEnabled should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getOptimizationLevel method")
    void shouldHaveGetOptimizationLevelMethod() throws NoSuchMethodException {
      Method method = WasmExecutionContext.class.getMethod("getOptimizationLevel");
      assertNotNull(method, "getOptimizationLevel should exist");
      assertEquals(
          WasmExecutionContext.OptimizationLevel.class,
          method.getReturnType(),
          "Should return OptimizationLevel");
    }

    @Test
    @DisplayName("should have setOptimizationLevel method")
    void shouldHaveSetOptimizationLevelMethod() throws NoSuchMethodException {
      Method method =
          WasmExecutionContext.class.getMethod(
              "setOptimizationLevel", WasmExecutionContext.OptimizationLevel.class);
      assertNotNull(method, "setOptimizationLevel should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getBranchPredictionStatistics method")
    void shouldHaveGetBranchPredictionStatisticsMethod() throws NoSuchMethodException {
      Method method = WasmExecutionContext.class.getMethod("getBranchPredictionStatistics");
      assertNotNull(method, "getBranchPredictionStatistics should exist");
      assertEquals(
          WasmExecutionContext.BranchPredictionStatistics.class,
          method.getReturnType(),
          "Should return BranchPredictionStatistics");
    }

    @Test
    @DisplayName("should have resetStatistics method")
    void shouldHaveResetStatisticsMethod() throws NoSuchMethodException {
      Method method = WasmExecutionContext.class.getMethod("resetStatistics");
      assertNotNull(method, "resetStatistics should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  // ========================================================================
  // Static Factory Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have createDefault static method")
    void shouldHaveCreateDefaultMethod() throws NoSuchMethodException {
      Method method = WasmExecutionContext.class.getMethod("createDefault");
      assertNotNull(method, "createDefault should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "createDefault should be static");
      assertEquals(
          WasmExecutionContext.class, method.getReturnType(), "Should return WasmExecutionContext");
    }

    @Test
    @DisplayName("should have create static method with 2 parameters")
    void shouldHaveCreateWith2Parameters() throws NoSuchMethodException {
      Method method =
          WasmExecutionContext.class.getMethod(
              "create", WasmExecutionContext.OptimizationLevel.class, boolean.class);
      assertNotNull(method, "create should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be static");
      assertEquals(
          WasmExecutionContext.class, method.getReturnType(), "Should return WasmExecutionContext");
    }
  }

  // ========================================================================
  // OptimizationLevel Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("OptimizationLevel Enum Tests")
  class OptimizationLevelEnumTests {

    @Test
    @DisplayName("should have NONE value")
    void shouldHaveNoneValue() {
      assertNotNull(
          WasmExecutionContext.OptimizationLevel.valueOf("NONE"), "Should have NONE value");
    }

    @Test
    @DisplayName("should have BASIC value")
    void shouldHaveBasicValue() {
      assertNotNull(
          WasmExecutionContext.OptimizationLevel.valueOf("BASIC"), "Should have BASIC value");
    }

    @Test
    @DisplayName("should have AGGRESSIVE value")
    void shouldHaveAggressiveValue() {
      assertNotNull(
          WasmExecutionContext.OptimizationLevel.valueOf("AGGRESSIVE"),
          "Should have AGGRESSIVE value");
    }

    @Test
    @DisplayName("should have MAXIMUM value")
    void shouldHaveMaximumValue() {
      assertNotNull(
          WasmExecutionContext.OptimizationLevel.valueOf("MAXIMUM"), "Should have MAXIMUM value");
    }

    @Test
    @DisplayName("should have exactly 4 values")
    void shouldHaveExactly4Values() {
      assertEquals(
          4,
          WasmExecutionContext.OptimizationLevel.values().length,
          "Should have exactly 4 enum values");
    }

    @Test
    @DisplayName("should have getLevel method")
    void shouldHaveGetLevelMethod() throws NoSuchMethodException {
      Method method = WasmExecutionContext.OptimizationLevel.class.getMethod("getLevel");
      assertNotNull(method, "getLevel should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have isAtLeast method")
    void shouldHaveIsAtLeastMethod() throws NoSuchMethodException {
      Method method =
          WasmExecutionContext.OptimizationLevel.class.getMethod(
              "isAtLeast", WasmExecutionContext.OptimizationLevel.class);
      assertNotNull(method, "isAtLeast should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  // ========================================================================
  // ExecutionStatistics Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("ExecutionStatistics Interface Tests")
  class ExecutionStatisticsTests {

    @Test
    @DisplayName("should have getInstructionCount method")
    void shouldHaveGetInstructionCountMethod() throws NoSuchMethodException {
      Method method =
          WasmExecutionContext.ExecutionStatistics.class.getMethod("getInstructionCount");
      assertNotNull(method, "getInstructionCount should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getFunctionCallCount method")
    void shouldHaveGetFunctionCallCountMethod() throws NoSuchMethodException {
      Method method =
          WasmExecutionContext.ExecutionStatistics.class.getMethod("getFunctionCallCount");
      assertNotNull(method, "getFunctionCallCount should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getBranchCount method")
    void shouldHaveGetBranchCountMethod() throws NoSuchMethodException {
      Method method = WasmExecutionContext.ExecutionStatistics.class.getMethod("getBranchCount");
      assertNotNull(method, "getBranchCount should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getCorrectBranchPredictions method")
    void shouldHaveGetCorrectBranchPredictionsMethod() throws NoSuchMethodException {
      Method method =
          WasmExecutionContext.ExecutionStatistics.class.getMethod("getCorrectBranchPredictions");
      assertNotNull(method, "getCorrectBranchPredictions should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getExecutionTimeNanos method")
    void shouldHaveGetExecutionTimeNanosMethod() throws NoSuchMethodException {
      Method method =
          WasmExecutionContext.ExecutionStatistics.class.getMethod("getExecutionTimeNanos");
      assertNotNull(method, "getExecutionTimeNanos should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getMemoryStatistics method")
    void shouldHaveGetMemoryStatisticsMethod() throws NoSuchMethodException {
      Method method =
          WasmExecutionContext.ExecutionStatistics.class.getMethod("getMemoryStatistics");
      assertNotNull(method, "getMemoryStatistics should exist");
      assertEquals(
          WasmExecutionContext.ExecutionStatistics.MemoryStatistics.class,
          method.getReturnType(),
          "Should return MemoryStatistics");
    }

    @Test
    @DisplayName("should have getHotSpotAnalysis method")
    void shouldHaveGetHotSpotAnalysisMethod() throws NoSuchMethodException {
      Method method =
          WasmExecutionContext.ExecutionStatistics.class.getMethod("getHotSpotAnalysis");
      assertNotNull(method, "getHotSpotAnalysis should exist");
      assertEquals(
          WasmExecutionContext.ExecutionStatistics.HotSpotAnalysis.class,
          method.getReturnType(),
          "Should return HotSpotAnalysis");
    }
  }

  // ========================================================================
  // BranchPredictionStatistics Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("BranchPredictionStatistics Interface Tests")
  class BranchPredictionStatisticsTests {

    @Test
    @DisplayName("should have getTotalPredictions method")
    void shouldHaveGetTotalPredictionsMethod() throws NoSuchMethodException {
      Method method =
          WasmExecutionContext.BranchPredictionStatistics.class.getMethod("getTotalPredictions");
      assertNotNull(method, "getTotalPredictions should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getCorrectPredictions method")
    void shouldHaveGetCorrectPredictionsMethod() throws NoSuchMethodException {
      Method method =
          WasmExecutionContext.BranchPredictionStatistics.class.getMethod("getCorrectPredictions");
      assertNotNull(method, "getCorrectPredictions should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getAccuracy method")
    void shouldHaveGetAccuracyMethod() throws NoSuchMethodException {
      Method method =
          WasmExecutionContext.BranchPredictionStatistics.class.getMethod("getAccuracy");
      assertNotNull(method, "getAccuracy should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }

    @Test
    @DisplayName("should have getMispredictionsByType method")
    void shouldHaveGetMispredictionsByTypeMethod() throws NoSuchMethodException {
      Method method =
          WasmExecutionContext.BranchPredictionStatistics.class.getMethod(
              "getMispredictionsByType");
      assertNotNull(method, "getMispredictionsByType should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getTopMispredictions method")
    void shouldHaveGetTopMispredictionsMethod() throws NoSuchMethodException {
      Method method =
          WasmExecutionContext.BranchPredictionStatistics.class.getMethod("getTopMispredictions");
      assertNotNull(method, "getTopMispredictions should exist");
      assertEquals(
          WasmExecutionContext.BranchPredictionStatistics.MispredictedBranch[].class,
          method.getReturnType(),
          "Should return MispredictedBranch[]");
    }
  }

  // ========================================================================
  // Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have OptimizationLevel nested enum")
    void shouldHaveOptimizationLevelNestedEnum() {
      Class<?>[] nestedClasses = WasmExecutionContext.class.getDeclaredClasses();
      boolean hasEnum =
          Arrays.stream(nestedClasses)
              .anyMatch(c -> c.getSimpleName().equals("OptimizationLevel") && c.isEnum());
      assertTrue(hasEnum, "Should have OptimizationLevel nested enum");
    }

    @Test
    @DisplayName("should have ExecutionStatistics nested interface")
    void shouldHaveExecutionStatisticsNestedInterface() {
      Class<?>[] nestedClasses = WasmExecutionContext.class.getDeclaredClasses();
      boolean hasInterface =
          Arrays.stream(nestedClasses)
              .anyMatch(c -> c.getSimpleName().equals("ExecutionStatistics") && c.isInterface());
      assertTrue(hasInterface, "Should have ExecutionStatistics nested interface");
    }

    @Test
    @DisplayName("should have BranchPredictionStatistics nested interface")
    void shouldHaveBranchPredictionStatisticsNestedInterface() {
      Class<?>[] nestedClasses = WasmExecutionContext.class.getDeclaredClasses();
      boolean hasInterface =
          Arrays.stream(nestedClasses)
              .anyMatch(
                  c -> c.getSimpleName().equals("BranchPredictionStatistics") && c.isInterface());
      assertTrue(hasInterface, "Should have BranchPredictionStatistics nested interface");
    }
  }

  // ========================================================================
  // MemoryStatistics Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("MemoryStatistics Interface Tests")
  class MemoryStatisticsTests {

    @Test
    @DisplayName("should have getTotalAllocatedBytes method")
    void shouldHaveGetTotalAllocatedBytesMethod() throws NoSuchMethodException {
      Method method =
          WasmExecutionContext.ExecutionStatistics.MemoryStatistics.class.getMethod(
              "getTotalAllocatedBytes");
      assertNotNull(method, "getTotalAllocatedBytes should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getCurrentUsageBytes method")
    void shouldHaveGetCurrentUsageBytesMethod() throws NoSuchMethodException {
      Method method =
          WasmExecutionContext.ExecutionStatistics.MemoryStatistics.class.getMethod(
              "getCurrentUsageBytes");
      assertNotNull(method, "getCurrentUsageBytes should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getPeakUsageBytes method")
    void shouldHaveGetPeakUsageBytesMethod() throws NoSuchMethodException {
      Method method =
          WasmExecutionContext.ExecutionStatistics.MemoryStatistics.class.getMethod(
              "getPeakUsageBytes");
      assertNotNull(method, "getPeakUsageBytes should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getGarbageCollections method")
    void shouldHaveGetGarbageCollectionsMethod() throws NoSuchMethodException {
      Method method =
          WasmExecutionContext.ExecutionStatistics.MemoryStatistics.class.getMethod(
              "getGarbageCollections");
      assertNotNull(method, "getGarbageCollections should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  // ========================================================================
  // HotSpotAnalysis Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("HotSpotAnalysis Interface Tests")
  class HotSpotAnalysisTests {

    @Test
    @DisplayName("should have getHotFunctions method")
    void shouldHaveGetHotFunctionsMethod() throws NoSuchMethodException {
      Method method =
          WasmExecutionContext.ExecutionStatistics.HotSpotAnalysis.class.getMethod(
              "getHotFunctions");
      assertNotNull(method, "getHotFunctions should exist");
      assertEquals(
          WasmExecutionContext.ExecutionStatistics.HotSpotAnalysis.FunctionHotSpot[].class,
          method.getReturnType(),
          "Should return FunctionHotSpot[]");
    }

    @Test
    @DisplayName("should have getHotBranches method")
    void shouldHaveGetHotBranchesMethod() throws NoSuchMethodException {
      Method method =
          WasmExecutionContext.ExecutionStatistics.HotSpotAnalysis.class.getMethod(
              "getHotBranches");
      assertNotNull(method, "getHotBranches should exist");
      assertEquals(
          WasmExecutionContext.ExecutionStatistics.HotSpotAnalysis.BranchHotSpot[].class,
          method.getReturnType(),
          "Should return BranchHotSpot[]");
    }

    @Test
    @DisplayName("should have getMemoryHotSpots method")
    void shouldHaveGetMemoryHotSpotsMethod() throws NoSuchMethodException {
      Method method =
          WasmExecutionContext.ExecutionStatistics.HotSpotAnalysis.class.getMethod(
              "getMemoryHotSpots");
      assertNotNull(method, "getMemoryHotSpots should exist");
      assertEquals(
          WasmExecutionContext.ExecutionStatistics.HotSpotAnalysis.MemoryHotSpot[].class,
          method.getReturnType(),
          "Should return MemoryHotSpot[]");
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
              "applyBranchHint",
              "getExecutionStatistics",
              "isProfileGuidedOptimizationEnabled",
              "getOptimizationLevel",
              "setOptimizationLevel",
              "getBranchPredictionStatistics",
              "resetStatistics",
              "createDefault",
              "create");

      Set<String> actualMethods =
          Arrays.stream(WasmExecutionContext.class.getDeclaredMethods())
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
          WasmExecutionContext.class.getInterfaces().length,
          "WasmExecutionContext should not extend any interface");
    }
  }
}
