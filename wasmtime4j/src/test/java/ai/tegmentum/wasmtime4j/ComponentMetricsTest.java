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

import ai.tegmentum.wasmtime4j.ComponentMetrics.ExportFormat;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentMetrics} interface.
 *
 * <p>ComponentMetrics provides metrics for WebAssembly component operations.
 */
@DisplayName("ComponentMetrics Tests")
class ComponentMetricsTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentMetrics.class.getModifiers()),
          "ComponentMetrics should be public");
      assertTrue(ComponentMetrics.class.isInterface(), "ComponentMetrics should be an interface");
    }

    @Test
    @DisplayName("should have ExecutionMetrics nested interface")
    void shouldHaveExecutionMetricsNestedInterface() {
      final var nestedClasses = ComponentMetrics.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("ExecutionMetrics")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "ExecutionMetrics should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have ExecutionMetrics nested interface");
    }

    @Test
    @DisplayName("should have MemoryMetrics nested interface")
    void shouldHaveMemoryMetricsNestedInterface() {
      final var nestedClasses = ComponentMetrics.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("MemoryMetrics")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "MemoryMetrics should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have MemoryMetrics nested interface");
    }

    @Test
    @DisplayName("should have PerformanceMetrics nested interface")
    void shouldHavePerformanceMetricsNestedInterface() {
      final var nestedClasses = ComponentMetrics.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("PerformanceMetrics")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "PerformanceMetrics should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have PerformanceMetrics nested interface");
    }

    @Test
    @DisplayName("should have ResourceMetrics nested interface")
    void shouldHaveResourceMetricsNestedInterface() {
      final var nestedClasses = ComponentMetrics.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("ResourceMetrics")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "ResourceMetrics should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have ResourceMetrics nested interface");
    }

    @Test
    @DisplayName("should have ErrorMetrics nested interface")
    void shouldHaveErrorMetricsNestedInterface() {
      final var nestedClasses = ComponentMetrics.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("ErrorMetrics")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "ErrorMetrics should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have ErrorMetrics nested interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getComponentId method")
    void shouldHaveGetComponentIdMethod() throws NoSuchMethodException {
      final Method method = ComponentMetrics.class.getMethod("getComponentId");
      assertNotNull(method, "getComponentId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getExecutionMetrics method")
    void shouldHaveGetExecutionMetricsMethod() throws NoSuchMethodException {
      final Method method = ComponentMetrics.class.getMethod("getExecutionMetrics");
      assertNotNull(method, "getExecutionMetrics method should exist");
    }

    @Test
    @DisplayName("should have getMemoryMetrics method")
    void shouldHaveGetMemoryMetricsMethod() throws NoSuchMethodException {
      final Method method = ComponentMetrics.class.getMethod("getMemoryMetrics");
      assertNotNull(method, "getMemoryMetrics method should exist");
    }

    @Test
    @DisplayName("should have getPerformanceMetrics method")
    void shouldHaveGetPerformanceMetricsMethod() throws NoSuchMethodException {
      final Method method = ComponentMetrics.class.getMethod("getPerformanceMetrics");
      assertNotNull(method, "getPerformanceMetrics method should exist");
    }

    @Test
    @DisplayName("should have getResourceMetrics method")
    void shouldHaveGetResourceMetricsMethod() throws NoSuchMethodException {
      final Method method = ComponentMetrics.class.getMethod("getResourceMetrics");
      assertNotNull(method, "getResourceMetrics method should exist");
    }

    @Test
    @DisplayName("should have getErrorMetrics method")
    void shouldHaveGetErrorMetricsMethod() throws NoSuchMethodException {
      final Method method = ComponentMetrics.class.getMethod("getErrorMetrics");
      assertNotNull(method, "getErrorMetrics method should exist");
    }

    @Test
    @DisplayName("should have getStartTime method")
    void shouldHaveGetStartTimeMethod() throws NoSuchMethodException {
      final Method method = ComponentMetrics.class.getMethod("getStartTime");
      assertNotNull(method, "getStartTime method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getEndTime method")
    void shouldHaveGetEndTimeMethod() throws NoSuchMethodException {
      final Method method = ComponentMetrics.class.getMethod("getEndTime");
      assertNotNull(method, "getEndTime method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have reset method")
    void shouldHaveResetMethod() throws NoSuchMethodException {
      final Method method = ComponentMetrics.class.getMethod("reset");
      assertNotNull(method, "reset method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have snapshot method")
    void shouldHaveSnapshotMethod() throws NoSuchMethodException {
      final Method method = ComponentMetrics.class.getMethod("snapshot");
      assertNotNull(method, "snapshot method should exist");
    }
  }

  @Nested
  @DisplayName("ExportFormat Enum Tests")
  class ExportFormatEnumTests {

    @Test
    @DisplayName("should have all export formats")
    void shouldHaveAllExportFormats() {
      final var formats = ExportFormat.values();
      assertEquals(4, formats.length, "Should have 4 export formats");
    }

    @Test
    @DisplayName("should have JSON format")
    void shouldHaveJsonFormat() {
      assertEquals(ExportFormat.JSON, ExportFormat.valueOf("JSON"));
    }

    @Test
    @DisplayName("should have CSV format")
    void shouldHaveCsvFormat() {
      assertEquals(ExportFormat.CSV, ExportFormat.valueOf("CSV"));
    }

    @Test
    @DisplayName("should have BINARY format")
    void shouldHaveBinaryFormat() {
      assertEquals(ExportFormat.BINARY, ExportFormat.valueOf("BINARY"));
    }

    @Test
    @DisplayName("should have PROMETHEUS format")
    void shouldHavePrometheusFormat() {
      assertEquals(ExportFormat.PROMETHEUS, ExportFormat.valueOf("PROMETHEUS"));
    }
  }

  @Nested
  @DisplayName("Nested Interface Structure Tests")
  class NestedInterfaceStructureTests {

    @Test
    @DisplayName("should have all expected nested interfaces")
    void shouldHaveAllExpectedNestedInterfaces() {
      final var nestedClasses = ComponentMetrics.class.getDeclaredClasses();
      final var classNames =
          java.util.Arrays.stream(nestedClasses)
              .filter(Class::isInterface)
              .map(Class::getSimpleName)
              .collect(java.util.stream.Collectors.toSet());

      assertTrue(classNames.contains("ExecutionMetrics"), "Should have ExecutionMetrics");
      assertTrue(classNames.contains("MemoryMetrics"), "Should have MemoryMetrics");
      assertTrue(classNames.contains("PerformanceMetrics"), "Should have PerformanceMetrics");
      assertTrue(classNames.contains("ResourceMetrics"), "Should have ResourceMetrics");
      assertTrue(classNames.contains("ErrorMetrics"), "Should have ErrorMetrics");
      assertTrue(classNames.contains("QuotaUsageMetrics"), "Should have QuotaUsageMetrics");
      assertTrue(classNames.contains("ErrorInfo"), "Should have ErrorInfo");
      assertTrue(classNames.contains("MetricsSnapshot"), "Should have MetricsSnapshot");
    }

    @Test
    @DisplayName("should have ExportFormat enum")
    void shouldHaveExportFormatEnum() {
      final var nestedClasses = ComponentMetrics.class.getDeclaredClasses();
      final var enumNames =
          java.util.Arrays.stream(nestedClasses)
              .filter(Class::isEnum)
              .map(Class::getSimpleName)
              .collect(java.util.stream.Collectors.toSet());

      assertTrue(enumNames.contains("ExportFormat"), "Should have ExportFormat enum");
    }
  }

  @Nested
  @DisplayName("ExecutionMetrics Interface Tests")
  class ExecutionMetricsInterfaceTests {

    @Test
    @DisplayName("ExecutionMetrics should have required methods")
    void executionMetricsShouldHaveRequiredMethods() throws NoSuchMethodException {
      final Class<?> executionMetricsClass = ComponentMetrics.ExecutionMetrics.class;

      assertNotNull(
          executionMetricsClass.getMethod("getExecutionCount"), "Should have getExecutionCount");
      assertNotNull(
          executionMetricsClass.getMethod("getSuccessfulExecutions"),
          "Should have getSuccessfulExecutions");
      assertNotNull(
          executionMetricsClass.getMethod("getFailedExecutions"),
          "Should have getFailedExecutions");
      assertNotNull(
          executionMetricsClass.getMethod("getAverageExecutionTime"),
          "Should have getAverageExecutionTime");
      assertNotNull(
          executionMetricsClass.getMethod("getMinExecutionTime"),
          "Should have getMinExecutionTime");
      assertNotNull(
          executionMetricsClass.getMethod("getMaxExecutionTime"),
          "Should have getMaxExecutionTime");
      assertNotNull(
          executionMetricsClass.getMethod("getTotalExecutionTime"),
          "Should have getTotalExecutionTime");
      assertNotNull(
          executionMetricsClass.getMethod("getExecutionRate"), "Should have getExecutionRate");
    }
  }

  @Nested
  @DisplayName("MemoryMetrics Interface Tests")
  class MemoryMetricsInterfaceTests {

    @Test
    @DisplayName("MemoryMetrics should have required methods")
    void memoryMetricsShouldHaveRequiredMethods() throws NoSuchMethodException {
      final Class<?> memoryMetricsClass = ComponentMetrics.MemoryMetrics.class;

      assertNotNull(
          memoryMetricsClass.getMethod("getCurrentMemoryUsage"),
          "Should have getCurrentMemoryUsage");
      assertNotNull(
          memoryMetricsClass.getMethod("getPeakMemoryUsage"), "Should have getPeakMemoryUsage");
      assertNotNull(
          memoryMetricsClass.getMethod("getAverageMemoryUsage"),
          "Should have getAverageMemoryUsage");
      assertNotNull(
          memoryMetricsClass.getMethod("getTotalAllocations"), "Should have getTotalAllocations");
      assertNotNull(
          memoryMetricsClass.getMethod("getTotalAllocatedMemory"),
          "Should have getTotalAllocatedMemory");
      assertNotNull(
          memoryMetricsClass.getMethod("getAllocationRate"), "Should have getAllocationRate");
      assertNotNull(memoryMetricsClass.getMethod("getGcCount"), "Should have getGcCount");
      assertNotNull(memoryMetricsClass.getMethod("getGcTime"), "Should have getGcTime");
    }
  }

  @Nested
  @DisplayName("PerformanceMetrics Interface Tests")
  class PerformanceMetricsInterfaceTests {

    @Test
    @DisplayName("PerformanceMetrics should have required methods")
    void performanceMetricsShouldHaveRequiredMethods() throws NoSuchMethodException {
      final Class<?> performanceMetricsClass = ComponentMetrics.PerformanceMetrics.class;

      assertNotNull(
          performanceMetricsClass.getMethod("getInstructionsPerSecond"),
          "Should have getInstructionsPerSecond");
      assertNotNull(
          performanceMetricsClass.getMethod("getFunctionCallsPerSecond"),
          "Should have getFunctionCallsPerSecond");
      assertNotNull(
          performanceMetricsClass.getMethod("getThroughput"), "Should have getThroughput");
      assertNotNull(
          performanceMetricsClass.getMethod("getAverageLatency"), "Should have getAverageLatency");
      assertNotNull(
          performanceMetricsClass.getMethod("getP95Latency"), "Should have getP95Latency");
      assertNotNull(
          performanceMetricsClass.getMethod("getP99Latency"), "Should have getP99Latency");
      assertNotNull(
          performanceMetricsClass.getMethod("getCpuUtilization"), "Should have getCpuUtilization");
    }
  }

  @Nested
  @DisplayName("ResourceMetrics Interface Tests")
  class ResourceMetricsInterfaceTests {

    @Test
    @DisplayName("ResourceMetrics should have required methods")
    void resourceMetricsShouldHaveRequiredMethods() throws NoSuchMethodException {
      final Class<?> resourceMetricsClass = ComponentMetrics.ResourceMetrics.class;

      assertNotNull(
          resourceMetricsClass.getMethod("getFuelConsumed"), "Should have getFuelConsumed");
      assertNotNull(
          resourceMetricsClass.getMethod("getFuelConsumptionRate"),
          "Should have getFuelConsumptionRate");
      assertNotNull(resourceMetricsClass.getMethod("getThreadCount"), "Should have getThreadCount");
      assertNotNull(
          resourceMetricsClass.getMethod("getFileDescriptorCount"),
          "Should have getFileDescriptorCount");
      assertNotNull(
          resourceMetricsClass.getMethod("getNetworkConnectionCount"),
          "Should have getNetworkConnectionCount");
      assertNotNull(resourceMetricsClass.getMethod("getQuotaUsage"), "Should have getQuotaUsage");
    }
  }

  @Nested
  @DisplayName("ErrorMetrics Interface Tests")
  class ErrorMetricsInterfaceTests {

    @Test
    @DisplayName("ErrorMetrics should have required methods")
    void errorMetricsShouldHaveRequiredMethods() throws NoSuchMethodException {
      final Class<?> errorMetricsClass = ComponentMetrics.ErrorMetrics.class;

      assertNotNull(errorMetricsClass.getMethod("getTotalErrors"), "Should have getTotalErrors");
      assertNotNull(errorMetricsClass.getMethod("getErrorRate"), "Should have getErrorRate");
      assertNotNull(
          errorMetricsClass.getMethod("getErrorDistribution"), "Should have getErrorDistribution");
      assertNotNull(
          errorMetricsClass.getMethod("getMostCommonErrors", int.class),
          "Should have getMostCommonErrors");
      assertNotNull(
          errorMetricsClass.getMethod("getCriticalErrors"), "Should have getCriticalErrors");
      assertNotNull(
          errorMetricsClass.getMethod("getRecoverableErrors"), "Should have getRecoverableErrors");
    }
  }
}
