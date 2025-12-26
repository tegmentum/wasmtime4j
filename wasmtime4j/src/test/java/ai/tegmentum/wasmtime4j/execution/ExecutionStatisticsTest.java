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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the ExecutionStatistics interface.
 *
 * <p>This test class verifies the interface structure, methods, and nested interfaces for
 * ExecutionStatistics using reflection-based testing.
 */
@DisplayName("ExecutionStatistics Tests")
class ExecutionStatisticsTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("ExecutionStatistics should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          ExecutionStatistics.class.isInterface(), "ExecutionStatistics should be an interface");
    }

    @Test
    @DisplayName("ExecutionStatistics should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExecutionStatistics.class.getModifiers()),
          "ExecutionStatistics should be public");
    }

    @Test
    @DisplayName("ExecutionStatistics should not extend other interfaces")
    void shouldNotExtendOtherInterfaces() {
      Class<?>[] interfaces = ExecutionStatistics.class.getInterfaces();
      assertEquals(0, interfaces.length, "ExecutionStatistics should not extend other interfaces");
    }
  }

  // ========================================================================
  // Main Interface Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getExecutionTime method")
    void shouldHaveGetExecutionTimeMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.class.getMethod("getExecutionTime");
      assertNotNull(method, "getExecutionTime method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getInstructionCount method")
    void shouldHaveGetInstructionCountMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.class.getMethod("getInstructionCount");
      assertNotNull(method, "getInstructionCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getFunctionCallCount method")
    void shouldHaveGetFunctionCallCountMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.class.getMethod("getFunctionCallCount");
      assertNotNull(method, "getFunctionCallCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getMemoryAllocations method")
    void shouldHaveGetMemoryAllocationsMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.class.getMethod("getMemoryAllocations");
      assertNotNull(method, "getMemoryAllocations method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getPeakMemoryUsage method")
    void shouldHaveGetPeakMemoryUsageMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.class.getMethod("getPeakMemoryUsage");
      assertNotNull(method, "getPeakMemoryUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getCurrentMemoryUsage method")
    void shouldHaveGetCurrentMemoryUsageMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.class.getMethod("getCurrentMemoryUsage");
      assertNotNull(method, "getCurrentMemoryUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getInstructionsPerSecond method")
    void shouldHaveGetInstructionsPerSecondMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.class.getMethod("getInstructionsPerSecond");
      assertNotNull(method, "getInstructionsPerSecond method should exist");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }

    @Test
    @DisplayName("should have getFunctionCallsPerSecond method")
    void shouldHaveGetFunctionCallsPerSecondMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.class.getMethod("getFunctionCallsPerSecond");
      assertNotNull(method, "getFunctionCallsPerSecond method should exist");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }

    @Test
    @DisplayName("should have getCpuUsage method")
    void shouldHaveGetCpuUsageMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.class.getMethod("getCpuUsage");
      assertNotNull(method, "getCpuUsage method should exist");
      assertEquals(
          ExecutionStatistics.CpuUsage.class,
          method.getReturnType(),
          "Return type should be CpuUsage");
    }

    @Test
    @DisplayName("should have getMemoryUsage method")
    void shouldHaveGetMemoryUsageMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.class.getMethod("getMemoryUsage");
      assertNotNull(method, "getMemoryUsage method should exist");
      assertEquals(
          ExecutionStatistics.MemoryUsage.class,
          method.getReturnType(),
          "Return type should be MemoryUsage");
    }

    @Test
    @DisplayName("should have getPerformanceMetrics method")
    void shouldHaveGetPerformanceMetricsMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.class.getMethod("getPerformanceMetrics");
      assertNotNull(method, "getPerformanceMetrics method should exist");
      assertEquals(
          ExecutionStatistics.PerformanceMetrics.class,
          method.getReturnType(),
          "Return type should be PerformanceMetrics");
    }
  }

  // ========================================================================
  // CpuUsage Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("CpuUsage Interface Tests")
  class CpuUsageTests {

    @Test
    @DisplayName("CpuUsage should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          ExecutionStatistics.CpuUsage.class.isInterface(), "CpuUsage should be an interface");
      assertTrue(
          ExecutionStatistics.CpuUsage.class.isMemberClass(), "CpuUsage should be a member class");
    }

    @Test
    @DisplayName("CpuUsage should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExecutionStatistics.CpuUsage.class.getModifiers()),
          "CpuUsage should be public");
    }

    @Test
    @DisplayName("CpuUsage should have getTotalCpuTime method")
    void shouldHaveGetTotalCpuTimeMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.CpuUsage.class.getMethod("getTotalCpuTime");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("CpuUsage should have getUserCpuTime method")
    void shouldHaveGetUserCpuTimeMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.CpuUsage.class.getMethod("getUserCpuTime");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("CpuUsage should have getSystemCpuTime method")
    void shouldHaveGetSystemCpuTimeMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.CpuUsage.class.getMethod("getSystemCpuTime");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("CpuUsage should have getCpuUsagePercentage method")
    void shouldHaveGetCpuUsagePercentageMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.CpuUsage.class.getMethod("getCpuUsagePercentage");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }
  }

  // ========================================================================
  // MemoryUsage Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("MemoryUsage Interface Tests")
  class MemoryUsageTests {

    @Test
    @DisplayName("MemoryUsage should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          ExecutionStatistics.MemoryUsage.class.isInterface(),
          "MemoryUsage should be an interface");
      assertTrue(
          ExecutionStatistics.MemoryUsage.class.isMemberClass(),
          "MemoryUsage should be a member class");
    }

    @Test
    @DisplayName("MemoryUsage should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExecutionStatistics.MemoryUsage.class.getModifiers()),
          "MemoryUsage should be public");
    }

    @Test
    @DisplayName("MemoryUsage should have getHeapUsage method")
    void shouldHaveGetHeapUsageMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.MemoryUsage.class.getMethod("getHeapUsage");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("MemoryUsage should have getStackUsage method")
    void shouldHaveGetStackUsageMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.MemoryUsage.class.getMethod("getStackUsage");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("MemoryUsage should have getTotalAllocated method")
    void shouldHaveGetTotalAllocatedMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.MemoryUsage.class.getMethod("getTotalAllocated");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("MemoryUsage should have getTotalFreed method")
    void shouldHaveGetTotalFreedMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.MemoryUsage.class.getMethod("getTotalFreed");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("MemoryUsage should have getGcCount method")
    void shouldHaveGetGcCountMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.MemoryUsage.class.getMethod("getGcCount");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("MemoryUsage should have getGcTime method")
    void shouldHaveGetGcTimeMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.MemoryUsage.class.getMethod("getGcTime");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }
  }

  // ========================================================================
  // PerformanceMetrics Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("PerformanceMetrics Interface Tests")
  class PerformanceMetricsTests {

    @Test
    @DisplayName("PerformanceMetrics should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          ExecutionStatistics.PerformanceMetrics.class.isInterface(),
          "PerformanceMetrics should be an interface");
      assertTrue(
          ExecutionStatistics.PerformanceMetrics.class.isMemberClass(),
          "PerformanceMetrics should be a member class");
    }

    @Test
    @DisplayName("PerformanceMetrics should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExecutionStatistics.PerformanceMetrics.class.getModifiers()),
          "PerformanceMetrics should be public");
    }

    @Test
    @DisplayName("PerformanceMetrics should have getThroughput method")
    void shouldHaveGetThroughputMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.PerformanceMetrics.class.getMethod("getThroughput");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }

    @Test
    @DisplayName("PerformanceMetrics should have getAverageLatency method")
    void shouldHaveGetAverageLatencyMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.PerformanceMetrics.class.getMethod("getAverageLatency");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }

    @Test
    @DisplayName("PerformanceMetrics should have getP95Latency method")
    void shouldHaveGetP95LatencyMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.PerformanceMetrics.class.getMethod("getP95Latency");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }

    @Test
    @DisplayName("PerformanceMetrics should have getP99Latency method")
    void shouldHaveGetP99LatencyMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.PerformanceMetrics.class.getMethod("getP99Latency");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }
  }

  // ========================================================================
  // Nested Interface Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Interface Count Tests")
  class NestedInterfaceCountTests {

    @Test
    @DisplayName("ExecutionStatistics should have 3 nested interfaces")
    void shouldHaveThreeNestedInterfaces() {
      Class<?>[] nestedClasses = ExecutionStatistics.class.getDeclaredClasses();
      int interfaceCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isInterface() && !nested.isAnnotation()) {
          interfaceCount++;
        }
      }
      assertEquals(3, interfaceCount, "ExecutionStatistics should have 3 nested interfaces");
    }

    @Test
    @DisplayName("All nested interfaces should be public")
    void allNestedInterfacesShouldBePublic() {
      Class<?>[] nestedClasses = ExecutionStatistics.class.getDeclaredClasses();
      for (Class<?> nested : nestedClasses) {
        if (nested.isInterface() && !nested.isAnnotation()) {
          assertTrue(
              Modifier.isPublic(nested.getModifiers()),
              nested.getSimpleName() + " should be public");
        }
      }
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("ExecutionStatistics should have at least 11 methods")
    void shouldHaveAtLeastElevenMethods() {
      Method[] methods = ExecutionStatistics.class.getDeclaredMethods();
      assertTrue(
          methods.length >= 11,
          "ExecutionStatistics should have at least 11 methods, found: " + methods.length);
    }

    @Test
    @DisplayName("All methods should be public and abstract")
    void allMethodsShouldBePublicAbstract() {
      Method[] methods = ExecutionStatistics.class.getDeclaredMethods();
      for (Method method : methods) {
        int modifiers = method.getModifiers();
        assertTrue(
            Modifier.isPublic(modifiers) && Modifier.isAbstract(modifiers),
            method.getName() + " should be public and abstract");
      }
    }
  }
}
