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
 * Tests for the AnomalyDetectionConfig interface.
 *
 * <p>This test class verifies the interface structure, methods, and nested types for
 * AnomalyDetectionConfig using reflection-based testing.
 */
@DisplayName("AnomalyDetectionConfig Tests")
class AnomalyDetectionConfigTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("AnomalyDetectionConfig should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          AnomalyDetectionConfig.class.isInterface(),
          "AnomalyDetectionConfig should be an interface");
    }

    @Test
    @DisplayName("AnomalyDetectionConfig should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(AnomalyDetectionConfig.class.getModifiers()),
          "AnomalyDetectionConfig should be public");
    }

    @Test
    @DisplayName("AnomalyDetectionConfig should not extend other interfaces")
    void shouldNotExtendOtherInterfaces() {
      Class<?>[] interfaces = AnomalyDetectionConfig.class.getInterfaces();
      assertEquals(
          0, interfaces.length, "AnomalyDetectionConfig should not extend other interfaces");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have isEnabled method")
    void shouldHaveIsEnabledMethod() throws NoSuchMethodException {
      Method method = AnomalyDetectionConfig.class.getMethod("isEnabled");
      assertNotNull(method, "isEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have setEnabled method")
    void shouldHaveSetEnabledMethod() throws NoSuchMethodException {
      Method method = AnomalyDetectionConfig.class.getMethod("setEnabled", boolean.class);
      assertNotNull(method, "setEnabled method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getAlgorithm method")
    void shouldHaveGetAlgorithmMethod() throws NoSuchMethodException {
      Method method = AnomalyDetectionConfig.class.getMethod("getAlgorithm");
      assertNotNull(method, "getAlgorithm method should exist");
      assertEquals(
          AnomalyDetectionConfig.DetectionAlgorithm.class,
          method.getReturnType(),
          "Return type should be DetectionAlgorithm");
    }

    @Test
    @DisplayName("should have setAlgorithm method")
    void shouldHaveSetAlgorithmMethod() throws NoSuchMethodException {
      Method method =
          AnomalyDetectionConfig.class.getMethod(
              "setAlgorithm", AnomalyDetectionConfig.DetectionAlgorithm.class);
      assertNotNull(method, "setAlgorithm method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getSensitivity method")
    void shouldHaveGetSensitivityMethod() throws NoSuchMethodException {
      Method method = AnomalyDetectionConfig.class.getMethod("getSensitivity");
      assertNotNull(method, "getSensitivity method should exist");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }

    @Test
    @DisplayName("should have setSensitivity method")
    void shouldHaveSetSensitivityMethod() throws NoSuchMethodException {
      Method method = AnomalyDetectionConfig.class.getMethod("setSensitivity", double.class);
      assertNotNull(method, "setSensitivity method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getMonitoredMetrics method")
    void shouldHaveGetMonitoredMetricsMethod() throws NoSuchMethodException {
      Method method = AnomalyDetectionConfig.class.getMethod("getMonitoredMetrics");
      assertNotNull(method, "getMonitoredMetrics method should exist");
      assertEquals(Set.class, method.getReturnType(), "Return type should be Set");
    }

    @Test
    @DisplayName("should have getThresholds method")
    void shouldHaveGetThresholdsMethod() throws NoSuchMethodException {
      Method method = AnomalyDetectionConfig.class.getMethod("getThresholds");
      assertNotNull(method, "getThresholds method should exist");
      assertEquals(
          AnomalyDetectionConfig.DetectionThresholds.class,
          method.getReturnType(),
          "Return type should be DetectionThresholds");
    }

    @Test
    @DisplayName("should have getWindowSize method")
    void shouldHaveGetWindowSizeMethod() throws NoSuchMethodException {
      Method method = AnomalyDetectionConfig.class.getMethod("getWindowSize");
      assertNotNull(method, "getWindowSize method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have getHandlers method")
    void shouldHaveGetHandlersMethod() throws NoSuchMethodException {
      Method method = AnomalyDetectionConfig.class.getMethod("getHandlers");
      assertNotNull(method, "getHandlers method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have addHandler method")
    void shouldHaveAddHandlerMethod() throws NoSuchMethodException {
      Method method =
          AnomalyDetectionConfig.class.getMethod(
              "addHandler", AnomalyDetectionConfig.AnomalyHandler.class);
      assertNotNull(method, "addHandler method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getBaselineLearning method")
    void shouldHaveGetBaselineLearningMethod() throws NoSuchMethodException {
      Method method = AnomalyDetectionConfig.class.getMethod("getBaselineLearning");
      assertNotNull(method, "getBaselineLearning method should exist");
      assertEquals(
          AnomalyDetectionConfig.BaselineLearningConfig.class,
          method.getReturnType(),
          "Return type should be BaselineLearningConfig");
    }
  }

  // ========================================================================
  // DetectionAlgorithm Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("DetectionAlgorithm Enum Tests")
  class DetectionAlgorithmTests {

    @Test
    @DisplayName("DetectionAlgorithm should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          AnomalyDetectionConfig.DetectionAlgorithm.class.isEnum(),
          "DetectionAlgorithm should be an enum");
      assertTrue(
          AnomalyDetectionConfig.DetectionAlgorithm.class.isMemberClass(),
          "DetectionAlgorithm should be a member class");
    }

    @Test
    @DisplayName("DetectionAlgorithm should have 7 values")
    void shouldHaveSevenValues() {
      AnomalyDetectionConfig.DetectionAlgorithm[] values =
          AnomalyDetectionConfig.DetectionAlgorithm.values();
      assertEquals(7, values.length, "DetectionAlgorithm should have 7 values");
    }

    @Test
    @DisplayName("DetectionAlgorithm should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames =
          Set.of(
              "STATISTICAL_THRESHOLD",
              "MOVING_AVERAGE",
              "EXPONENTIAL_SMOOTHING",
              "ISOLATION_FOREST",
              "ONE_CLASS_SVM",
              "LOCAL_OUTLIER_FACTOR",
              "NEURAL_NETWORK");
      Set<String> actualNames = new HashSet<>();
      for (AnomalyDetectionConfig.DetectionAlgorithm algo :
          AnomalyDetectionConfig.DetectionAlgorithm.values()) {
        actualNames.add(algo.name());
      }
      assertEquals(expectedNames, actualNames, "DetectionAlgorithm should have expected values");
    }
  }

  // ========================================================================
  // AnomalyMetric Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("AnomalyMetric Enum Tests")
  class AnomalyMetricTests {

    @Test
    @DisplayName("AnomalyMetric should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          AnomalyDetectionConfig.AnomalyMetric.class.isEnum(), "AnomalyMetric should be an enum");
      assertTrue(
          AnomalyDetectionConfig.AnomalyMetric.class.isMemberClass(),
          "AnomalyMetric should be a member class");
    }

    @Test
    @DisplayName("AnomalyMetric should have 8 values")
    void shouldHaveEightValues() {
      AnomalyDetectionConfig.AnomalyMetric[] values = AnomalyDetectionConfig.AnomalyMetric.values();
      assertEquals(8, values.length, "AnomalyMetric should have 8 values");
    }

    @Test
    @DisplayName("AnomalyMetric should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames =
          Set.of(
              "EXECUTION_TIME",
              "MEMORY_USAGE",
              "CPU_USAGE",
              "ERROR_RATE",
              "THROUGHPUT",
              "FUEL_CONSUMPTION",
              "FUNCTION_CALL_FREQUENCY",
              "CUSTOM");
      Set<String> actualNames = new HashSet<>();
      for (AnomalyDetectionConfig.AnomalyMetric metric :
          AnomalyDetectionConfig.AnomalyMetric.values()) {
        actualNames.add(metric.name());
      }
      assertEquals(expectedNames, actualNames, "AnomalyMetric should have expected values");
    }
  }

  // ========================================================================
  // ThresholdType Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ThresholdType Enum Tests")
  class ThresholdTypeTests {

    @Test
    @DisplayName("ThresholdType should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          AnomalyDetectionConfig.ThresholdType.class.isEnum(), "ThresholdType should be an enum");
    }

    @Test
    @DisplayName("ThresholdType should have 4 values")
    void shouldHaveFourValues() {
      AnomalyDetectionConfig.ThresholdType[] values = AnomalyDetectionConfig.ThresholdType.values();
      assertEquals(4, values.length, "ThresholdType should have 4 values");
    }

    @Test
    @DisplayName("ThresholdType should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames =
          Set.of("ABSOLUTE", "PERCENTAGE", "STANDARD_DEVIATION", "PERCENTILE");
      Set<String> actualNames = new HashSet<>();
      for (AnomalyDetectionConfig.ThresholdType type :
          AnomalyDetectionConfig.ThresholdType.values()) {
        actualNames.add(type.name());
      }
      assertEquals(expectedNames, actualNames, "ThresholdType should have expected values");
    }
  }

  // ========================================================================
  // AnomalyType Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("AnomalyType Enum Tests")
  class AnomalyTypeTests {

    @Test
    @DisplayName("AnomalyType should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          AnomalyDetectionConfig.AnomalyType.class.isEnum(), "AnomalyType should be an enum");
    }

    @Test
    @DisplayName("AnomalyType should have 4 values")
    void shouldHaveFourValues() {
      AnomalyDetectionConfig.AnomalyType[] values = AnomalyDetectionConfig.AnomalyType.values();
      assertEquals(4, values.length, "AnomalyType should have 4 values");
    }

    @Test
    @DisplayName("AnomalyType should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames = Set.of("POINT", "CONTEXTUAL", "COLLECTIVE", "DRIFT");
      Set<String> actualNames = new HashSet<>();
      for (AnomalyDetectionConfig.AnomalyType type : AnomalyDetectionConfig.AnomalyType.values()) {
        actualNames.add(type.name());
      }
      assertEquals(expectedNames, actualNames, "AnomalyType should have expected values");
    }
  }

  // ========================================================================
  // SeverityLevel Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("SeverityLevel Enum Tests")
  class SeverityLevelTests {

    @Test
    @DisplayName("SeverityLevel should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          AnomalyDetectionConfig.SeverityLevel.class.isEnum(), "SeverityLevel should be an enum");
    }

    @Test
    @DisplayName("SeverityLevel should have 4 values")
    void shouldHaveFourValues() {
      AnomalyDetectionConfig.SeverityLevel[] values = AnomalyDetectionConfig.SeverityLevel.values();
      assertEquals(4, values.length, "SeverityLevel should have 4 values");
    }

    @Test
    @DisplayName("SeverityLevel should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
      Set<String> actualNames = new HashSet<>();
      for (AnomalyDetectionConfig.SeverityLevel level :
          AnomalyDetectionConfig.SeverityLevel.values()) {
        actualNames.add(level.name());
      }
      assertEquals(expectedNames, actualNames, "SeverityLevel should have expected values");
    }
  }

  // ========================================================================
  // LearningAlgorithm Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("LearningAlgorithm Enum Tests")
  class LearningAlgorithmTests {

    @Test
    @DisplayName("LearningAlgorithm should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          AnomalyDetectionConfig.LearningAlgorithm.class.isEnum(),
          "LearningAlgorithm should be an enum");
    }

    @Test
    @DisplayName("LearningAlgorithm should have 4 values")
    void shouldHaveFourValues() {
      AnomalyDetectionConfig.LearningAlgorithm[] values =
          AnomalyDetectionConfig.LearningAlgorithm.values();
      assertEquals(4, values.length, "LearningAlgorithm should have 4 values");
    }

    @Test
    @DisplayName("LearningAlgorithm should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames = Set.of("ONLINE", "BATCH", "INCREMENTAL", "ADAPTIVE");
      Set<String> actualNames = new HashSet<>();
      for (AnomalyDetectionConfig.LearningAlgorithm algo :
          AnomalyDetectionConfig.LearningAlgorithm.values()) {
        actualNames.add(algo.name());
      }
      assertEquals(expectedNames, actualNames, "LearningAlgorithm should have expected values");
    }
  }

  // ========================================================================
  // BaselineUpdateStrategy Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("BaselineUpdateStrategy Enum Tests")
  class BaselineUpdateStrategyTests {

    @Test
    @DisplayName("BaselineUpdateStrategy should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          AnomalyDetectionConfig.BaselineUpdateStrategy.class.isEnum(),
          "BaselineUpdateStrategy should be an enum");
    }

    @Test
    @DisplayName("BaselineUpdateStrategy should have 4 values")
    void shouldHaveFourValues() {
      AnomalyDetectionConfig.BaselineUpdateStrategy[] values =
          AnomalyDetectionConfig.BaselineUpdateStrategy.values();
      assertEquals(4, values.length, "BaselineUpdateStrategy should have 4 values");
    }
  }

  // ========================================================================
  // Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("DetectionThresholds Interface Tests")
  class DetectionThresholdsTests {

    @Test
    @DisplayName("DetectionThresholds should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          AnomalyDetectionConfig.DetectionThresholds.class.isInterface(),
          "DetectionThresholds should be an interface");
      assertTrue(
          AnomalyDetectionConfig.DetectionThresholds.class.isMemberClass(),
          "DetectionThresholds should be a member class");
    }

    @Test
    @DisplayName("DetectionThresholds should have 6 methods")
    void shouldHaveSixMethods() {
      Method[] methods = AnomalyDetectionConfig.DetectionThresholds.class.getDeclaredMethods();
      assertEquals(6, methods.length, "DetectionThresholds should have 6 methods");
    }
  }

  @Nested
  @DisplayName("ThresholdConfig Interface Tests")
  class ThresholdConfigTests {

    @Test
    @DisplayName("ThresholdConfig should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          AnomalyDetectionConfig.ThresholdConfig.class.isInterface(),
          "ThresholdConfig should be an interface");
    }

    @Test
    @DisplayName("ThresholdConfig should have 4 methods")
    void shouldHaveFourMethods() {
      Method[] methods = AnomalyDetectionConfig.ThresholdConfig.class.getDeclaredMethods();
      assertEquals(4, methods.length, "ThresholdConfig should have 4 methods");
    }
  }

  @Nested
  @DisplayName("AnomalyHandler Interface Tests")
  class AnomalyHandlerTests {

    @Test
    @DisplayName("AnomalyHandler should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          AnomalyDetectionConfig.AnomalyHandler.class.isInterface(),
          "AnomalyHandler should be an interface");
    }

    @Test
    @DisplayName("AnomalyHandler should have 4 methods")
    void shouldHaveFourMethods() {
      Method[] methods = AnomalyDetectionConfig.AnomalyHandler.class.getDeclaredMethods();
      assertEquals(4, methods.length, "AnomalyHandler should have 4 methods");
    }
  }

  @Nested
  @DisplayName("AnomalyEvent Interface Tests")
  class AnomalyEventTests {

    @Test
    @DisplayName("AnomalyEvent should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          AnomalyDetectionConfig.AnomalyEvent.class.isInterface(),
          "AnomalyEvent should be an interface");
    }

    @Test
    @DisplayName("AnomalyEvent should have 8 methods")
    void shouldHaveEightMethods() {
      Method[] methods = AnomalyDetectionConfig.AnomalyEvent.class.getDeclaredMethods();
      assertEquals(8, methods.length, "AnomalyEvent should have 8 methods");
    }
  }

  @Nested
  @DisplayName("BaselineLearningConfig Interface Tests")
  class BaselineLearningConfigTests {

    @Test
    @DisplayName("BaselineLearningConfig should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          AnomalyDetectionConfig.BaselineLearningConfig.class.isInterface(),
          "BaselineLearningConfig should be an interface");
    }

    @Test
    @DisplayName("BaselineLearningConfig should have 6 methods")
    void shouldHaveSixMethods() {
      Method[] methods = AnomalyDetectionConfig.BaselineLearningConfig.class.getDeclaredMethods();
      assertEquals(6, methods.length, "BaselineLearningConfig should have 6 methods");
    }
  }

  // ========================================================================
  // Nested Type Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Type Count Tests")
  class NestedTypeCountTests {

    @Test
    @DisplayName("AnomalyDetectionConfig should have 7 nested enums")
    void shouldHaveSevenNestedEnums() {
      Class<?>[] nestedClasses = AnomalyDetectionConfig.class.getDeclaredClasses();
      int enumCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isEnum()) {
          enumCount++;
        }
      }
      assertEquals(7, enumCount, "AnomalyDetectionConfig should have 7 nested enums");
    }

    @Test
    @DisplayName("AnomalyDetectionConfig should have 5 nested interfaces")
    void shouldHaveFiveNestedInterfaces() {
      Class<?>[] nestedClasses = AnomalyDetectionConfig.class.getDeclaredClasses();
      int interfaceCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isInterface() && !nested.isAnnotation()) {
          interfaceCount++;
        }
      }
      assertEquals(5, interfaceCount, "AnomalyDetectionConfig should have 5 nested interfaces");
    }
  }
}
