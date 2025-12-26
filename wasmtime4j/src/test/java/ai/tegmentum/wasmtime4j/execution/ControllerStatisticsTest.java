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
 * Tests for the ControllerStatistics interface.
 *
 * <p>This test class verifies the interface structure, methods, and nested types for
 * ControllerStatistics using reflection-based testing.
 */
@DisplayName("ControllerStatistics Tests")
class ControllerStatisticsTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("ControllerStatistics should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          ControllerStatistics.class.isInterface(), "ControllerStatistics should be an interface");
    }

    @Test
    @DisplayName("ControllerStatistics should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ControllerStatistics.class.getModifiers()),
          "ControllerStatistics should be public");
    }

    @Test
    @DisplayName("ControllerStatistics should not extend other interfaces")
    void shouldNotExtendOtherInterfaces() {
      Class<?>[] interfaces = ControllerStatistics.class.getInterfaces();
      assertEquals(0, interfaces.length, "ControllerStatistics should not extend other interfaces");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getStartTime method")
    void shouldHaveGetStartTimeMethod() throws NoSuchMethodException {
      Method method = ControllerStatistics.class.getMethod("getStartTime");
      assertNotNull(method, "getStartTime method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getEndTime method")
    void shouldHaveGetEndTimeMethod() throws NoSuchMethodException {
      Method method = ControllerStatistics.class.getMethod("getEndTime");
      assertNotNull(method, "getEndTime method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getDuration method")
    void shouldHaveGetDurationMethod() throws NoSuchMethodException {
      Method method = ControllerStatistics.class.getMethod("getDuration");
      assertNotNull(method, "getDuration method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getExecutionStatistics method")
    void shouldHaveGetExecutionStatisticsMethod() throws NoSuchMethodException {
      Method method = ControllerStatistics.class.getMethod("getExecutionStatistics");
      assertNotNull(method, "getExecutionStatistics method should exist");
      assertEquals(
          ExecutionStatistics.class,
          method.getReturnType(),
          "Return type should be ExecutionStatistics");
    }

    @Test
    @DisplayName("should have getResourceManagementStatistics method")
    void shouldHaveGetResourceManagementStatisticsMethod() throws NoSuchMethodException {
      Method method = ControllerStatistics.class.getMethod("getResourceManagementStatistics");
      assertNotNull(method, "getResourceManagementStatistics method should exist");
      assertEquals(
          ControllerStatistics.ResourceManagementStatistics.class,
          method.getReturnType(),
          "Return type should be ResourceManagementStatistics");
    }

    @Test
    @DisplayName("should have getPerformanceStatistics method")
    void shouldHaveGetPerformanceStatisticsMethod() throws NoSuchMethodException {
      Method method = ControllerStatistics.class.getMethod("getPerformanceStatistics");
      assertNotNull(method, "getPerformanceStatistics method should exist");
      assertEquals(
          ControllerStatistics.PerformanceStatistics.class,
          method.getReturnType(),
          "Return type should be PerformanceStatistics");
    }

    @Test
    @DisplayName("should have getErrorStatistics method")
    void shouldHaveGetErrorStatisticsMethod() throws NoSuchMethodException {
      Method method = ControllerStatistics.class.getMethod("getErrorStatistics");
      assertNotNull(method, "getErrorStatistics method should exist");
      assertEquals(
          ControllerStatistics.ErrorStatistics.class,
          method.getReturnType(),
          "Return type should be ErrorStatistics");
    }

    @Test
    @DisplayName("should have getThroughputStatistics method")
    void shouldHaveGetThroughputStatisticsMethod() throws NoSuchMethodException {
      Method method = ControllerStatistics.class.getMethod("getThroughputStatistics");
      assertNotNull(method, "getThroughputStatistics method should exist");
      assertEquals(
          ControllerStatistics.ThroughputStatistics.class,
          method.getReturnType(),
          "Return type should be ThroughputStatistics");
    }

    @Test
    @DisplayName("should have getLatencyStatistics method")
    void shouldHaveGetLatencyStatisticsMethod() throws NoSuchMethodException {
      Method method = ControllerStatistics.class.getMethod("getLatencyStatistics");
      assertNotNull(method, "getLatencyStatistics method should exist");
      assertEquals(
          ControllerStatistics.LatencyStatistics.class,
          method.getReturnType(),
          "Return type should be LatencyStatistics");
    }

    @Test
    @DisplayName("should have getQueueStatistics method")
    void shouldHaveGetQueueStatisticsMethod() throws NoSuchMethodException {
      Method method = ControllerStatistics.class.getMethod("getQueueStatistics");
      assertNotNull(method, "getQueueStatistics method should exist");
      assertEquals(
          ControllerStatistics.QueueStatistics.class,
          method.getReturnType(),
          "Return type should be QueueStatistics");
    }

    @Test
    @DisplayName("should have getAnomalyDetectionStatistics method")
    void shouldHaveGetAnomalyDetectionStatisticsMethod() throws NoSuchMethodException {
      Method method = ControllerStatistics.class.getMethod("getAnomalyDetectionStatistics");
      assertNotNull(method, "getAnomalyDetectionStatistics method should exist");
      assertEquals(
          ControllerStatistics.AnomalyDetectionStatistics.class,
          method.getReturnType(),
          "Return type should be AnomalyDetectionStatistics");
    }

    @Test
    @DisplayName("should have reset method")
    void shouldHaveResetMethod() throws NoSuchMethodException {
      Method method = ControllerStatistics.class.getMethod("reset");
      assertNotNull(method, "reset method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have snapshot method")
    void shouldHaveSnapshotMethod() throws NoSuchMethodException {
      Method method = ControllerStatistics.class.getMethod("snapshot");
      assertNotNull(method, "snapshot method should exist");
      assertEquals(
          ControllerStatistics.class,
          method.getReturnType(),
          "Return type should be ControllerStatistics");
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
          ControllerStatistics.TrendDirection.class.isEnum(), "TrendDirection should be an enum");
      assertTrue(
          ControllerStatistics.TrendDirection.class.isMemberClass(),
          "TrendDirection should be a member class");
    }

    @Test
    @DisplayName("TrendDirection should have 4 values")
    void shouldHaveFourValues() {
      ControllerStatistics.TrendDirection[] values = ControllerStatistics.TrendDirection.values();
      assertEquals(4, values.length, "TrendDirection should have 4 values");
    }

    @Test
    @DisplayName("TrendDirection should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames = Set.of("INCREASING", "DECREASING", "STABLE", "VOLATILE");
      Set<String> actualNames = new HashSet<>();
      for (ControllerStatistics.TrendDirection dir : ControllerStatistics.TrendDirection.values()) {
        actualNames.add(dir.name());
      }
      assertEquals(expectedNames, actualNames, "TrendDirection should have expected values");
    }
  }

  // ========================================================================
  // ResourceManagementStatistics Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("ResourceManagementStatistics Interface Tests")
  class ResourceManagementStatisticsTests {

    @Test
    @DisplayName("ResourceManagementStatistics should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          ControllerStatistics.ResourceManagementStatistics.class.isInterface(),
          "ResourceManagementStatistics should be an interface");
    }

    @Test
    @DisplayName("ResourceManagementStatistics should have 7 methods")
    void shouldHaveSevenMethods() {
      Method[] methods =
          ControllerStatistics.ResourceManagementStatistics.class.getDeclaredMethods();
      assertEquals(7, methods.length, "ResourceManagementStatistics should have 7 methods");
    }

    @Test
    @DisplayName("ResourceManagementStatistics should have getTotalAllocations method")
    void shouldHaveGetTotalAllocationsMethod() throws NoSuchMethodException {
      Method method =
          ControllerStatistics.ResourceManagementStatistics.class.getMethod("getTotalAllocations");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }
  }

  // ========================================================================
  // PerformanceStatistics Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("PerformanceStatistics Interface Tests")
  class PerformanceStatisticsTests {

    @Test
    @DisplayName("PerformanceStatistics should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          ControllerStatistics.PerformanceStatistics.class.isInterface(),
          "PerformanceStatistics should be an interface");
    }

    @Test
    @DisplayName("PerformanceStatistics should have 6 methods")
    void shouldHaveSixMethods() {
      Method[] methods = ControllerStatistics.PerformanceStatistics.class.getDeclaredMethods();
      assertEquals(6, methods.length, "PerformanceStatistics should have 6 methods");
    }
  }

  // ========================================================================
  // ErrorStatistics Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("ErrorStatistics Interface Tests")
  class ErrorStatisticsTests {

    @Test
    @DisplayName("ErrorStatistics should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          ControllerStatistics.ErrorStatistics.class.isInterface(),
          "ErrorStatistics should be an interface");
    }

    @Test
    @DisplayName("ErrorStatistics should have 6 methods")
    void shouldHaveSixMethods() {
      Method[] methods = ControllerStatistics.ErrorStatistics.class.getDeclaredMethods();
      assertEquals(6, methods.length, "ErrorStatistics should have 6 methods");
    }
  }

  // ========================================================================
  // ThroughputStatistics Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("ThroughputStatistics Interface Tests")
  class ThroughputStatisticsTests {

    @Test
    @DisplayName("ThroughputStatistics should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          ControllerStatistics.ThroughputStatistics.class.isInterface(),
          "ThroughputStatistics should be an interface");
    }

    @Test
    @DisplayName("ThroughputStatistics should have 5 methods")
    void shouldHaveFiveMethods() {
      Method[] methods = ControllerStatistics.ThroughputStatistics.class.getDeclaredMethods();
      assertEquals(5, methods.length, "ThroughputStatistics should have 5 methods");
    }
  }

  // ========================================================================
  // LatencyStatistics Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("LatencyStatistics Interface Tests")
  class LatencyStatisticsTests {

    @Test
    @DisplayName("LatencyStatistics should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          ControllerStatistics.LatencyStatistics.class.isInterface(),
          "LatencyStatistics should be an interface");
    }

    @Test
    @DisplayName("LatencyStatistics should have 5 methods")
    void shouldHaveFiveMethods() {
      Method[] methods = ControllerStatistics.LatencyStatistics.class.getDeclaredMethods();
      assertEquals(5, methods.length, "LatencyStatistics should have 5 methods");
    }
  }

  // ========================================================================
  // QueueStatistics Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("QueueStatistics Interface Tests")
  class QueueStatisticsTests {

    @Test
    @DisplayName("QueueStatistics should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          ControllerStatistics.QueueStatistics.class.isInterface(),
          "QueueStatistics should be an interface");
    }

    @Test
    @DisplayName("QueueStatistics should have 6 methods")
    void shouldHaveSixMethods() {
      Method[] methods = ControllerStatistics.QueueStatistics.class.getDeclaredMethods();
      assertEquals(6, methods.length, "QueueStatistics should have 6 methods");
    }
  }

  // ========================================================================
  // AnomalyDetectionStatistics Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("AnomalyDetectionStatistics Interface Tests")
  class AnomalyDetectionStatisticsTests {

    @Test
    @DisplayName("AnomalyDetectionStatistics should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          ControllerStatistics.AnomalyDetectionStatistics.class.isInterface(),
          "AnomalyDetectionStatistics should be an interface");
    }

    @Test
    @DisplayName("AnomalyDetectionStatistics should have 5 methods")
    void shouldHaveFiveMethods() {
      Method[] methods = ControllerStatistics.AnomalyDetectionStatistics.class.getDeclaredMethods();
      assertEquals(5, methods.length, "AnomalyDetectionStatistics should have 5 methods");
    }
  }

  // ========================================================================
  // JitCompilationStatistics Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("JitCompilationStatistics Interface Tests")
  class JitCompilationStatisticsTests {

    @Test
    @DisplayName("JitCompilationStatistics should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          ControllerStatistics.JitCompilationStatistics.class.isInterface(),
          "JitCompilationStatistics should be an interface");
    }

    @Test
    @DisplayName("JitCompilationStatistics should have 5 methods")
    void shouldHaveFiveMethods() {
      Method[] methods = ControllerStatistics.JitCompilationStatistics.class.getDeclaredMethods();
      assertEquals(5, methods.length, "JitCompilationStatistics should have 5 methods");
    }
  }

  // ========================================================================
  // ErrorInfo Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("ErrorInfo Interface Tests")
  class ErrorInfoTests {

    @Test
    @DisplayName("ErrorInfo should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          ControllerStatistics.ErrorInfo.class.isInterface(), "ErrorInfo should be an interface");
    }

    @Test
    @DisplayName("ErrorInfo should have 4 methods")
    void shouldHaveFourMethods() {
      Method[] methods = ControllerStatistics.ErrorInfo.class.getDeclaredMethods();
      assertEquals(4, methods.length, "ErrorInfo should have 4 methods");
    }
  }

  // ========================================================================
  // ThroughputTrend Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("ThroughputTrend Interface Tests")
  class ThroughputTrendTests {

    @Test
    @DisplayName("ThroughputTrend should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          ControllerStatistics.ThroughputTrend.class.isInterface(),
          "ThroughputTrend should be an interface");
    }

    @Test
    @DisplayName("ThroughputTrend should have 3 methods")
    void shouldHaveThreeMethods() {
      Method[] methods = ControllerStatistics.ThroughputTrend.class.getDeclaredMethods();
      assertEquals(3, methods.length, "ThroughputTrend should have 3 methods");
    }
  }

  // ========================================================================
  // LatencyDistribution Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("LatencyDistribution Interface Tests")
  class LatencyDistributionTests {

    @Test
    @DisplayName("LatencyDistribution should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          ControllerStatistics.LatencyDistribution.class.isInterface(),
          "LatencyDistribution should be an interface");
    }

    @Test
    @DisplayName("LatencyDistribution should have 3 methods")
    void shouldHaveThreeMethods() {
      Method[] methods = ControllerStatistics.LatencyDistribution.class.getDeclaredMethods();
      assertEquals(3, methods.length, "LatencyDistribution should have 3 methods");
    }
  }

  // ========================================================================
  // ThroughputDataPoint Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("ThroughputDataPoint Interface Tests")
  class ThroughputDataPointTests {

    @Test
    @DisplayName("ThroughputDataPoint should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          ControllerStatistics.ThroughputDataPoint.class.isInterface(),
          "ThroughputDataPoint should be an interface");
    }

    @Test
    @DisplayName("ThroughputDataPoint should have 2 methods")
    void shouldHaveTwoMethods() {
      Method[] methods = ControllerStatistics.ThroughputDataPoint.class.getDeclaredMethods();
      assertEquals(2, methods.length, "ThroughputDataPoint should have 2 methods");
    }
  }

  // ========================================================================
  // Nested Type Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Type Count Tests")
  class NestedTypeCountTests {

    @Test
    @DisplayName("ControllerStatistics should have 1 nested enum")
    void shouldHaveOneNestedEnum() {
      Class<?>[] nestedClasses = ControllerStatistics.class.getDeclaredClasses();
      int enumCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isEnum()) {
          enumCount++;
        }
      }
      assertEquals(1, enumCount, "ControllerStatistics should have 1 nested enum");
    }

    @Test
    @DisplayName("ControllerStatistics should have 12 nested interfaces")
    void shouldHaveTwelveNestedInterfaces() {
      Class<?>[] nestedClasses = ControllerStatistics.class.getDeclaredClasses();
      int interfaceCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isInterface() && !nested.isAnnotation()) {
          interfaceCount++;
        }
      }
      assertEquals(12, interfaceCount, "ControllerStatistics should have 12 nested interfaces");
    }
  }
}
