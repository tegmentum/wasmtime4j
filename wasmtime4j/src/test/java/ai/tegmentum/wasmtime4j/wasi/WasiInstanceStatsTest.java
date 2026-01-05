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

package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiInstanceStats} interface.
 *
 * <p>WasiInstanceStats provides comprehensive statistics for WASI instances.
 */
@DisplayName("WasiInstanceStats Tests")
class WasiInstanceStatsTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(WasiInstanceStats.class.getModifiers()),
          "WasiInstanceStats should be public");
      assertTrue(WasiInstanceStats.class.isInterface(), "WasiInstanceStats should be an interface");
    }
  }

  @Nested
  @DisplayName("Timestamp Method Tests")
  class TimestampMethodTests {

    @Test
    @DisplayName("should have getCollectedAt method")
    void shouldHaveGetCollectedAtMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getCollectedAt");
      assertNotNull(method, "getCollectedAt method should exist");
      assertEquals(Instant.class, method.getReturnType(), "Should return Instant");
    }

    @Test
    @DisplayName("should have getCreatedAt method")
    void shouldHaveGetCreatedAtMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getCreatedAt");
      assertNotNull(method, "getCreatedAt method should exist");
      assertEquals(Instant.class, method.getReturnType(), "Should return Instant");
    }

    @Test
    @DisplayName("should have getUptime method")
    void shouldHaveGetUptimeMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getUptime");
      assertNotNull(method, "getUptime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }
  }

  @Nested
  @DisplayName("Instance Identity Method Tests")
  class InstanceIdentityMethodTests {

    @Test
    @DisplayName("should have getInstanceId method")
    void shouldHaveGetInstanceIdMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getInstanceId");
      assertNotNull(method, "getInstanceId method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getState method")
    void shouldHaveGetStateMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getState");
      assertNotNull(method, "getState method should exist");
      assertEquals(
          WasiInstanceState.class, method.getReturnType(), "Should return WasiInstanceState");
    }
  }

  @Nested
  @DisplayName("Execution Stats Method Tests")
  class ExecutionStatsMethodTests {

    @Test
    @DisplayName("should have getExecutionTime method")
    void shouldHaveGetExecutionTimeMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getExecutionTime");
      assertNotNull(method, "getExecutionTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have getFunctionCallCount method")
    void shouldHaveGetFunctionCallCountMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getFunctionCallCount");
      assertNotNull(method, "getFunctionCallCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getFunctionCallStats method")
    void shouldHaveGetFunctionCallStatsMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getFunctionCallStats");
      assertNotNull(method, "getFunctionCallStats method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getFunctionExecutionTimeStats method")
    void shouldHaveGetFunctionExecutionTimeStatsMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getFunctionExecutionTimeStats");
      assertNotNull(method, "getFunctionExecutionTimeStats method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }
  }

  @Nested
  @DisplayName("Memory Stats Method Tests")
  class MemoryStatsMethodTests {

    @Test
    @DisplayName("should have getCurrentMemoryUsage method")
    void shouldHaveGetCurrentMemoryUsageMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getCurrentMemoryUsage");
      assertNotNull(method, "getCurrentMemoryUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getPeakMemoryUsage method")
    void shouldHaveGetPeakMemoryUsageMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getPeakMemoryUsage");
      assertNotNull(method, "getPeakMemoryUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getMemoryAllocationCount method")
    void shouldHaveGetMemoryAllocationCountMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getMemoryAllocationCount");
      assertNotNull(method, "getMemoryAllocationCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getTotalMemoryAllocated method")
    void shouldHaveGetTotalMemoryAllocatedMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getTotalMemoryAllocated");
      assertNotNull(method, "getTotalMemoryAllocated method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Resource Stats Method Tests")
  class ResourceStatsMethodTests {

    @Test
    @DisplayName("should have getCurrentResourceCount method")
    void shouldHaveGetCurrentResourceCountMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getCurrentResourceCount");
      assertNotNull(method, "getCurrentResourceCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getPeakResourceCount method")
    void shouldHaveGetPeakResourceCountMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getPeakResourceCount");
      assertNotNull(method, "getPeakResourceCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getTotalResourcesCreated method")
    void shouldHaveGetTotalResourcesCreatedMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getTotalResourcesCreated");
      assertNotNull(method, "getTotalResourcesCreated method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getResourceUsageByType method")
    void shouldHaveGetResourceUsageByTypeMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getResourceUsageByType");
      assertNotNull(method, "getResourceUsageByType method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }
  }

  @Nested
  @DisplayName("Error Stats Method Tests")
  class ErrorStatsMethodTests {

    @Test
    @DisplayName("should have getErrorCount method")
    void shouldHaveGetErrorCountMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getErrorCount");
      assertNotNull(method, "getErrorCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getErrorStats method")
    void shouldHaveGetErrorStatsMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getErrorStats");
      assertNotNull(method, "getErrorStats method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }
  }

  @Nested
  @DisplayName("Suspension Stats Method Tests")
  class SuspensionStatsMethodTests {

    @Test
    @DisplayName("should have getSuspensionCount method")
    void shouldHaveGetSuspensionCountMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getSuspensionCount");
      assertNotNull(method, "getSuspensionCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getTotalSuspensionTime method")
    void shouldHaveGetTotalSuspensionTimeMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getTotalSuspensionTime");
      assertNotNull(method, "getTotalSuspensionTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }
  }

  @Nested
  @DisplayName("Async Stats Method Tests")
  class AsyncStatsMethodTests {

    @Test
    @DisplayName("should have getAsyncOperationCount method")
    void shouldHaveGetAsyncOperationCountMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getAsyncOperationCount");
      assertNotNull(method, "getAsyncOperationCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getPendingAsyncOperationCount method")
    void shouldHaveGetPendingAsyncOperationCountMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getPendingAsyncOperationCount");
      assertNotNull(method, "getPendingAsyncOperationCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("IO Stats Method Tests")
  class IoStatsMethodTests {

    @Test
    @DisplayName("should have getFileSystemStats method")
    void shouldHaveGetFileSystemStatsMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getFileSystemStats");
      assertNotNull(method, "getFileSystemStats method should exist");
      assertEquals(
          WasiFileSystemStats.class, method.getReturnType(), "Should return WasiFileSystemStats");
    }

    @Test
    @DisplayName("should have getNetworkStats method")
    void shouldHaveGetNetworkStatsMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getNetworkStats");
      assertNotNull(method, "getNetworkStats method should exist");
      assertEquals(
          WasiNetworkStats.class, method.getReturnType(), "Should return WasiNetworkStats");
    }
  }

  @Nested
  @DisplayName("Performance Method Tests")
  class PerformanceMethodTests {

    @Test
    @DisplayName("should have getAverageExecutionTime method")
    void shouldHaveGetAverageExecutionTimeMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getAverageExecutionTime");
      assertNotNull(method, "getAverageExecutionTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have getThroughput method")
    void shouldHaveGetThroughputMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getThroughput");
      assertNotNull(method, "getThroughput method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }

    @Test
    @DisplayName("should have getMemoryEfficiency method")
    void shouldHaveGetMemoryEfficiencyMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getMemoryEfficiency");
      assertNotNull(method, "getMemoryEfficiency method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }
  }

  @Nested
  @DisplayName("Utility Method Tests")
  class UtilityMethodTests {

    @Test
    @DisplayName("should have getCustomProperties method")
    void shouldHaveGetCustomPropertiesMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getCustomProperties");
      assertNotNull(method, "getCustomProperties method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getSummary method")
    void shouldHaveGetSummaryMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("getSummary");
      assertNotNull(method, "getSummary method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have reset method")
    void shouldHaveResetMethod() throws NoSuchMethodException {
      final Method method = WasiInstanceStats.class.getMethod("reset");
      assertNotNull(method, "reset method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }
}
