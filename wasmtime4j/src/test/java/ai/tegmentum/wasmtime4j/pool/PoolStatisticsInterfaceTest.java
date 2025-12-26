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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PoolStatistics} interface.
 *
 * <p>PoolStatistics provides statistics for monitoring pooling allocator usage.
 */
@DisplayName("PoolStatistics Interface Tests")
class PoolStatisticsInterfaceTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(PoolStatistics.class.isInterface(), "PoolStatistics should be an interface");
    }

    @Test
    @DisplayName("should have getInstancesAllocated method")
    void shouldHaveGetInstancesAllocatedMethod() throws NoSuchMethodException {
      final Method method = PoolStatistics.class.getMethod("getInstancesAllocated");
      assertNotNull(method, "getInstancesAllocated method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getInstancesReused method")
    void shouldHaveGetInstancesReusedMethod() throws NoSuchMethodException {
      final Method method = PoolStatistics.class.getMethod("getInstancesReused");
      assertNotNull(method, "getInstancesReused method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getInstancesCreated method")
    void shouldHaveGetInstancesCreatedMethod() throws NoSuchMethodException {
      final Method method = PoolStatistics.class.getMethod("getInstancesCreated");
      assertNotNull(method, "getInstancesCreated method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getMemoryPoolsAllocated method")
    void shouldHaveGetMemoryPoolsAllocatedMethod() throws NoSuchMethodException {
      final Method method = PoolStatistics.class.getMethod("getMemoryPoolsAllocated");
      assertNotNull(method, "getMemoryPoolsAllocated method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getMemoryPoolsReused method")
    void shouldHaveGetMemoryPoolsReusedMethod() throws NoSuchMethodException {
      final Method method = PoolStatistics.class.getMethod("getMemoryPoolsReused");
      assertNotNull(method, "getMemoryPoolsReused method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getStackPoolsAllocated method")
    void shouldHaveGetStackPoolsAllocatedMethod() throws NoSuchMethodException {
      final Method method = PoolStatistics.class.getMethod("getStackPoolsAllocated");
      assertNotNull(method, "getStackPoolsAllocated method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getStackPoolsReused method")
    void shouldHaveGetStackPoolsReusedMethod() throws NoSuchMethodException {
      final Method method = PoolStatistics.class.getMethod("getStackPoolsReused");
      assertNotNull(method, "getStackPoolsReused method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getTablePoolsAllocated method")
    void shouldHaveGetTablePoolsAllocatedMethod() throws NoSuchMethodException {
      final Method method = PoolStatistics.class.getMethod("getTablePoolsAllocated");
      assertNotNull(method, "getTablePoolsAllocated method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getTablePoolsReused method")
    void shouldHaveGetTablePoolsReusedMethod() throws NoSuchMethodException {
      final Method method = PoolStatistics.class.getMethod("getTablePoolsReused");
      assertNotNull(method, "getTablePoolsReused method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getPeakMemoryUsage method")
    void shouldHaveGetPeakMemoryUsageMethod() throws NoSuchMethodException {
      final Method method = PoolStatistics.class.getMethod("getPeakMemoryUsage");
      assertNotNull(method, "getPeakMemoryUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getCurrentMemoryUsage method")
    void shouldHaveGetCurrentMemoryUsageMethod() throws NoSuchMethodException {
      final Method method = PoolStatistics.class.getMethod("getCurrentMemoryUsage");
      assertNotNull(method, "getCurrentMemoryUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getAllocationFailures method")
    void shouldHaveGetAllocationFailuresMethod() throws NoSuchMethodException {
      final Method method = PoolStatistics.class.getMethod("getAllocationFailures");
      assertNotNull(method, "getAllocationFailures method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getPoolWarmingTime method")
    void shouldHaveGetPoolWarmingTimeMethod() throws NoSuchMethodException {
      final Method method = PoolStatistics.class.getMethod("getPoolWarmingTime");
      assertNotNull(method, "getPoolWarmingTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have getAverageAllocationTime method")
    void shouldHaveGetAverageAllocationTimeMethod() throws NoSuchMethodException {
      final Method method = PoolStatistics.class.getMethod("getAverageAllocationTime");
      assertNotNull(method, "getAverageAllocationTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }
  }

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("should have getReuseRatio default method")
    void shouldHaveGetReuseRatioDefaultMethod() throws NoSuchMethodException {
      final Method method = PoolStatistics.class.getMethod("getReuseRatio");
      assertNotNull(method, "getReuseRatio method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
      assertTrue(method.isDefault(), "getReuseRatio should be a default method");
    }

    @Test
    @DisplayName("should have getMemoryUtilization default method")
    void shouldHaveGetMemoryUtilizationDefaultMethod() throws NoSuchMethodException {
      final Method method = PoolStatistics.class.getMethod("getMemoryUtilization");
      assertNotNull(method, "getMemoryUtilization method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
      assertTrue(method.isDefault(), "getMemoryUtilization should be a default method");
    }
  }
}
