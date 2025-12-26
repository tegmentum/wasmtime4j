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
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the LoadBasedQuotaConfig interface.
 *
 * <p>This test class verifies the interface structure, methods, nested types, and enums for
 * LoadBasedQuotaConfig using reflection-based testing.
 */
@DisplayName("LoadBasedQuotaConfig Tests")
class LoadBasedQuotaConfigTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("LoadBasedQuotaConfig should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          LoadBasedQuotaConfig.class.isInterface(), "LoadBasedQuotaConfig should be an interface");
    }

    @Test
    @DisplayName("LoadBasedQuotaConfig should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(LoadBasedQuotaConfig.class.getModifiers()),
          "LoadBasedQuotaConfig should be public");
    }

    @Test
    @DisplayName("LoadBasedQuotaConfig should not extend other interfaces")
    void shouldNotExtendOtherInterfaces() {
      Class<?>[] interfaces = LoadBasedQuotaConfig.class.getInterfaces();
      assertEquals(0, interfaces.length, "LoadBasedQuotaConfig should not extend other interfaces");
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
      Method method = LoadBasedQuotaConfig.class.getMethod("isEnabled");
      assertNotNull(method, "isEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have setEnabled method")
    void shouldHaveSetEnabledMethod() throws NoSuchMethodException {
      Method method = LoadBasedQuotaConfig.class.getMethod("setEnabled", boolean.class);
      assertNotNull(method, "setEnabled method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getMonitoringStrategy method")
    void shouldHaveGetMonitoringStrategyMethod() throws NoSuchMethodException {
      Method method = LoadBasedQuotaConfig.class.getMethod("getMonitoringStrategy");
      assertNotNull(method, "getMonitoringStrategy method should exist");
      assertEquals(
          LoadBasedQuotaConfig.LoadMonitoringStrategy.class,
          method.getReturnType(),
          "Return type should be LoadMonitoringStrategy");
    }

    @Test
    @DisplayName("should have setMonitoringStrategy method")
    void shouldHaveSetMonitoringStrategyMethod() throws NoSuchMethodException {
      Method method =
          LoadBasedQuotaConfig.class.getMethod(
              "setMonitoringStrategy", LoadBasedQuotaConfig.LoadMonitoringStrategy.class);
      assertNotNull(method, "setMonitoringStrategy method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getAdjustmentStrategy method")
    void shouldHaveGetAdjustmentStrategyMethod() throws NoSuchMethodException {
      Method method = LoadBasedQuotaConfig.class.getMethod("getAdjustmentStrategy");
      assertNotNull(method, "getAdjustmentStrategy method should exist");
      assertEquals(
          LoadBasedQuotaConfig.QuotaAdjustmentStrategy.class,
          method.getReturnType(),
          "Return type should be QuotaAdjustmentStrategy");
    }

    @Test
    @DisplayName("should have setAdjustmentStrategy method")
    void shouldHaveSetAdjustmentStrategyMethod() throws NoSuchMethodException {
      Method method =
          LoadBasedQuotaConfig.class.getMethod(
              "setAdjustmentStrategy", LoadBasedQuotaConfig.QuotaAdjustmentStrategy.class);
      assertNotNull(method, "setAdjustmentStrategy method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getThresholds method")
    void shouldHaveGetThresholdsMethod() throws NoSuchMethodException {
      Method method = LoadBasedQuotaConfig.class.getMethod("getThresholds");
      assertNotNull(method, "getThresholds method should exist");
      assertEquals(
          LoadBasedQuotaConfig.LoadThresholds.class,
          method.getReturnType(),
          "Return type should be LoadThresholds");
    }

    @Test
    @DisplayName("should have setThresholds method")
    void shouldHaveSetThresholdsMethod() throws NoSuchMethodException {
      Method method =
          LoadBasedQuotaConfig.class.getMethod(
              "setThresholds", LoadBasedQuotaConfig.LoadThresholds.class);
      assertNotNull(method, "setThresholds method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getBounds method")
    void shouldHaveGetBoundsMethod() throws NoSuchMethodException {
      Method method = LoadBasedQuotaConfig.class.getMethod("getBounds");
      assertNotNull(method, "getBounds method should exist");
      assertEquals(
          LoadBasedQuotaConfig.QuotaBounds.class,
          method.getReturnType(),
          "Return type should be QuotaBounds");
    }

    @Test
    @DisplayName("should have setBounds method")
    void shouldHaveSetBoundsMethod() throws NoSuchMethodException {
      Method method =
          LoadBasedQuotaConfig.class.getMethod("setBounds", LoadBasedQuotaConfig.QuotaBounds.class);
      assertNotNull(method, "setBounds method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getMonitoringInterval method")
    void shouldHaveGetMonitoringIntervalMethod() throws NoSuchMethodException {
      Method method = LoadBasedQuotaConfig.class.getMethod("getMonitoringInterval");
      assertNotNull(method, "getMonitoringInterval method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have setMonitoringInterval method")
    void shouldHaveSetMonitoringIntervalMethod() throws NoSuchMethodException {
      Method method = LoadBasedQuotaConfig.class.getMethod("setMonitoringInterval", long.class);
      assertNotNull(method, "setMonitoringInterval method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getSmoothingFactor method")
    void shouldHaveGetSmoothingFactorMethod() throws NoSuchMethodException {
      Method method = LoadBasedQuotaConfig.class.getMethod("getSmoothingFactor");
      assertNotNull(method, "getSmoothingFactor method should exist");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }

    @Test
    @DisplayName("should have setSmoothingFactor method")
    void shouldHaveSetSmoothingFactorMethod() throws NoSuchMethodException {
      Method method = LoadBasedQuotaConfig.class.getMethod("setSmoothingFactor", double.class);
      assertNotNull(method, "setSmoothingFactor method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getMetricsConfig method")
    void shouldHaveGetMetricsConfigMethod() throws NoSuchMethodException {
      Method method = LoadBasedQuotaConfig.class.getMethod("getMetricsConfig");
      assertNotNull(method, "getMetricsConfig method should exist");
      assertEquals(
          LoadBasedQuotaConfig.LoadMetricsConfig.class,
          method.getReturnType(),
          "Return type should be LoadMetricsConfig");
    }

    @Test
    @DisplayName("should have setMetricsConfig method")
    void shouldHaveSetMetricsConfigMethod() throws NoSuchMethodException {
      Method method =
          LoadBasedQuotaConfig.class.getMethod(
              "setMetricsConfig", LoadBasedQuotaConfig.LoadMetricsConfig.class);
      assertNotNull(method, "setMetricsConfig method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }
  }

  // ========================================================================
  // LoadMonitoringStrategy Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("LoadMonitoringStrategy Enum Tests")
  class LoadMonitoringStrategyTests {

    @Test
    @DisplayName("LoadMonitoringStrategy should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          LoadBasedQuotaConfig.LoadMonitoringStrategy.class.isEnum(),
          "LoadMonitoringStrategy should be an enum");
      assertTrue(
          LoadBasedQuotaConfig.LoadMonitoringStrategy.class.isMemberClass(),
          "LoadMonitoringStrategy should be a member class");
    }

    @Test
    @DisplayName("LoadMonitoringStrategy should have 6 values")
    void shouldHaveSixValues() {
      LoadBasedQuotaConfig.LoadMonitoringStrategy[] values =
          LoadBasedQuotaConfig.LoadMonitoringStrategy.values();
      assertEquals(6, values.length, "LoadMonitoringStrategy should have 6 values");
    }

    @Test
    @DisplayName("LoadMonitoringStrategy should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames =
          Set.of(
              "CPU_BASED",
              "MEMORY_BASED",
              "THROUGHPUT_BASED",
              "LATENCY_BASED",
              "COMBINED",
              "CUSTOM");
      Set<String> actualNames = new HashSet<>();
      for (LoadBasedQuotaConfig.LoadMonitoringStrategy strategy :
          LoadBasedQuotaConfig.LoadMonitoringStrategy.values()) {
        actualNames.add(strategy.name());
      }
      assertEquals(
          expectedNames, actualNames, "LoadMonitoringStrategy should have expected values");
    }
  }

  // ========================================================================
  // QuotaAdjustmentStrategy Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("QuotaAdjustmentStrategy Enum Tests")
  class QuotaAdjustmentStrategyTests {

    @Test
    @DisplayName("QuotaAdjustmentStrategy should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          LoadBasedQuotaConfig.QuotaAdjustmentStrategy.class.isEnum(),
          "QuotaAdjustmentStrategy should be an enum");
    }

    @Test
    @DisplayName("QuotaAdjustmentStrategy should have 5 values")
    void shouldHaveFiveValues() {
      LoadBasedQuotaConfig.QuotaAdjustmentStrategy[] values =
          LoadBasedQuotaConfig.QuotaAdjustmentStrategy.values();
      assertEquals(5, values.length, "QuotaAdjustmentStrategy should have 5 values");
    }

    @Test
    @DisplayName("QuotaAdjustmentStrategy should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames =
          Set.of("LINEAR", "EXPONENTIAL", "STEP", "PROPORTIONAL", "PID_CONTROLLER");
      Set<String> actualNames = new HashSet<>();
      for (LoadBasedQuotaConfig.QuotaAdjustmentStrategy strategy :
          LoadBasedQuotaConfig.QuotaAdjustmentStrategy.values()) {
        actualNames.add(strategy.name());
      }
      assertEquals(
          expectedNames, actualNames, "QuotaAdjustmentStrategy should have expected values");
    }
  }

  // ========================================================================
  // LoadMetric Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("LoadMetric Enum Tests")
  class LoadMetricTests {

    @Test
    @DisplayName("LoadMetric should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(LoadBasedQuotaConfig.LoadMetric.class.isEnum(), "LoadMetric should be an enum");
    }

    @Test
    @DisplayName("LoadMetric should have 7 values")
    void shouldHaveSevenValues() {
      LoadBasedQuotaConfig.LoadMetric[] values = LoadBasedQuotaConfig.LoadMetric.values();
      assertEquals(7, values.length, "LoadMetric should have 7 values");
    }

    @Test
    @DisplayName("LoadMetric should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames =
          Set.of(
              "CPU_UTILIZATION",
              "MEMORY_UTILIZATION",
              "REQUEST_THROUGHPUT",
              "AVERAGE_RESPONSE_TIME",
              "ERROR_RATE",
              "QUEUE_DEPTH",
              "ACTIVE_CONNECTIONS");
      Set<String> actualNames = new HashSet<>();
      for (LoadBasedQuotaConfig.LoadMetric metric : LoadBasedQuotaConfig.LoadMetric.values()) {
        actualNames.add(metric.name());
      }
      assertEquals(expectedNames, actualNames, "LoadMetric should have expected values");
    }
  }

  // ========================================================================
  // OutlierDetectionMethod Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("OutlierDetectionMethod Enum Tests")
  class OutlierDetectionMethodTests {

    @Test
    @DisplayName("OutlierDetectionMethod should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          LoadBasedQuotaConfig.OutlierDetectionMethod.class.isEnum(),
          "OutlierDetectionMethod should be an enum");
    }

    @Test
    @DisplayName("OutlierDetectionMethod should have 5 values")
    void shouldHaveFiveValues() {
      LoadBasedQuotaConfig.OutlierDetectionMethod[] values =
          LoadBasedQuotaConfig.OutlierDetectionMethod.values();
      assertEquals(5, values.length, "OutlierDetectionMethod should have 5 values");
    }

    @Test
    @DisplayName("OutlierDetectionMethod should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames =
          Set.of(
              "STANDARD_DEVIATION",
              "INTERQUARTILE_RANGE",
              "Z_SCORE",
              "MODIFIED_Z_SCORE",
              "ISOLATION_FOREST");
      Set<String> actualNames = new HashSet<>();
      for (LoadBasedQuotaConfig.OutlierDetectionMethod method :
          LoadBasedQuotaConfig.OutlierDetectionMethod.values()) {
        actualNames.add(method.name());
      }
      assertEquals(
          expectedNames, actualNames, "OutlierDetectionMethod should have expected values");
    }
  }

  // ========================================================================
  // OutlierHandlingStrategy Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("OutlierHandlingStrategy Enum Tests")
  class OutlierHandlingStrategyTests {

    @Test
    @DisplayName("OutlierHandlingStrategy should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          LoadBasedQuotaConfig.OutlierHandlingStrategy.class.isEnum(),
          "OutlierHandlingStrategy should be an enum");
    }

    @Test
    @DisplayName("OutlierHandlingStrategy should have 5 values")
    void shouldHaveFiveValues() {
      LoadBasedQuotaConfig.OutlierHandlingStrategy[] values =
          LoadBasedQuotaConfig.OutlierHandlingStrategy.values();
      assertEquals(5, values.length, "OutlierHandlingStrategy should have 5 values");
    }

    @Test
    @DisplayName("OutlierHandlingStrategy should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames =
          Set.of(
              "IGNORE",
              "REPLACE_WITH_MEDIAN",
              "REPLACE_WITH_MOVING_AVERAGE",
              "CAP_OUTLIERS",
              "LOG_AND_CONTINUE");
      Set<String> actualNames = new HashSet<>();
      for (LoadBasedQuotaConfig.OutlierHandlingStrategy strategy :
          LoadBasedQuotaConfig.OutlierHandlingStrategy.values()) {
        actualNames.add(strategy.name());
      }
      assertEquals(
          expectedNames, actualNames, "OutlierHandlingStrategy should have expected values");
    }
  }

  // ========================================================================
  // Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Interface Tests")
  class NestedInterfaceTests {

    @Test
    @DisplayName("LoadThresholds should be a nested interface")
    void loadThresholdsShouldBeNestedInterface() {
      assertTrue(
          LoadBasedQuotaConfig.LoadThresholds.class.isInterface(),
          "LoadThresholds should be an interface");
      assertTrue(
          LoadBasedQuotaConfig.LoadThresholds.class.isMemberClass(),
          "LoadThresholds should be a member class");
    }

    @Test
    @DisplayName("QuotaBounds should be a nested interface")
    void quotaBoundsShouldBeNestedInterface() {
      assertTrue(
          LoadBasedQuotaConfig.QuotaBounds.class.isInterface(),
          "QuotaBounds should be an interface");
    }

    @Test
    @DisplayName("LoadMetricsConfig should be a nested interface")
    void loadMetricsConfigShouldBeNestedInterface() {
      assertTrue(
          LoadBasedQuotaConfig.LoadMetricsConfig.class.isInterface(),
          "LoadMetricsConfig should be an interface");
    }

    @Test
    @DisplayName("OutlierDetectionConfig should be a nested interface")
    void outlierDetectionConfigShouldBeNestedInterface() {
      assertTrue(
          LoadBasedQuotaConfig.OutlierDetectionConfig.class.isInterface(),
          "OutlierDetectionConfig should be an interface");
    }
  }

  // ========================================================================
  // LoadThresholds Interface Method Tests
  // ========================================================================

  @Nested
  @DisplayName("LoadThresholds Interface Method Tests")
  class LoadThresholdsMethodTests {

    @Test
    @DisplayName("LoadThresholds should have getLowThreshold method")
    void shouldHaveGetLowThresholdMethod() throws NoSuchMethodException {
      Method method = LoadBasedQuotaConfig.LoadThresholds.class.getMethod("getLowThreshold");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }

    @Test
    @DisplayName("LoadThresholds should have getMediumThreshold method")
    void shouldHaveGetMediumThresholdMethod() throws NoSuchMethodException {
      Method method = LoadBasedQuotaConfig.LoadThresholds.class.getMethod("getMediumThreshold");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }

    @Test
    @DisplayName("LoadThresholds should have getHighThreshold method")
    void shouldHaveGetHighThresholdMethod() throws NoSuchMethodException {
      Method method = LoadBasedQuotaConfig.LoadThresholds.class.getMethod("getHighThreshold");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }

    @Test
    @DisplayName("LoadThresholds should have getCriticalThreshold method")
    void shouldHaveGetCriticalThresholdMethod() throws NoSuchMethodException {
      Method method = LoadBasedQuotaConfig.LoadThresholds.class.getMethod("getCriticalThreshold");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }

    @Test
    @DisplayName("LoadThresholds should have getHysteresis method")
    void shouldHaveGetHysteresisMethod() throws NoSuchMethodException {
      Method method = LoadBasedQuotaConfig.LoadThresholds.class.getMethod("getHysteresis");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }
  }

  // ========================================================================
  // QuotaBounds Interface Method Tests
  // ========================================================================

  @Nested
  @DisplayName("QuotaBounds Interface Method Tests")
  class QuotaBoundsMethodTests {

    @Test
    @DisplayName("QuotaBounds should have getMinFuelQuota method")
    void shouldHaveGetMinFuelQuotaMethod() throws NoSuchMethodException {
      Method method = LoadBasedQuotaConfig.QuotaBounds.class.getMethod("getMinFuelQuota");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("QuotaBounds should have getMaxFuelQuota method")
    void shouldHaveGetMaxFuelQuotaMethod() throws NoSuchMethodException {
      Method method = LoadBasedQuotaConfig.QuotaBounds.class.getMethod("getMaxFuelQuota");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("QuotaBounds should have getMinMemoryQuota method")
    void shouldHaveGetMinMemoryQuotaMethod() throws NoSuchMethodException {
      Method method = LoadBasedQuotaConfig.QuotaBounds.class.getMethod("getMinMemoryQuota");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("QuotaBounds should have getMaxMemoryQuota method")
    void shouldHaveGetMaxMemoryQuotaMethod() throws NoSuchMethodException {
      Method method = LoadBasedQuotaConfig.QuotaBounds.class.getMethod("getMaxMemoryQuota");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("QuotaBounds should have getMinTimeQuota method")
    void shouldHaveGetMinTimeQuotaMethod() throws NoSuchMethodException {
      Method method = LoadBasedQuotaConfig.QuotaBounds.class.getMethod("getMinTimeQuota");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("QuotaBounds should have getMaxTimeQuota method")
    void shouldHaveGetMaxTimeQuotaMethod() throws NoSuchMethodException {
      Method method = LoadBasedQuotaConfig.QuotaBounds.class.getMethod("getMaxTimeQuota");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }
  }

  // ========================================================================
  // Nested Type Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Type Count Tests")
  class NestedTypeCountTests {

    @Test
    @DisplayName("LoadBasedQuotaConfig should have 5 nested enums")
    void shouldHaveFiveNestedEnums() {
      Class<?>[] nestedClasses = LoadBasedQuotaConfig.class.getDeclaredClasses();
      int enumCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isEnum()) {
          enumCount++;
        }
      }
      assertEquals(5, enumCount, "LoadBasedQuotaConfig should have 5 nested enums");
    }

    @Test
    @DisplayName("LoadBasedQuotaConfig should have 4 nested interfaces")
    void shouldHaveFourNestedInterfaces() {
      Class<?>[] nestedClasses = LoadBasedQuotaConfig.class.getDeclaredClasses();
      int interfaceCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isInterface() && !nested.isAnnotation()) {
          interfaceCount++;
        }
      }
      assertEquals(4, interfaceCount, "LoadBasedQuotaConfig should have 4 nested interfaces");
    }
  }
}
