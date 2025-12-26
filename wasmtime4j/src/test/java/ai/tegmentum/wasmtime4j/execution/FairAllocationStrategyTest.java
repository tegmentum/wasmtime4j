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

package ai.tegmentum.wasmtime4j.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the FairAllocationStrategy interface.
 *
 * <p>This test class verifies the interface structure, methods, nested types, and enums for
 * FairAllocationStrategy using reflection-based testing.
 */
@DisplayName("FairAllocationStrategy Tests")
class FairAllocationStrategyTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("FairAllocationStrategy should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          FairAllocationStrategy.class.isInterface(),
          "FairAllocationStrategy should be an interface");
    }

    @Test
    @DisplayName("FairAllocationStrategy should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(FairAllocationStrategy.class.getModifiers()),
          "FairAllocationStrategy should be public");
    }

    @Test
    @DisplayName("FairAllocationStrategy should not extend other interfaces")
    void shouldNotExtendOtherInterfaces() {
      Class<?>[] interfaces = FairAllocationStrategy.class.getInterfaces();
      assertEquals(
          0, interfaces.length, "FairAllocationStrategy should not extend other interfaces");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      Method method = FairAllocationStrategy.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have getAlgorithm method")
    void shouldHaveGetAlgorithmMethod() throws NoSuchMethodException {
      Method method = FairAllocationStrategy.class.getMethod("getAlgorithm");
      assertNotNull(method, "getAlgorithm method should exist");
      assertEquals(
          FairAllocationStrategy.AllocationAlgorithm.class,
          method.getReturnType(),
          "Return type should be AllocationAlgorithm");
    }

    @Test
    @DisplayName("should have setAlgorithm method")
    void shouldHaveSetAlgorithmMethod() throws NoSuchMethodException {
      Method method =
          FairAllocationStrategy.class.getMethod(
              "setAlgorithm", FairAllocationStrategy.AllocationAlgorithm.class);
      assertNotNull(method, "setAlgorithm method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getFairnessMetric method")
    void shouldHaveGetFairnessMetricMethod() throws NoSuchMethodException {
      Method method = FairAllocationStrategy.class.getMethod("getFairnessMetric");
      assertNotNull(method, "getFairnessMetric method should exist");
      assertEquals(
          FairAllocationStrategy.FairnessMetric.class,
          method.getReturnType(),
          "Return type should be FairnessMetric");
    }

    @Test
    @DisplayName("should have setFairnessMetric method")
    void shouldHaveSetFairnessMetricMethod() throws NoSuchMethodException {
      Method method =
          FairAllocationStrategy.class.getMethod(
              "setFairnessMetric", FairAllocationStrategy.FairnessMetric.class);
      assertNotNull(method, "setFairnessMetric method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have allocate method")
    void shouldHaveAllocateMethod() throws NoSuchMethodException {
      Method method = FairAllocationStrategy.class.getMethod("allocate", List.class);
      assertNotNull(method, "allocate method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have getConstraints method")
    void shouldHaveGetConstraintsMethod() throws NoSuchMethodException {
      Method method = FairAllocationStrategy.class.getMethod("getConstraints");
      assertNotNull(method, "getConstraints method should exist");
      assertEquals(
          FairAllocationStrategy.AllocationConstraints.class,
          method.getReturnType(),
          "Return type should be AllocationConstraints");
    }

    @Test
    @DisplayName("should have setConstraints method")
    void shouldHaveSetConstraintsMethod() throws NoSuchMethodException {
      Method method =
          FairAllocationStrategy.class.getMethod(
              "setConstraints", FairAllocationStrategy.AllocationConstraints.class);
      assertNotNull(method, "setConstraints method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getHistory method")
    void shouldHaveGetHistoryMethod() throws NoSuchMethodException {
      Method method = FairAllocationStrategy.class.getMethod("getHistory");
      assertNotNull(method, "getHistory method should exist");
      assertEquals(
          FairAllocationStrategy.AllocationHistory.class,
          method.getReturnType(),
          "Return type should be AllocationHistory");
    }

    @Test
    @DisplayName("should have evaluateFairness method")
    void shouldHaveEvaluateFairnessMethod() throws NoSuchMethodException {
      Method method = FairAllocationStrategy.class.getMethod("evaluateFairness");
      assertNotNull(method, "evaluateFairness method should exist");
      assertEquals(
          FairAllocationStrategy.FairnessEvaluation.class,
          method.getReturnType(),
          "Return type should be FairnessEvaluation");
    }
  }

  // ========================================================================
  // AllocationAlgorithm Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("AllocationAlgorithm Enum Tests")
  class AllocationAlgorithmTests {

    @Test
    @DisplayName("AllocationAlgorithm should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          FairAllocationStrategy.AllocationAlgorithm.class.isEnum(),
          "AllocationAlgorithm should be an enum");
      assertTrue(
          FairAllocationStrategy.AllocationAlgorithm.class.isMemberClass(),
          "AllocationAlgorithm should be a member class");
    }

    @Test
    @DisplayName("AllocationAlgorithm should have 5 values")
    void shouldHaveFiveValues() {
      FairAllocationStrategy.AllocationAlgorithm[] values =
          FairAllocationStrategy.AllocationAlgorithm.values();
      assertEquals(5, values.length, "AllocationAlgorithm should have 5 values");
    }

    @Test
    @DisplayName("AllocationAlgorithm should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames =
          Set.of(
              "ROUND_ROBIN",
              "WEIGHTED_FAIR_QUEUING",
              "PROPORTIONAL_SHARE",
              "LOTTERY_SCHEDULING",
              "DEFICIT_ROUND_ROBIN");
      Set<String> actualNames = new HashSet<>();
      for (FairAllocationStrategy.AllocationAlgorithm alg :
          FairAllocationStrategy.AllocationAlgorithm.values()) {
        actualNames.add(alg.name());
      }
      assertEquals(expectedNames, actualNames, "AllocationAlgorithm should have expected values");
    }
  }

  // ========================================================================
  // FairnessMetric Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("FairnessMetric Enum Tests")
  class FairnessMetricTests {

    @Test
    @DisplayName("FairnessMetric should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          FairAllocationStrategy.FairnessMetric.class.isEnum(), "FairnessMetric should be an enum");
    }

    @Test
    @DisplayName("FairnessMetric should have 4 values")
    void shouldHaveFourValues() {
      FairAllocationStrategy.FairnessMetric[] values =
          FairAllocationStrategy.FairnessMetric.values();
      assertEquals(4, values.length, "FairnessMetric should have 4 values");
    }

    @Test
    @DisplayName("FairnessMetric should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames =
          Set.of(
              "JAINS_INDEX",
              "COEFFICIENT_OF_VARIATION",
              "MAX_MIN_FAIRNESS",
              "PROPORTIONAL_FAIRNESS");
      Set<String> actualNames = new HashSet<>();
      for (FairAllocationStrategy.FairnessMetric metric :
          FairAllocationStrategy.FairnessMetric.values()) {
        actualNames.add(metric.name());
      }
      assertEquals(expectedNames, actualNames, "FairnessMetric should have expected values");
    }
  }

  // ========================================================================
  // ViolationType Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ViolationType Enum Tests")
  class ViolationTypeTests {

    @Test
    @DisplayName("ViolationType should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          FairAllocationStrategy.ViolationType.class.isEnum(), "ViolationType should be an enum");
    }

    @Test
    @DisplayName("ViolationType should have 4 values")
    void shouldHaveFourValues() {
      FairAllocationStrategy.ViolationType[] values = FairAllocationStrategy.ViolationType.values();
      assertEquals(4, values.length, "ViolationType should have 4 values");
    }

    @Test
    @DisplayName("ViolationType should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames =
          Set.of("UNFAIR_DISTRIBUTION", "STARVATION", "PRIORITY_INVERSION", "RESOURCE_HOGGING");
      Set<String> actualNames = new HashSet<>();
      for (FairAllocationStrategy.ViolationType type :
          FairAllocationStrategy.ViolationType.values()) {
        actualNames.add(type.name());
      }
      assertEquals(expectedNames, actualNames, "ViolationType should have expected values");
    }
  }

  // ========================================================================
  // ViolationSeverity Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ViolationSeverity Enum Tests")
  class ViolationSeverityTests {

    @Test
    @DisplayName("ViolationSeverity should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          FairAllocationStrategy.ViolationSeverity.class.isEnum(),
          "ViolationSeverity should be an enum");
    }

    @Test
    @DisplayName("ViolationSeverity should have 4 values")
    void shouldHaveFourValues() {
      FairAllocationStrategy.ViolationSeverity[] values =
          FairAllocationStrategy.ViolationSeverity.values();
      assertEquals(4, values.length, "ViolationSeverity should have 4 values");
    }

    @Test
    @DisplayName("ViolationSeverity should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
      Set<String> actualNames = new HashSet<>();
      for (FairAllocationStrategy.ViolationSeverity severity :
          FairAllocationStrategy.ViolationSeverity.values()) {
        actualNames.add(severity.name());
      }
      assertEquals(expectedNames, actualNames, "ViolationSeverity should have expected values");
    }
  }

  // ========================================================================
  // Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Interface Tests")
  class NestedInterfaceTests {

    @Test
    @DisplayName("AllocationRequest should be a nested interface")
    void allocationRequestShouldBeNestedInterface() {
      assertTrue(
          FairAllocationStrategy.AllocationRequest.class.isInterface(),
          "AllocationRequest should be an interface");
      assertTrue(
          FairAllocationStrategy.AllocationRequest.class.isMemberClass(),
          "AllocationRequest should be a member class");
    }

    @Test
    @DisplayName("AllocationResult should be a nested interface")
    void allocationResultShouldBeNestedInterface() {
      assertTrue(
          FairAllocationStrategy.AllocationResult.class.isInterface(),
          "AllocationResult should be an interface");
    }

    @Test
    @DisplayName("ResourceRequirements should be a nested interface")
    void resourceRequirementsShouldBeNestedInterface() {
      assertTrue(
          FairAllocationStrategy.ResourceRequirements.class.isInterface(),
          "ResourceRequirements should be an interface");
    }

    @Test
    @DisplayName("AllocatedResources should be a nested interface")
    void allocatedResourcesShouldBeNestedInterface() {
      assertTrue(
          FairAllocationStrategy.AllocatedResources.class.isInterface(),
          "AllocatedResources should be an interface");
    }

    @Test
    @DisplayName("AllocationConstraints should be a nested interface")
    void allocationConstraintsShouldBeNestedInterface() {
      assertTrue(
          FairAllocationStrategy.AllocationConstraints.class.isInterface(),
          "AllocationConstraints should be an interface");
    }

    @Test
    @DisplayName("AllocationHistory should be a nested interface")
    void allocationHistoryShouldBeNestedInterface() {
      assertTrue(
          FairAllocationStrategy.AllocationHistory.class.isInterface(),
          "AllocationHistory should be an interface");
    }

    @Test
    @DisplayName("FairnessEvaluation should be a nested interface")
    void fairnessEvaluationShouldBeNestedInterface() {
      assertTrue(
          FairAllocationStrategy.FairnessEvaluation.class.isInterface(),
          "FairnessEvaluation should be an interface");
    }

    @Test
    @DisplayName("AllocationStatistics should be a nested interface")
    void allocationStatisticsShouldBeNestedInterface() {
      assertTrue(
          FairAllocationStrategy.AllocationStatistics.class.isInterface(),
          "AllocationStatistics should be an interface");
    }

    @Test
    @DisplayName("AllocationTrends should be a nested interface")
    void allocationTrendsShouldBeNestedInterface() {
      assertTrue(
          FairAllocationStrategy.AllocationTrends.class.isInterface(),
          "AllocationTrends should be an interface");
    }

    @Test
    @DisplayName("FairnessViolation should be a nested interface")
    void fairnessViolationShouldBeNestedInterface() {
      assertTrue(
          FairAllocationStrategy.FairnessViolation.class.isInterface(),
          "FairnessViolation should be an interface");
    }

    @Test
    @DisplayName("ResourceUtilization should be a nested interface")
    void resourceUtilizationShouldBeNestedInterface() {
      assertTrue(
          FairAllocationStrategy.ResourceUtilization.class.isInterface(),
          "ResourceUtilization should be an interface");
    }

    @Test
    @DisplayName("TrendData should be a nested interface")
    void trendDataShouldBeNestedInterface() {
      assertTrue(
          FairAllocationStrategy.TrendData.class.isInterface(), "TrendData should be an interface");
    }

    @Test
    @DisplayName("DataPoint should be a nested interface")
    void dataPointShouldBeNestedInterface() {
      assertTrue(
          FairAllocationStrategy.DataPoint.class.isInterface(), "DataPoint should be an interface");
    }
  }

  // ========================================================================
  // Nested Type Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Type Count Tests")
  class NestedTypeCountTests {

    @Test
    @DisplayName("FairAllocationStrategy should have 4 nested enums")
    void shouldHaveFourNestedEnums() {
      Class<?>[] nestedClasses = FairAllocationStrategy.class.getDeclaredClasses();
      int enumCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isEnum()) {
          enumCount++;
        }
      }
      assertEquals(4, enumCount, "FairAllocationStrategy should have 4 nested enums");
    }

    @Test
    @DisplayName("FairAllocationStrategy should have 13 nested interfaces")
    void shouldHaveThirteenNestedInterfaces() {
      Class<?>[] nestedClasses = FairAllocationStrategy.class.getDeclaredClasses();
      int interfaceCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isInterface() && !nested.isAnnotation()) {
          interfaceCount++;
        }
      }
      assertEquals(13, interfaceCount, "FairAllocationStrategy should have 13 nested interfaces");
    }
  }
}
