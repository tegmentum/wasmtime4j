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

package ai.tegmentum.wasmtime4j.compilation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JitPerformanceMonitor} interface.
 *
 * <p>JitPerformanceMonitor provides JIT performance monitoring for WebAssembly components.
 */
@DisplayName("JitPerformanceMonitor Tests")
class JitPerformanceMonitorTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeInterface() {
      assertTrue(
          JitPerformanceMonitor.class.isInterface(),
          "JitPerformanceMonitor should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(JitPerformanceMonitor.class.getModifiers()),
          "JitPerformanceMonitor should be public");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have startMonitoring method")
    void shouldHaveStartMonitoringMethod() throws NoSuchMethodException {
      final Method method = JitPerformanceMonitor.class.getMethod("startMonitoring");
      assertNotNull(method, "startMonitoring method should exist");
      assertEquals(void.class, method.getReturnType(), "startMonitoring should return void");
    }

    @Test
    @DisplayName("should have stopMonitoring method")
    void shouldHaveStopMonitoringMethod() throws NoSuchMethodException {
      final Method method = JitPerformanceMonitor.class.getMethod("stopMonitoring");
      assertNotNull(method, "stopMonitoring method should exist");
      assertEquals(void.class, method.getReturnType(), "stopMonitoring should return void");
    }

    @Test
    @DisplayName("should have getCompilationStatistics method")
    void shouldHaveGetCompilationStatisticsMethod() throws NoSuchMethodException {
      final Method method = JitPerformanceMonitor.class.getMethod("getCompilationStatistics");
      assertNotNull(method, "getCompilationStatistics method should exist");
      assertEquals(
          JitPerformanceMonitor.CompilationStatistics.class,
          method.getReturnType(),
          "getCompilationStatistics should return CompilationStatistics");
    }

    @Test
    @DisplayName("should have getExecutionStatistics method")
    void shouldHaveGetExecutionStatisticsMethod() throws NoSuchMethodException {
      final Method method = JitPerformanceMonitor.class.getMethod("getExecutionStatistics");
      assertNotNull(method, "getExecutionStatistics method should exist");
      assertEquals(
          JitPerformanceMonitor.ExecutionStatistics.class,
          method.getReturnType(),
          "getExecutionStatistics should return ExecutionStatistics");
    }

    @Test
    @DisplayName("should have getPerformanceMetrics method")
    void shouldHaveGetPerformanceMetricsMethod() throws NoSuchMethodException {
      final Method method = JitPerformanceMonitor.class.getMethod("getPerformanceMetrics");
      assertNotNull(method, "getPerformanceMetrics method should exist");
      assertEquals(
          JitPerformanceMonitor.PerformanceMetrics.class,
          method.getReturnType(),
          "getPerformanceMetrics should return PerformanceMetrics");
    }

    @Test
    @DisplayName("should have resetStatistics method")
    void shouldHaveResetStatisticsMethod() throws NoSuchMethodException {
      final Method method = JitPerformanceMonitor.class.getMethod("resetStatistics");
      assertNotNull(method, "resetStatistics method should exist");
      assertEquals(void.class, method.getReturnType(), "resetStatistics should return void");
    }

