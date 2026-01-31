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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AnalysisMethod}.
 *
 * <p>Verifies enum structure, constants, requiresExecution, isPerformanceIntensive,
 * providesTimingInfo, isStaticAnalysis, providesMemoryInfo, getOverheadLevel,
 * getDescription, and the inner OverheadLevel enum.
 */
@DisplayName("AnalysisMethod Tests")
class AnalysisMethodTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(AnalysisMethod.class.isEnum(),
          "AnalysisMethod should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 15 values")
    void shouldHaveExactValueCount() {
      assertEquals(15, AnalysisMethod.values().length,
          "AnalysisMethod should have exactly 15 values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should contain all expected constants")
    void shouldContainAllExpectedConstants() {
      assertNotNull(AnalysisMethod.SAMPLING_ANALYSIS,
          "SAMPLING_ANALYSIS should exist");
      assertNotNull(AnalysisMethod.INSTRUMENTATION_ANALYSIS,
          "INSTRUMENTATION_ANALYSIS should exist");
      assertNotNull(AnalysisMethod.CALL_FREQUENCY_ANALYSIS,
          "CALL_FREQUENCY_ANALYSIS should exist");
      assertNotNull(AnalysisMethod.EXECUTION_TIME_ANALYSIS,
          "EXECUTION_TIME_ANALYSIS should exist");
      assertNotNull(AnalysisMethod.MEMORY_USAGE_ANALYSIS,
          "MEMORY_USAGE_ANALYSIS should exist");
      assertNotNull(AnalysisMethod.HOTNESS_ANALYSIS,
          "HOTNESS_ANALYSIS should exist");
      assertNotNull(AnalysisMethod.DEAD_CODE_ANALYSIS,
          "DEAD_CODE_ANALYSIS should exist");
      assertNotNull(AnalysisMethod.CALL_GRAPH_ANALYSIS,
          "CALL_GRAPH_ANALYSIS should exist");
      assertNotNull(AnalysisMethod.CONTROL_FLOW_ANALYSIS,
          "CONTROL_FLOW_ANALYSIS should exist");
      assertNotNull(AnalysisMethod.DATA_FLOW_ANALYSIS,
          "DATA_FLOW_ANALYSIS should exist");
      assertNotNull(AnalysisMethod.LOOP_ANALYSIS,
          "LOOP_ANALYSIS should exist");
      assertNotNull(AnalysisMethod.RECURSION_ANALYSIS,
          "RECURSION_ANALYSIS should exist");
      assertNotNull(AnalysisMethod.CACHE_ANALYSIS,
          "CACHE_ANALYSIS should exist");
      assertNotNull(AnalysisMethod.BRANCH_PREDICTION_ANALYSIS,
          "BRANCH_PREDICTION_ANALYSIS should exist");
      assertNotNull(AnalysisMethod.OPTIMIZATION_ANALYSIS,
          "OPTIMIZATION_ANALYSIS should exist");
    }
  }

  @Nested
  @DisplayName("RequiresExecution Tests")
  class RequiresExecutionTests {

    @Test
    @DisplayName("runtime analysis methods should require execution")
    void runtimeMethodsShouldRequireExecution() {
      assertTrue(AnalysisMethod.SAMPLING_ANALYSIS.requiresExecution(),
          "SAMPLING_ANALYSIS should require execution");
      assertTrue(AnalysisMethod.INSTRUMENTATION_ANALYSIS.requiresExecution(),
          "INSTRUMENTATION_ANALYSIS should require execution");
      assertTrue(AnalysisMethod.CALL_FREQUENCY_ANALYSIS.requiresExecution(),
          "CALL_FREQUENCY_ANALYSIS should require execution");
      assertTrue(AnalysisMethod.EXECUTION_TIME_ANALYSIS.requiresExecution(),
          "EXECUTION_TIME_ANALYSIS should require execution");
      assertTrue(AnalysisMethod.MEMORY_USAGE_ANALYSIS.requiresExecution(),
          "MEMORY_USAGE_ANALYSIS should require execution");
      assertTrue(AnalysisMethod.HOTNESS_ANALYSIS.requiresExecution(),
          "HOTNESS_ANALYSIS should require execution");
      assertTrue(AnalysisMethod.CACHE_ANALYSIS.requiresExecution(),
          "CACHE_ANALYSIS should require execution");
      assertTrue(AnalysisMethod.BRANCH_PREDICTION_ANALYSIS.requiresExecution(),
          "BRANCH_PREDICTION_ANALYSIS should require execution");
    }

    @Test
    @DisplayName("static analysis methods should not require execution")
    void staticMethodsShouldNotRequireExecution() {
      assertFalse(AnalysisMethod.DEAD_CODE_ANALYSIS.requiresExecution(),
          "DEAD_CODE_ANALYSIS should not require execution");
      assertFalse(AnalysisMethod.CALL_GRAPH_ANALYSIS.requiresExecution(),
          "CALL_GRAPH_ANALYSIS should not require execution");
      assertFalse(AnalysisMethod.CONTROL_FLOW_ANALYSIS.requiresExecution(),
          "CONTROL_FLOW_ANALYSIS should not require execution");
      assertFalse(AnalysisMethod.DATA_FLOW_ANALYSIS.requiresExecution(),
          "DATA_FLOW_ANALYSIS should not require execution");
      assertFalse(AnalysisMethod.LOOP_ANALYSIS.requiresExecution(),
          "LOOP_ANALYSIS should not require execution");
      assertFalse(AnalysisMethod.RECURSION_ANALYSIS.requiresExecution(),
          "RECURSION_ANALYSIS should not require execution");
      assertFalse(AnalysisMethod.OPTIMIZATION_ANALYSIS.requiresExecution(),
          "OPTIMIZATION_ANALYSIS should not require execution");
    }
  }

  @Nested
  @DisplayName("IsStaticAnalysis Tests")
  class IsStaticAnalysisTests {

    @Test
    @DisplayName("isStaticAnalysis should be opposite of requiresExecution")
    void shouldBeOppositeOfRequiresExecution() {
      for (final AnalysisMethod method : AnalysisMethod.values()) {
        assertEquals(!method.requiresExecution(), method.isStaticAnalysis(),
            "isStaticAnalysis should be opposite of requiresExecution for "
                + method.name());
      }
    }
  }

  @Nested
  @DisplayName("IsPerformanceIntensive Tests")
  class IsPerformanceIntensiveTests {

    @Test
    @DisplayName("performance intensive methods should return true")
    void performanceIntensiveMethodsShouldReturnTrue() {
      assertTrue(AnalysisMethod.INSTRUMENTATION_ANALYSIS.isPerformanceIntensive(),
          "INSTRUMENTATION_ANALYSIS should be performance intensive");
      assertTrue(AnalysisMethod.MEMORY_USAGE_ANALYSIS.isPerformanceIntensive(),
          "MEMORY_USAGE_ANALYSIS should be performance intensive");
      assertTrue(AnalysisMethod.CACHE_ANALYSIS.isPerformanceIntensive(),
          "CACHE_ANALYSIS should be performance intensive");
    }

    @Test
    @DisplayName("non-performance intensive methods should return false")
    void nonPerformanceIntensiveMethodsShouldReturnFalse() {
      assertFalse(AnalysisMethod.SAMPLING_ANALYSIS.isPerformanceIntensive(),
          "SAMPLING_ANALYSIS should not be performance intensive");
      assertFalse(AnalysisMethod.DEAD_CODE_ANALYSIS.isPerformanceIntensive(),
          "DEAD_CODE_ANALYSIS should not be performance intensive");
      assertFalse(AnalysisMethod.CALL_GRAPH_ANALYSIS.isPerformanceIntensive(),
          "CALL_GRAPH_ANALYSIS should not be performance intensive");
    }
  }

  @Nested
  @DisplayName("ProvidesTimingInfo Tests")
  class ProvidesTimingInfoTests {

    @Test
    @DisplayName("timing methods should provide timing info")
    void timingMethodsShouldProvideTimingInfo() {
      assertTrue(AnalysisMethod.SAMPLING_ANALYSIS.providesTimingInfo(),
          "SAMPLING_ANALYSIS should provide timing info");
      assertTrue(AnalysisMethod.INSTRUMENTATION_ANALYSIS.providesTimingInfo(),
          "INSTRUMENTATION_ANALYSIS should provide timing info");
      assertTrue(AnalysisMethod.EXECUTION_TIME_ANALYSIS.providesTimingInfo(),
          "EXECUTION_TIME_ANALYSIS should provide timing info");
      assertTrue(AnalysisMethod.HOTNESS_ANALYSIS.providesTimingInfo(),
          "HOTNESS_ANALYSIS should provide timing info");
      assertTrue(AnalysisMethod.CACHE_ANALYSIS.providesTimingInfo(),
          "CACHE_ANALYSIS should provide timing info");
    }

    @Test
    @DisplayName("non-timing methods should not provide timing info")
    void nonTimingMethodsShouldNotProvideTimingInfo() {
      assertFalse(AnalysisMethod.DEAD_CODE_ANALYSIS.providesTimingInfo(),
          "DEAD_CODE_ANALYSIS should not provide timing info");
      assertFalse(AnalysisMethod.MEMORY_USAGE_ANALYSIS.providesTimingInfo(),
          "MEMORY_USAGE_ANALYSIS should not provide timing info");
    }
  }

  @Nested
  @DisplayName("ProvidesMemoryInfo Tests")
  class ProvidesMemoryInfoTests {

    @Test
    @DisplayName("memory methods should provide memory info")
    void memoryMethodsShouldProvideMemoryInfo() {
      assertTrue(AnalysisMethod.MEMORY_USAGE_ANALYSIS.providesMemoryInfo(),
          "MEMORY_USAGE_ANALYSIS should provide memory info");
      assertTrue(AnalysisMethod.CACHE_ANALYSIS.providesMemoryInfo(),
          "CACHE_ANALYSIS should provide memory info");
    }

    @Test
    @DisplayName("non-memory methods should not provide memory info")
    void nonMemoryMethodsShouldNotProvideMemoryInfo() {
      assertFalse(AnalysisMethod.SAMPLING_ANALYSIS.providesMemoryInfo(),
          "SAMPLING_ANALYSIS should not provide memory info");
      assertFalse(AnalysisMethod.DEAD_CODE_ANALYSIS.providesMemoryInfo(),
          "DEAD_CODE_ANALYSIS should not provide memory info");
      assertFalse(AnalysisMethod.CALL_GRAPH_ANALYSIS.providesMemoryInfo(),
          "CALL_GRAPH_ANALYSIS should not provide memory info");
    }
  }

  @Nested
  @DisplayName("GetOverheadLevel Tests")
  class GetOverheadLevelTests {

    @Test
    @DisplayName("low overhead methods should return LOW")
    void lowOverheadMethodsShouldReturnLow() {
      assertEquals(AnalysisMethod.OverheadLevel.LOW,
          AnalysisMethod.SAMPLING_ANALYSIS.getOverheadLevel(),
          "SAMPLING_ANALYSIS should have LOW overhead");
      assertEquals(AnalysisMethod.OverheadLevel.LOW,
          AnalysisMethod.DEAD_CODE_ANALYSIS.getOverheadLevel(),
          "DEAD_CODE_ANALYSIS should have LOW overhead");
      assertEquals(AnalysisMethod.OverheadLevel.LOW,
          AnalysisMethod.CALL_GRAPH_ANALYSIS.getOverheadLevel(),
          "CALL_GRAPH_ANALYSIS should have LOW overhead");
      assertEquals(AnalysisMethod.OverheadLevel.LOW,
          AnalysisMethod.OPTIMIZATION_ANALYSIS.getOverheadLevel(),
          "OPTIMIZATION_ANALYSIS should have LOW overhead");
    }

    @Test
    @DisplayName("medium overhead methods should return MEDIUM")
    void mediumOverheadMethodsShouldReturnMedium() {
      assertEquals(AnalysisMethod.OverheadLevel.MEDIUM,
          AnalysisMethod.EXECUTION_TIME_ANALYSIS.getOverheadLevel(),
          "EXECUTION_TIME_ANALYSIS should have MEDIUM overhead");
      assertEquals(AnalysisMethod.OverheadLevel.MEDIUM,
          AnalysisMethod.HOTNESS_ANALYSIS.getOverheadLevel(),
          "HOTNESS_ANALYSIS should have MEDIUM overhead");
      assertEquals(AnalysisMethod.OverheadLevel.MEDIUM,
          AnalysisMethod.BRANCH_PREDICTION_ANALYSIS.getOverheadLevel(),
          "BRANCH_PREDICTION_ANALYSIS should have MEDIUM overhead");
    }

    @Test
    @DisplayName("high overhead methods should return HIGH")
    void highOverheadMethodsShouldReturnHigh() {
      assertEquals(AnalysisMethod.OverheadLevel.HIGH,
          AnalysisMethod.INSTRUMENTATION_ANALYSIS.getOverheadLevel(),
          "INSTRUMENTATION_ANALYSIS should have HIGH overhead");
      assertEquals(AnalysisMethod.OverheadLevel.HIGH,
          AnalysisMethod.MEMORY_USAGE_ANALYSIS.getOverheadLevel(),
          "MEMORY_USAGE_ANALYSIS should have HIGH overhead");
      assertEquals(AnalysisMethod.OverheadLevel.HIGH,
          AnalysisMethod.CACHE_ANALYSIS.getOverheadLevel(),
          "CACHE_ANALYSIS should have HIGH overhead");
    }
  }

  @Nested
  @DisplayName("GetDescription Tests")
  class GetDescriptionTests {

    @Test
    @DisplayName("every constant should have a non-null description")
    void everyConstantShouldHaveDescription() {
      for (final AnalysisMethod method : AnalysisMethod.values()) {
        assertNotNull(method.getDescription(),
            method.name() + " should have a non-null description");
        assertFalse(method.getDescription().isEmpty(),
            method.name() + " should have a non-empty description");
      }
    }
  }

  @Nested
  @DisplayName("OverheadLevel Inner Enum Tests")
  class OverheadLevelInnerEnumTests {

    @Test
    @DisplayName("OverheadLevel should be an enum with 3 values")
    void shouldBeEnumWithThreeValues() {
      assertTrue(AnalysisMethod.OverheadLevel.class.isEnum(),
          "OverheadLevel should be an enum type");
      assertEquals(3, AnalysisMethod.OverheadLevel.values().length,
          "OverheadLevel should have exactly 3 values");
    }

    @Test
    @DisplayName("should contain LOW, MEDIUM, HIGH")
    void shouldContainAllLevels() {
      assertNotNull(AnalysisMethod.OverheadLevel.LOW,
          "LOW should exist");
      assertNotNull(AnalysisMethod.OverheadLevel.MEDIUM,
          "MEDIUM should exist");
      assertNotNull(AnalysisMethod.OverheadLevel.HIGH,
          "HIGH should exist");
    }

    @Test
    @DisplayName("should resolve via valueOf")
    void shouldResolveViaValueOf() {
      assertEquals(AnalysisMethod.OverheadLevel.LOW,
          AnalysisMethod.OverheadLevel.valueOf("LOW"),
          "valueOf should return LOW");
      assertEquals(AnalysisMethod.OverheadLevel.MEDIUM,
          AnalysisMethod.OverheadLevel.valueOf("MEDIUM"),
          "valueOf should return MEDIUM");
      assertEquals(AnalysisMethod.OverheadLevel.HIGH,
          AnalysisMethod.OverheadLevel.valueOf("HIGH"),
          "valueOf should return HIGH");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("should have unique ordinals")
    void shouldHaveUniqueOrdinals() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final AnalysisMethod value : AnalysisMethod.values()) {
        ordinals.add(value.ordinal());
      }
      assertEquals(AnalysisMethod.values().length, ordinals.size(),
          "All ordinals should be unique");
    }

    @Test
    @DisplayName("should have sequential ordinals starting from 0")
    void shouldHaveSequentialOrdinals() {
      final AnalysisMethod[] values = AnalysisMethod.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal(),
            "Ordinal of " + values[i].name() + " should be " + i);
      }
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("should resolve all constants via valueOf")
    void shouldResolveAllConstantsViaValueOf() {
      for (final AnalysisMethod value : AnalysisMethod.values()) {
        assertEquals(value, AnalysisMethod.valueOf(value.name()),
            "valueOf should return " + value.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowForInvalidName() {
      assertThrows(IllegalArgumentException.class,
          () -> AnalysisMethod.valueOf("INVALID_CONSTANT"),
          "valueOf with invalid name should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("should return new array on each call")
    void shouldReturnNewArrayOnEachCall() {
      final AnalysisMethod[] first = AnalysisMethod.values();
      final AnalysisMethod[] second = AnalysisMethod.values();
      assertTrue(first != second, "values() should return a new array on each call");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("every analysis method should have an overhead level")
    void everyMethodShouldHaveOverheadLevel() {
      for (final AnalysisMethod method : AnalysisMethod.values()) {
        assertNotNull(method.getOverheadLevel(),
            method.name() + " should have a non-null overhead level");
      }
    }

    @Test
    @DisplayName("performance intensive methods should have HIGH overhead")
    void performanceIntensiveShouldHaveHighOverhead() {
      for (final AnalysisMethod method : AnalysisMethod.values()) {
        if (method.isPerformanceIntensive()) {
          assertEquals(AnalysisMethod.OverheadLevel.HIGH, method.getOverheadLevel(),
              method.name()
                  + " is performance intensive and should have HIGH overhead");
        }
      }
    }
  }
}
