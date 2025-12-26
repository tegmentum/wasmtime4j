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
 * Tests for the ExecutionMonitoringConfig interface.
 *
 * <p>This test class verifies the interface structure, methods, and nested types for
 * ExecutionMonitoringConfig using reflection-based testing.
 */
@DisplayName("ExecutionMonitoringConfig Tests")
class ExecutionMonitoringConfigTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("ExecutionMonitoringConfig should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          ExecutionMonitoringConfig.class.isInterface(),
          "ExecutionMonitoringConfig should be an interface");
    }

    @Test
    @DisplayName("ExecutionMonitoringConfig should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExecutionMonitoringConfig.class.getModifiers()),
          "ExecutionMonitoringConfig should be public");
    }

    @Test
    @DisplayName("ExecutionMonitoringConfig should not extend other interfaces")
    void shouldNotExtendOtherInterfaces() {
      Class<?>[] interfaces = ExecutionMonitoringConfig.class.getInterfaces();
      assertEquals(
          0, interfaces.length, "ExecutionMonitoringConfig should not extend other interfaces");
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
      Method method = ExecutionMonitoringConfig.class.getMethod("isEnabled");
      assertNotNull(method, "isEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have setEnabled method")
    void shouldHaveSetEnabledMethod() throws NoSuchMethodException {
      Method method = ExecutionMonitoringConfig.class.getMethod("setEnabled", boolean.class);
      assertNotNull(method, "setEnabled method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getSamplingInterval method")
    void shouldHaveGetSamplingIntervalMethod() throws NoSuchMethodException {
      Method method = ExecutionMonitoringConfig.class.getMethod("getSamplingInterval");
      assertNotNull(method, "getSamplingInterval method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have setSamplingInterval method")
    void shouldHaveSetSamplingIntervalMethod() throws NoSuchMethodException {
      Method method = ExecutionMonitoringConfig.class.getMethod("setSamplingInterval", long.class);
      assertNotNull(method, "setSamplingInterval method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getMonitoredMetrics method")
    void shouldHaveGetMonitoredMetricsMethod() throws NoSuchMethodException {
      Method method = ExecutionMonitoringConfig.class.getMethod("getMonitoredMetrics");
      assertNotNull(method, "getMonitoredMetrics method should exist");
      assertEquals(Set.class, method.getReturnType(), "Return type should be Set");
    }

    @Test
    @DisplayName("should have addMonitoredMetric method")
    void shouldHaveAddMonitoredMetricMethod() throws NoSuchMethodException {
      Method method =
          ExecutionMonitoringConfig.class.getMethod(
              "addMonitoredMetric", ExecutionMonitoringConfig.MonitoredMetric.class);
      assertNotNull(method, "addMonitoredMetric method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have removeMonitoredMetric method")
    void shouldHaveRemoveMonitoredMetricMethod() throws NoSuchMethodException {
      Method method =
          ExecutionMonitoringConfig.class.getMethod(
              "removeMonitoredMetric", ExecutionMonitoringConfig.MonitoredMetric.class);
      assertNotNull(method, "removeMonitoredMetric method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getOutputFormat method")
    void shouldHaveGetOutputFormatMethod() throws NoSuchMethodException {
      Method method = ExecutionMonitoringConfig.class.getMethod("getOutputFormat");
      assertNotNull(method, "getOutputFormat method should exist");
      assertEquals(
          ExecutionMonitoringConfig.OutputFormat.class,
          method.getReturnType(),
          "Return type should be OutputFormat");
    }

    @Test
    @DisplayName("should have setOutputFormat method")
    void shouldHaveSetOutputFormatMethod() throws NoSuchMethodException {
      Method method =
          ExecutionMonitoringConfig.class.getMethod(
              "setOutputFormat", ExecutionMonitoringConfig.OutputFormat.class);
      assertNotNull(method, "setOutputFormat method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getOutputDestination method")
    void shouldHaveGetOutputDestinationMethod() throws NoSuchMethodException {
      Method method = ExecutionMonitoringConfig.class.getMethod("getOutputDestination");
      assertNotNull(method, "getOutputDestination method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have setOutputDestination method")
    void shouldHaveSetOutputDestinationMethod() throws NoSuchMethodException {
      Method method =
          ExecutionMonitoringConfig.class.getMethod("setOutputDestination", String.class);
      assertNotNull(method, "setOutputDestination method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getAlertThresholds method")
    void shouldHaveGetAlertThresholdsMethod() throws NoSuchMethodException {
      Method method = ExecutionMonitoringConfig.class.getMethod("getAlertThresholds");
      assertNotNull(method, "getAlertThresholds method should exist");
      assertEquals(
          ExecutionMonitoringConfig.AlertThresholds.class,
          method.getReturnType(),
          "Return type should be AlertThresholds");
    }

    @Test
    @DisplayName("should have setAlertThresholds method")
    void shouldHaveSetAlertThresholdsMethod() throws NoSuchMethodException {
      Method method =
          ExecutionMonitoringConfig.class.getMethod(
              "setAlertThresholds", ExecutionMonitoringConfig.AlertThresholds.class);
      assertNotNull(method, "setAlertThresholds method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }
  }

  // ========================================================================
  // MonitoredMetric Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("MonitoredMetric Enum Tests")
  class MonitoredMetricTests {

    @Test
    @DisplayName("MonitoredMetric should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          ExecutionMonitoringConfig.MonitoredMetric.class.isEnum(),
          "MonitoredMetric should be an enum");
      assertTrue(
          ExecutionMonitoringConfig.MonitoredMetric.class.isMemberClass(),
          "MonitoredMetric should be a member class");
    }

    @Test
    @DisplayName("MonitoredMetric should have 7 values")
    void shouldHaveSevenValues() {
      ExecutionMonitoringConfig.MonitoredMetric[] values =
          ExecutionMonitoringConfig.MonitoredMetric.values();
      assertEquals(7, values.length, "MonitoredMetric should have 7 values");
    }

    @Test
    @DisplayName("MonitoredMetric should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames =
          Set.of(
              "CPU_USAGE",
              "MEMORY_USAGE",
              "INSTRUCTION_COUNT",
              "FUNCTION_CALLS",
              "EXECUTION_TIME",
              "GARBAGE_COLLECTION",
              "THREAD_COUNT");
      Set<String> actualNames = new HashSet<>();
      for (ExecutionMonitoringConfig.MonitoredMetric metric :
          ExecutionMonitoringConfig.MonitoredMetric.values()) {
        actualNames.add(metric.name());
      }
      assertEquals(expectedNames, actualNames, "MonitoredMetric should have expected values");
    }
  }

  // ========================================================================
  // OutputFormat Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("OutputFormat Enum Tests")
  class OutputFormatTests {

    @Test
    @DisplayName("OutputFormat should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          ExecutionMonitoringConfig.OutputFormat.class.isEnum(), "OutputFormat should be an enum");
      assertTrue(
          ExecutionMonitoringConfig.OutputFormat.class.isMemberClass(),
          "OutputFormat should be a member class");
    }

    @Test
    @DisplayName("OutputFormat should have 4 values")
    void shouldHaveFourValues() {
      ExecutionMonitoringConfig.OutputFormat[] values =
          ExecutionMonitoringConfig.OutputFormat.values();
      assertEquals(4, values.length, "OutputFormat should have 4 values");
    }

    @Test
    @DisplayName("OutputFormat should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames = Set.of("JSON", "CSV", "TEXT", "BINARY");
      Set<String> actualNames = new HashSet<>();
      for (ExecutionMonitoringConfig.OutputFormat format :
          ExecutionMonitoringConfig.OutputFormat.values()) {
        actualNames.add(format.name());
      }
      assertEquals(expectedNames, actualNames, "OutputFormat should have expected values");
    }
  }

  // ========================================================================
  // AlertThresholds Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("AlertThresholds Interface Tests")
  class AlertThresholdsTests {

    @Test
    @DisplayName("AlertThresholds should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          ExecutionMonitoringConfig.AlertThresholds.class.isInterface(),
          "AlertThresholds should be an interface");
      assertTrue(
          ExecutionMonitoringConfig.AlertThresholds.class.isMemberClass(),
          "AlertThresholds should be a member class");
    }

    @Test
    @DisplayName("AlertThresholds should have getCpuUsageThreshold method")
    void shouldHaveGetCpuUsageThresholdMethod() throws NoSuchMethodException {
      Method method =
          ExecutionMonitoringConfig.AlertThresholds.class.getMethod("getCpuUsageThreshold");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }

    @Test
    @DisplayName("AlertThresholds should have getMemoryUsageThreshold method")
    void shouldHaveGetMemoryUsageThresholdMethod() throws NoSuchMethodException {
      Method method =
          ExecutionMonitoringConfig.AlertThresholds.class.getMethod("getMemoryUsageThreshold");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }

    @Test
    @DisplayName("AlertThresholds should have getExecutionTimeThreshold method")
    void shouldHaveGetExecutionTimeThresholdMethod() throws NoSuchMethodException {
      Method method =
          ExecutionMonitoringConfig.AlertThresholds.class.getMethod("getExecutionTimeThreshold");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("AlertThresholds should have getErrorRateThreshold method")
    void shouldHaveGetErrorRateThresholdMethod() throws NoSuchMethodException {
      Method method =
          ExecutionMonitoringConfig.AlertThresholds.class.getMethod("getErrorRateThreshold");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }
  }

  // ========================================================================
  // Nested Type Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Type Count Tests")
  class NestedTypeCountTests {

    @Test
    @DisplayName("ExecutionMonitoringConfig should have 2 nested enums")
    void shouldHaveTwoNestedEnums() {
      Class<?>[] nestedClasses = ExecutionMonitoringConfig.class.getDeclaredClasses();
      int enumCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isEnum()) {
          enumCount++;
        }
      }
      assertEquals(2, enumCount, "ExecutionMonitoringConfig should have 2 nested enums");
    }

    @Test
    @DisplayName("ExecutionMonitoringConfig should have 1 nested interface")
    void shouldHaveOneNestedInterface() {
      Class<?>[] nestedClasses = ExecutionMonitoringConfig.class.getDeclaredClasses();
      int interfaceCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isInterface() && !nested.isAnnotation()) {
          interfaceCount++;
        }
      }
      assertEquals(1, interfaceCount, "ExecutionMonitoringConfig should have 1 nested interface");
    }
  }
}