    @Test
    @DisplayName("should have isMonitoring method")
    void shouldHaveIsMonitoringMethod() throws NoSuchMethodException {
      final Method method = JitPerformanceMonitor.class.getMethod("isMonitoring");
      assertNotNull(method, "isMonitoring method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isMonitoring should return boolean");
    }
  }

  @Nested
  @DisplayName("Nested Interface Tests")
  class NestedInterfaceTests {

    @Test
    @DisplayName("should have CompilationStatistics nested interface")
    void shouldHaveCompilationStatisticsInterface() {
      final Class<?>[] declaredClasses = JitPerformanceMonitor.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if ("CompilationStatistics".equals(clazz.getSimpleName()) && clazz.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have CompilationStatistics nested interface");
    }

    @Test
    @DisplayName("should have ExecutionStatistics nested interface")
    void shouldHaveExecutionStatisticsInterface() {
      final Class<?>[] declaredClasses = JitPerformanceMonitor.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if ("ExecutionStatistics".equals(clazz.getSimpleName()) && clazz.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have ExecutionStatistics nested interface");
    }

    @Test
    @DisplayName("should have PerformanceMetrics nested interface")
    void shouldHavePerformanceMetricsInterface() {
      final Class<?>[] declaredClasses = JitPerformanceMonitor.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if ("PerformanceMetrics".equals(clazz.getSimpleName()) && clazz.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have PerformanceMetrics nested interface");
    }

    @Test
    @DisplayName("should have TierStatistics nested interface")
    void shouldHaveTierStatisticsInterface() {
      final Class<?>[] declaredClasses = JitPerformanceMonitor.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if ("TierStatistics".equals(clazz.getSimpleName()) && clazz.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have TierStatistics nested interface");
    }

    @Test
    @DisplayName("should have FunctionCallStatistics nested interface")
    void shouldHaveFunctionCallStatisticsInterface() {
      final Class<?>[] declaredClasses = JitPerformanceMonitor.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if ("FunctionCallStatistics".equals(clazz.getSimpleName()) && clazz.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have FunctionCallStatistics nested interface");
    }
  }

  @Nested
  @DisplayName("CompilationStatistics Interface Tests")
  class CompilationStatisticsInterfaceTests {

    @Test
    @DisplayName("should have getTotalCompilationTime method")
    void shouldHaveGetTotalCompilationTimeMethod() throws NoSuchMethodException {
      final Method method =
          JitPerformanceMonitor.CompilationStatistics.class.getMethod("getTotalCompilationTime");
      assertNotNull(method, "getTotalCompilationTime method should exist");
      assertEquals(
          long.class, method.getReturnType(), "getTotalCompilationTime should return long");
    }

    @Test
    @DisplayName("should have getFunctionsCompiled method")
    void shouldHaveGetFunctionsCompiledMethod() throws NoSuchMethodException {
      final Method method =
          JitPerformanceMonitor.CompilationStatistics.class.getMethod("getFunctionsCompiled");
      assertNotNull(method, "getFunctionsCompiled method should exist");
      assertEquals(int.class, method.getReturnType(), "getFunctionsCompiled should return int");
    }

    @Test
    @DisplayName("should have getAverageCompilationTime method")
    void shouldHaveGetAverageCompilationTimeMethod() throws NoSuchMethodException {
      final Method method =
          JitPerformanceMonitor.CompilationStatistics.class.getMethod("getAverageCompilationTime");
      assertNotNull(method, "getAverageCompilationTime method should exist");
      assertEquals(
          double.class, method.getReturnType(), "getAverageCompilationTime should return double");
    }

    @Test
    @DisplayName("should have getTierStatistics method")
    void shouldHaveGetTierStatisticsMethod() throws NoSuchMethodException {
      final Method method =
          JitPerformanceMonitor.CompilationStatistics.class.getMethod("getTierStatistics");
      assertNotNull(method, "getTierStatistics method should exist");
      assertEquals(
          java.util.Map.class, method.getReturnType(), "getTierStatistics should return Map");
    }
  }

  @Nested
  @DisplayName("ExecutionStatistics Interface Tests")
  class ExecutionStatisticsInterfaceTests {

    @Test
    @DisplayName("should have getTotalExecutionTime method")
    void shouldHaveGetTotalExecutionTimeMethod() throws NoSuchMethodException {
      final Method method =
          JitPerformanceMonitor.ExecutionStatistics.class.getMethod("getTotalExecutionTime");
      assertNotNull(method, "getTotalExecutionTime method should exist");
      assertEquals(long.class, method.getReturnType(), "getTotalExecutionTime should return long");
    }

    @Test
    @DisplayName("should have getInstructionCount method")
    void shouldHaveGetInstructionCountMethod() throws NoSuchMethodException {
      final Method method =
          JitPerformanceMonitor.ExecutionStatistics.class.getMethod("getInstructionCount");
      assertNotNull(method, "getInstructionCount method should exist");
      assertEquals(long.class, method.getReturnType(), "getInstructionCount should return long");
    }

    @Test
    @DisplayName("should have getInstructionsPerSecond method")
    void shouldHaveGetInstructionsPerSecondMethod() throws NoSuchMethodException {
      final Method method =
          JitPerformanceMonitor.ExecutionStatistics.class.getMethod("getInstructionsPerSecond");
      assertNotNull(method, "getInstructionsPerSecond method should exist");
      assertEquals(
          double.class, method.getReturnType(), "getInstructionsPerSecond should return double");
    }
  }

  @Nested
  @DisplayName("PerformanceMetrics Interface Tests")
  class PerformanceMetricsInterfaceTests {

    @Test
    @DisplayName("should have getThroughput method")
    void shouldHaveGetThroughputMethod() throws NoSuchMethodException {
      final Method method =
          JitPerformanceMonitor.PerformanceMetrics.class.getMethod("getThroughput");
      assertNotNull(method, "getThroughput method should exist");
      assertEquals(double.class, method.getReturnType(), "getThroughput should return double");
    }

    @Test
    @DisplayName("should have getAverageLatency method")
    void shouldHaveGetAverageLatencyMethod() throws NoSuchMethodException {
      final Method method =
          JitPerformanceMonitor.PerformanceMetrics.class.getMethod("getAverageLatency");
      assertNotNull(method, "getAverageLatency method should exist");
      assertEquals(double.class, method.getReturnType(), "getAverageLatency should return double");
    }

    @Test
    @DisplayName("should have getP95Latency method")
    void shouldHaveGetP95LatencyMethod() throws NoSuchMethodException {
      final Method method =
          JitPerformanceMonitor.PerformanceMetrics.class.getMethod("getP95Latency");
      assertNotNull(method, "getP95Latency method should exist");
      assertEquals(double.class, method.getReturnType(), "getP95Latency should return double");
    }

    @Test
    @DisplayName("should have getCacheHitRatio method")
    void shouldHaveGetCacheHitRatioMethod() throws NoSuchMethodException {
      final Method method =
          JitPerformanceMonitor.PerformanceMetrics.class.getMethod("getCacheHitRatio");
      assertNotNull(method, "getCacheHitRatio method should exist");
      assertEquals(double.class, method.getReturnType(), "getCacheHitRatio should return double");
    }
  }

  @Nested
  @DisplayName("TierStatistics Interface Tests")
  class TierStatisticsInterfaceTests {

    @Test
    @DisplayName("should have getTierName method")
    void shouldHaveGetTierNameMethod() throws NoSuchMethodException {
      final Method method = JitPerformanceMonitor.TierStatistics.class.getMethod("getTierName");
      assertNotNull(method, "getTierName method should exist");
      assertEquals(String.class, method.getReturnType(), "getTierName should return String");
    }

    @Test
    @DisplayName("should have getCompilationCount method")
    void shouldHaveGetCompilationCountMethod() throws NoSuchMethodException {
      final Method method =
          JitPerformanceMonitor.TierStatistics.class.getMethod("getCompilationCount");
      assertNotNull(method, "getCompilationCount method should exist");
      assertEquals(int.class, method.getReturnType(), "getCompilationCount should return int");
    }

    @Test
    @DisplayName("should have getTotalTime method")
    void shouldHaveGetTotalTimeMethod() throws NoSuchMethodException {
      final Method method = JitPerformanceMonitor.TierStatistics.class.getMethod("getTotalTime");
      assertNotNull(method, "getTotalTime method should exist");
      assertEquals(long.class, method.getReturnType(), "getTotalTime should return long");
    }
  }

  @Nested
  @DisplayName("FunctionCallStatistics Interface Tests")
  class FunctionCallStatisticsInterfaceTests {

    @Test
    @DisplayName("should have getFunctionName method")
    void shouldHaveGetFunctionNameMethod() throws NoSuchMethodException {
      final Method method =
          JitPerformanceMonitor.FunctionCallStatistics.class.getMethod("getFunctionName");
      assertNotNull(method, "getFunctionName method should exist");
      assertEquals(String.class, method.getReturnType(), "getFunctionName should return String");
    }

    @Test
    @DisplayName("should have getCallCount method")
    void shouldHaveGetCallCountMethod() throws NoSuchMethodException {
      final Method method =
          JitPerformanceMonitor.FunctionCallStatistics.class.getMethod("getCallCount");
      assertNotNull(method, "getCallCount method should exist");
      assertEquals(long.class, method.getReturnType(), "getCallCount should return long");
    }

    @Test
    @DisplayName("should have getTotalExecutionTime method")
    void shouldHaveGetTotalExecutionTimeMethod() throws NoSuchMethodException {
      final Method method =
          JitPerformanceMonitor.FunctionCallStatistics.class.getMethod("getTotalExecutionTime");
      assertNotNull(method, "getTotalExecutionTime method should exist");
      assertEquals(long.class, method.getReturnType(), "getTotalExecutionTime should return long");
    }

    @Test
    @DisplayName("should have getAverageExecutionTime method")
    void shouldHaveGetAverageExecutionTimeMethod() throws NoSuchMethodException {
      final Method method =
          JitPerformanceMonitor.FunctionCallStatistics.class.getMethod("getAverageExecutionTime");
      assertNotNull(method, "getAverageExecutionTime method should exist");
      assertEquals(
          double.class, method.getReturnType(), "getAverageExecutionTime should return double");
    }
  }
}
