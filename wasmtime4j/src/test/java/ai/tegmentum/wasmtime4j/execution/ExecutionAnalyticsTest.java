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
 * Tests for the ExecutionAnalytics interface.
 *
 * <p>This test class verifies the interface structure, methods, nested types, and enums for
 * ExecutionAnalytics using reflection-based testing.
 */
@DisplayName("ExecutionAnalytics Tests")
class ExecutionAnalyticsTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("ExecutionAnalytics should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          ExecutionAnalytics.class.isInterface(), "ExecutionAnalytics should be an interface");
    }

    @Test
    @DisplayName("ExecutionAnalytics should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExecutionAnalytics.class.getModifiers()),
          "ExecutionAnalytics should be public");
    }

    @Test
    @DisplayName("ExecutionAnalytics should not extend other interfaces")
    void shouldNotExtendOtherInterfaces() {
      Class<?>[] interfaces = ExecutionAnalytics.class.getInterfaces();
      assertEquals(0, interfaces.length, "ExecutionAnalytics should not extend other interfaces");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getPerformanceAnalytics method")
    void shouldHaveGetPerformanceAnalyticsMethod() throws NoSuchMethodException {
      Method method = ExecutionAnalytics.class.getMethod("getPerformanceAnalytics");
      assertNotNull(method, "getPerformanceAnalytics method should exist");
      assertEquals(
          ExecutionAnalytics.PerformanceAnalytics.class,
          method.getReturnType(),
          "Return type should be PerformanceAnalytics");
    }

    @Test
    @DisplayName("should have getResourceAnalytics method")
    void shouldHaveGetResourceAnalyticsMethod() throws NoSuchMethodException {
      Method method = ExecutionAnalytics.class.getMethod("getResourceAnalytics");
      assertNotNull(method, "getResourceAnalytics method should exist");
      assertEquals(
          ExecutionAnalytics.ResourceAnalytics.class,
          method.getReturnType(),
          "Return type should be ResourceAnalytics");
    }

    @Test
    @DisplayName("should have getErrorAnalytics method")
    void shouldHaveGetErrorAnalyticsMethod() throws NoSuchMethodException {
      Method method = ExecutionAnalytics.class.getMethod("getErrorAnalytics");
      assertNotNull(method, "getErrorAnalytics method should exist");
      assertEquals(
          ExecutionAnalytics.ErrorAnalytics.class,
          method.getReturnType(),
          "Return type should be ErrorAnalytics");
    }

    @Test
    @DisplayName("should have getTrendAnalytics method")
    void shouldHaveGetTrendAnalyticsMethod() throws NoSuchMethodException {
      Method method = ExecutionAnalytics.class.getMethod("getTrendAnalytics");
      assertNotNull(method, "getTrendAnalytics method should exist");
      assertEquals(
          ExecutionAnalytics.TrendAnalytics.class,
          method.getReturnType(),
          "Return type should be TrendAnalytics");
    }

    @Test
    @DisplayName("should have getTimeRange method")
    void shouldHaveGetTimeRangeMethod() throws NoSuchMethodException {
      Method method = ExecutionAnalytics.class.getMethod("getTimeRange");
      assertNotNull(method, "getTimeRange method should exist");
      assertEquals(
          ExecutionAnalytics.TimeRange.class,
          method.getReturnType(),
          "Return type should be TimeRange");
    }

    @Test
    @DisplayName("should have setTimeRange method")
    void shouldHaveSetTimeRangeMethod() throws NoSuchMethodException {
      Method method =
          ExecutionAnalytics.class.getMethod("setTimeRange", ExecutionAnalytics.TimeRange.class);
      assertNotNull(method, "setTimeRange method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have refresh method")
    void shouldHaveRefreshMethod() throws NoSuchMethodException {
      Method method = ExecutionAnalytics.class.getMethod("refresh");
      assertNotNull(method, "refresh method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have export method")
    void shouldHaveExportMethod() throws NoSuchMethodException {
      Method method =
          ExecutionAnalytics.class.getMethod("export", ExecutionAnalytics.ExportFormat.class);
      assertNotNull(method, "export method should exist");
      assertEquals(byte[].class, method.getReturnType(), "Return type should be byte[]");
    }
  }

  // ========================================================================
  // ExportFormat Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ExportFormat Enum Tests")
  class ExportFormatTests {

    @Test
    @DisplayName("ExportFormat should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(ExecutionAnalytics.ExportFormat.class.isEnum(), "ExportFormat should be an enum");
      assertTrue(
          ExecutionAnalytics.ExportFormat.class.isMemberClass(),
          "ExportFormat should be a member class");
    }

    @Test
    @DisplayName("ExportFormat should have 4 values")
    void shouldHaveFourValues() {
      ExecutionAnalytics.ExportFormat[] values = ExecutionAnalytics.ExportFormat.values();
      assertEquals(4, values.length, "ExportFormat should have 4 values");
    }

    @Test
    @DisplayName("ExportFormat should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames = Set.of("JSON", "CSV", "EXCEL", "PDF");
      Set<String> actualNames = new HashSet<>();
      for (ExecutionAnalytics.ExportFormat format : ExecutionAnalytics.ExportFormat.values()) {
        actualNames.add(format.name());
      }
      assertEquals(expectedNames, actualNames, "ExportFormat should have expected values");
    }
  }

  // ========================================================================
  // TrendDirection Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("TrendDirection Enum Tests")
  class TrendDirectionTests {

    @Test
    @DisplayName("TrendDirection should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          ExecutionAnalytics.TrendDirection.class.isEnum(), "TrendDirection should be an enum");
    }

    @Test
    @DisplayName("TrendDirection should have 4 values")
    void shouldHaveFourValues() {
      ExecutionAnalytics.TrendDirection[] values = ExecutionAnalytics.TrendDirection.values();
      assertEquals(4, values.length, "TrendDirection should have 4 values");
    }

    @Test
    @DisplayName("TrendDirection should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames = Set.of("UPWARD", "DOWNWARD", "STABLE", "UNCLEAR");
      Set<String> actualNames = new HashSet<>();
      for (ExecutionAnalytics.TrendDirection direction :
          ExecutionAnalytics.TrendDirection.values()) {
        actualNames.add(direction.name());
      }
      assertEquals(expectedNames, actualNames, "TrendDirection should have expected values");
    }
  }

  // ========================================================================
  // Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Interface Tests")
  class NestedInterfaceTests {

    @Test
    @DisplayName("PerformanceAnalytics should be a nested interface")
    void performanceAnalyticsShouldBeNestedInterface() {
      assertTrue(
          ExecutionAnalytics.PerformanceAnalytics.class.isInterface(),
          "PerformanceAnalytics should be an interface");
      assertTrue(
          ExecutionAnalytics.PerformanceAnalytics.class.isMemberClass(),
          "PerformanceAnalytics should be a member class");
    }

    @Test
    @DisplayName("ResourceAnalytics should be a nested interface")
    void resourceAnalyticsShouldBeNestedInterface() {
      assertTrue(
          ExecutionAnalytics.ResourceAnalytics.class.isInterface(),
          "ResourceAnalytics should be an interface");
    }

    @Test
    @DisplayName("ErrorAnalytics should be a nested interface")
    void errorAnalyticsShouldBeNestedInterface() {
      assertTrue(
          ExecutionAnalytics.ErrorAnalytics.class.isInterface(),
          "ErrorAnalytics should be an interface");
    }

    @Test
    @DisplayName("TrendAnalytics should be a nested interface")
    void trendAnalyticsShouldBeNestedInterface() {
      assertTrue(
          ExecutionAnalytics.TrendAnalytics.class.isInterface(),
          "TrendAnalytics should be an interface");
    }

    @Test
    @DisplayName("TimeRange should be a nested interface")
    void timeRangeShouldBeNestedInterface() {
      assertTrue(
          ExecutionAnalytics.TimeRange.class.isInterface(), "TimeRange should be an interface");
    }

    @Test
    @DisplayName("LatencyStatistics should be a nested interface")
    void latencyStatisticsShouldBeNestedInterface() {
      assertTrue(
          ExecutionAnalytics.LatencyStatistics.class.isInterface(),
          "LatencyStatistics should be an interface");
    }

    @Test
    @DisplayName("MemoryUsageStatistics should be a nested interface")
    void memoryUsageStatisticsShouldBeNestedInterface() {
      assertTrue(
          ExecutionAnalytics.MemoryUsageStatistics.class.isInterface(),
          "MemoryUsageStatistics should be an interface");
    }

    @Test
    @DisplayName("CpuUsageStatistics should be a nested interface")
    void cpuUsageStatisticsShouldBeNestedInterface() {
      assertTrue(
          ExecutionAnalytics.CpuUsageStatistics.class.isInterface(),
          "CpuUsageStatistics should be an interface");
    }

    @Test
    @DisplayName("FuelConsumptionStatistics should be a nested interface")
    void fuelConsumptionStatisticsShouldBeNestedInterface() {
      assertTrue(
          ExecutionAnalytics.FuelConsumptionStatistics.class.isInterface(),
          "FuelConsumptionStatistics should be an interface");
    }

    @Test
    @DisplayName("ErrorInfo should be a nested interface")
    void errorInfoShouldBeNestedInterface() {
      assertTrue(
          ExecutionAnalytics.ErrorInfo.class.isInterface(), "ErrorInfo should be an interface");
    }

    @Test
    @DisplayName("TrendData should be a nested interface")
    void trendDataShouldBeNestedInterface() {
      assertTrue(
          ExecutionAnalytics.TrendData.class.isInterface(), "TrendData should be an interface");
    }

    @Test
    @DisplayName("DataPoint should be a nested interface")
    void dataPointShouldBeNestedInterface() {
      assertTrue(
          ExecutionAnalytics.DataPoint.class.isInterface(), "DataPoint should be an interface");
    }
  }

  // ========================================================================
  // PerformanceAnalytics Interface Method Tests
  // ========================================================================

  @Nested
  @DisplayName("PerformanceAnalytics Interface Method Tests")
  class PerformanceAnalyticsMethodTests {

    @Test
    @DisplayName("PerformanceAnalytics should have getAverageExecutionTime method")
    void shouldHaveGetAverageExecutionTimeMethod() throws NoSuchMethodException {
      Method method =
          ExecutionAnalytics.PerformanceAnalytics.class.getMethod("getAverageExecutionTime");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }

    @Test
    @DisplayName("PerformanceAnalytics should have getPercentiles method")
    void shouldHaveGetPercentilesMethod() throws NoSuchMethodException {
      Method method = ExecutionAnalytics.PerformanceAnalytics.class.getMethod("getPercentiles");
      assertEquals(java.util.Map.class, method.getReturnType(), "Return type should be Map");
    }

    @Test
    @DisplayName("PerformanceAnalytics should have getThroughput method")
    void shouldHaveGetThroughputMethod() throws NoSuchMethodException {
      Method method = ExecutionAnalytics.PerformanceAnalytics.class.getMethod("getThroughput");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }

    @Test
    @DisplayName("PerformanceAnalytics should have getLatencyStatistics method")
    void shouldHaveGetLatencyStatisticsMethod() throws NoSuchMethodException {
      Method method =
          ExecutionAnalytics.PerformanceAnalytics.class.getMethod("getLatencyStatistics");
      assertEquals(
          ExecutionAnalytics.LatencyStatistics.class,
          method.getReturnType(),
          "Return type should be LatencyStatistics");
    }
  }

  // ========================================================================
  // TrendData Interface Method Tests
  // ========================================================================

  @Nested
  @DisplayName("TrendData Interface Method Tests")
  class TrendDataMethodTests {

    @Test
    @DisplayName("TrendData should have getDataPoints method")
    void shouldHaveGetDataPointsMethod() throws NoSuchMethodException {
      Method method = ExecutionAnalytics.TrendData.class.getMethod("getDataPoints");
      assertEquals(java.util.List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("TrendData should have getDirection method")
    void shouldHaveGetDirectionMethod() throws NoSuchMethodException {
      Method method = ExecutionAnalytics.TrendData.class.getMethod("getDirection");
      assertEquals(
          ExecutionAnalytics.TrendDirection.class,
          method.getReturnType(),
          "Return type should be TrendDirection");
    }

    @Test
    @DisplayName("TrendData should have getStrength method")
    void shouldHaveGetStrengthMethod() throws NoSuchMethodException {
      Method method = ExecutionAnalytics.TrendData.class.getMethod("getStrength");
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
    @DisplayName("ExecutionAnalytics should have 2 nested enums")
    void shouldHaveTwoNestedEnums() {
      Class<?>[] nestedClasses = ExecutionAnalytics.class.getDeclaredClasses();
      int enumCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isEnum()) {
          enumCount++;
        }
      }
      assertEquals(2, enumCount, "ExecutionAnalytics should have 2 nested enums");
    }

    @Test
    @DisplayName("ExecutionAnalytics should have 12 nested interfaces")
    void shouldHaveTwelveNestedInterfaces() {
      Class<?>[] nestedClasses = ExecutionAnalytics.class.getDeclaredClasses();
      int interfaceCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isInterface() && !nested.isAnnotation()) {
          interfaceCount++;
        }
      }
      assertEquals(12, interfaceCount, "ExecutionAnalytics should have 12 nested interfaces");
    }
  }
}
