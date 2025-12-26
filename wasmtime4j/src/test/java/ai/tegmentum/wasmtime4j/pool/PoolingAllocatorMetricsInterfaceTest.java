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

package ai.tegmentum.wasmtime4j.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PoolingAllocatorMetrics} interface.
 *
 * <p>PoolingAllocatorMetrics provides runtime metrics for the pooling allocator.
 */
@DisplayName("PoolingAllocatorMetrics Interface Tests")
class PoolingAllocatorMetricsInterfaceTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          PoolingAllocatorMetrics.class.isInterface(),
          "PoolingAllocatorMetrics should be an interface");
    }

    @Test
    @DisplayName("should have getActiveInstanceCount method")
    void shouldHaveGetActiveInstanceCountMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorMetrics.class.getMethod("getActiveInstanceCount");
      assertNotNull(method, "getActiveInstanceCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getTotalInstanceCount method")
    void shouldHaveGetTotalInstanceCountMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorMetrics.class.getMethod("getTotalInstanceCount");
      assertNotNull(method, "getTotalInstanceCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getPeakInstanceCount method")
    void shouldHaveGetPeakInstanceCountMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorMetrics.class.getMethod("getPeakInstanceCount");
      assertNotNull(method, "getPeakInstanceCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getTotalReservedMemory method")
    void shouldHaveGetTotalReservedMemoryMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorMetrics.class.getMethod("getTotalReservedMemory");
      assertNotNull(method, "getTotalReservedMemory method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getActiveMemoryUsage method")
    void shouldHaveGetActiveMemoryUsageMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorMetrics.class.getMethod("getActiveMemoryUsage");
      assertNotNull(method, "getActiveMemoryUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getPeakMemoryUsage method")
    void shouldHaveGetPeakMemoryUsageMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorMetrics.class.getMethod("getPeakMemoryUsage");
      assertNotNull(method, "getPeakMemoryUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getActiveStackCount method")
    void shouldHaveGetActiveStackCountMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorMetrics.class.getMethod("getActiveStackCount");
      assertNotNull(method, "getActiveStackCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getTotalStackCount method")
    void shouldHaveGetTotalStackCountMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorMetrics.class.getMethod("getTotalStackCount");
      assertNotNull(method, "getTotalStackCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getActiveTableCount method")
    void shouldHaveGetActiveTableCountMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorMetrics.class.getMethod("getActiveTableCount");
      assertNotNull(method, "getActiveTableCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getTotalTableCount method")
    void shouldHaveGetTotalTableCountMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorMetrics.class.getMethod("getTotalTableCount");
      assertNotNull(method, "getTotalTableCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getAllocationCount method")
    void shouldHaveGetAllocationCountMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorMetrics.class.getMethod("getAllocationCount");
      assertNotNull(method, "getAllocationCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getDeallocationCount method")
    void shouldHaveGetDeallocationCountMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorMetrics.class.getMethod("getDeallocationCount");
      assertNotNull(method, "getDeallocationCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getAllocationFailures method")
    void shouldHaveGetAllocationFailuresMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorMetrics.class.getMethod("getAllocationFailures");
      assertNotNull(method, "getAllocationFailures method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getAverageAllocationLatency method")
    void shouldHaveGetAverageAllocationLatencyMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorMetrics.class.getMethod("getAverageAllocationLatency");
      assertNotNull(method, "getAverageAllocationLatency method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have getMaxAllocationLatency method")
    void shouldHaveGetMaxAllocationLatencyMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorMetrics.class.getMethod("getMaxAllocationLatency");
      assertNotNull(method, "getMaxAllocationLatency method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have getCreationTime method")
    void shouldHaveGetCreationTimeMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorMetrics.class.getMethod("getCreationTime");
      assertNotNull(method, "getCreationTime method should exist");
      assertEquals(Instant.class, method.getReturnType(), "Should return Instant");
    }

    @Test
    @DisplayName("should have snapshot method")
    void shouldHaveSnapshotMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorMetrics.class.getMethod("snapshot");
      assertNotNull(method, "snapshot method should exist");
      assertEquals(
          PoolingAllocatorMetrics.MetricsSnapshot.class,
          method.getReturnType(),
          "Should return MetricsSnapshot");
    }

    @Test
    @DisplayName("should have resetStatistics method")
    void shouldHaveResetStatisticsMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorMetrics.class.getMethod("resetStatistics");
      assertNotNull(method, "resetStatistics method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("should have getAvailableInstanceCount default method")
    void shouldHaveGetAvailableInstanceCountDefaultMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorMetrics.class.getMethod("getAvailableInstanceCount");
      assertNotNull(method, "getAvailableInstanceCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
      assertTrue(method.isDefault(), "getAvailableInstanceCount should be a default method");
    }

    @Test
    @DisplayName("should have getInstanceUtilization default method")
    void shouldHaveGetInstanceUtilizationDefaultMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorMetrics.class.getMethod("getInstanceUtilization");
      assertNotNull(method, "getInstanceUtilization method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
      assertTrue(method.isDefault(), "getInstanceUtilization should be a default method");
    }

    @Test
    @DisplayName("should have getMemoryUtilization default method")
    void shouldHaveGetMemoryUtilizationDefaultMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorMetrics.class.getMethod("getMemoryUtilization");
      assertNotNull(method, "getMemoryUtilization method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
      assertTrue(method.isDefault(), "getMemoryUtilization should be a default method");
    }

    @Test
    @DisplayName("should have getStackUtilization default method")
    void shouldHaveGetStackUtilizationDefaultMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorMetrics.class.getMethod("getStackUtilization");
      assertNotNull(method, "getStackUtilization method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
      assertTrue(method.isDefault(), "getStackUtilization should be a default method");
    }

    @Test
    @DisplayName("should have getUptime default method")
    void shouldHaveGetUptimeDefaultMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorMetrics.class.getMethod("getUptime");
      assertNotNull(method, "getUptime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
      assertTrue(method.isDefault(), "getUptime should be a default method");
    }
  }

  @Nested
  @DisplayName("MetricsSnapshot Nested Interface Tests")
  class MetricsSnapshotNestedInterfaceTests {

    @Test
    @DisplayName("MetricsSnapshot should be a nested interface")
    void metricsSnapshotShouldBeNestedInterface() {
      assertTrue(
          PoolingAllocatorMetrics.MetricsSnapshot.class.isInterface(),
          "MetricsSnapshot should be an interface");
    }

    @Test
    @DisplayName("MetricsSnapshot should have getTimestamp method")
    void shouldHaveGetTimestampMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorMetrics.MetricsSnapshot.class.getMethod("getTimestamp");
      assertNotNull(method, "getTimestamp method should exist");
      assertEquals(Instant.class, method.getReturnType(), "Should return Instant");
    }

    @Test
    @DisplayName("MetricsSnapshot should have getActiveInstanceCount method")
    void shouldHaveSnapshotGetActiveInstanceCountMethod() throws NoSuchMethodException {
      final Method method =
          PoolingAllocatorMetrics.MetricsSnapshot.class.getMethod("getActiveInstanceCount");
      assertNotNull(method, "getActiveInstanceCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("MetricsSnapshot should have getTotalInstanceCount method")
    void shouldHaveSnapshotGetTotalInstanceCountMethod() throws NoSuchMethodException {
      final Method method =
          PoolingAllocatorMetrics.MetricsSnapshot.class.getMethod("getTotalInstanceCount");
      assertNotNull(method, "getTotalInstanceCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("MetricsSnapshot should have getAllocationCount method")
    void shouldHaveSnapshotGetAllocationCountMethod() throws NoSuchMethodException {
      final Method method =
          PoolingAllocatorMetrics.MetricsSnapshot.class.getMethod("getAllocationCount");
      assertNotNull(method, "getAllocationCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }
}
