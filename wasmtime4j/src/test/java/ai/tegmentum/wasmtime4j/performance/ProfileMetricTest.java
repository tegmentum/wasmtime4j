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

package ai.tegmentum.wasmtime4j.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ProfileMetric} enum.
 *
 * <p>ProfileMetric defines types of performance metrics that can be monitored.
 */
@DisplayName("ProfileMetric Tests")
class ProfileMetricTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeEnum() {
      assertTrue(ProfileMetric.class.isEnum(), "ProfileMetric should be an enum");
    }

    @Test
    @DisplayName("should have CPU_USAGE constant")
    void shouldHaveCpuUsageConstant() {
      assertNotNull(ProfileMetric.CPU_USAGE, "CPU_USAGE constant should exist");
    }

    @Test
    @DisplayName("should have MEMORY_USAGE constant")
    void shouldHaveMemoryUsageConstant() {
      assertNotNull(ProfileMetric.MEMORY_USAGE, "MEMORY_USAGE constant should exist");
    }

    @Test
    @DisplayName("should have FUNCTION_CALLS constant")
    void shouldHaveFunctionCallsConstant() {
      assertNotNull(ProfileMetric.FUNCTION_CALLS, "FUNCTION_CALLS constant should exist");
    }

    @Test
    @DisplayName("should have COMPILATION_TIME constant")
    void shouldHaveCompilationTimeConstant() {
      assertNotNull(ProfileMetric.COMPILATION_TIME, "COMPILATION_TIME constant should exist");
    }

    @Test
    @DisplayName("should have GC_ACTIVITY constant")
    void shouldHaveGcActivityConstant() {
      assertNotNull(ProfileMetric.GC_ACTIVITY, "GC_ACTIVITY constant should exist");
    }

    @Test
    @DisplayName("should have I_O_OPERATIONS constant")
    void shouldHaveIoOperationsConstant() {
      assertNotNull(ProfileMetric.I_O_OPERATIONS, "I_O_OPERATIONS constant should exist");
    }

    @Test
    @DisplayName("should have THREAD_ACTIVITY constant")
    void shouldHaveThreadActivityConstant() {
      assertNotNull(ProfileMetric.THREAD_ACTIVITY, "THREAD_ACTIVITY constant should exist");
    }

    @Test
    @DisplayName("should have CACHE_PERFORMANCE constant")
    void shouldHaveCachePerformanceConstant() {
      assertNotNull(ProfileMetric.CACHE_PERFORMANCE, "CACHE_PERFORMANCE constant should exist");
    }

    @Test
    @DisplayName("should have JIT_ACTIVITY constant")
    void shouldHaveJitActivityConstant() {
      assertNotNull(ProfileMetric.JIT_ACTIVITY, "JIT_ACTIVITY constant should exist");
    }

    @Test
    @DisplayName("should have 9 metric types")
    void shouldHave9MetricTypes() {
      assertEquals(9, ProfileMetric.values().length, "Should have 9 metric types");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getDisplayName method")
    void shouldHaveGetDisplayNameMethod() throws NoSuchMethodException {
      final Method method = ProfileMetric.class.getMethod("getDisplayName");
      assertNotNull(method, "getDisplayName method should exist");
      assertEquals(String.class, method.getReturnType(), "getDisplayName should return String");
    }

    @Test
    @DisplayName("should have getDescription method")
    void shouldHaveGetDescriptionMethod() throws NoSuchMethodException {
      final Method method = ProfileMetric.class.getMethod("getDescription");
      assertNotNull(method, "getDescription method should exist");
      assertEquals(String.class, method.getReturnType(), "getDescription should return String");
    }

    @Test
    @DisplayName("should have getOverheadFactor method")
    void shouldHaveGetOverheadFactorMethod() throws NoSuchMethodException {
      final Method method = ProfileMetric.class.getMethod("getOverheadFactor");
      assertNotNull(method, "getOverheadFactor method should exist");
      assertEquals(double.class, method.getReturnType(), "getOverheadFactor should return double");
    }
  }

  @Nested
  @DisplayName("Classification Method Tests")
  class ClassificationMethodTests {

    @Test
    @DisplayName("should have isLowOverhead method")
    void shouldHaveIsLowOverheadMethod() throws NoSuchMethodException {
      final Method method = ProfileMetric.class.getMethod("isLowOverhead");
      assertNotNull(method, "isLowOverhead method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isLowOverhead should return boolean");
    }

    @Test
    @DisplayName("should have isHighOverhead method")
    void shouldHaveIsHighOverheadMethod() throws NoSuchMethodException {
      final Method method = ProfileMetric.class.getMethod("isHighOverhead");
      assertNotNull(method, "isHighOverhead method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isHighOverhead should return boolean");
    }

    @Test
    @DisplayName("should have isResourceMetric method")
    void shouldHaveIsResourceMetricMethod() throws NoSuchMethodException {
      final Method method = ProfileMetric.class.getMethod("isResourceMetric");
      assertNotNull(method, "isResourceMetric method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isResourceMetric should return boolean");
    }

    @Test
    @DisplayName("should have isCompilationMetric method")
    void shouldHaveIsCompilationMetricMethod() throws NoSuchMethodException {
      final Method method = ProfileMetric.class.getMethod("isCompilationMetric");
      assertNotNull(method, "isCompilationMetric method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "isCompilationMetric should return boolean");
    }

    @Test
    @DisplayName("should have requiresFrequentSampling method")
    void shouldHaveRequiresFrequentSamplingMethod() throws NoSuchMethodException {
      final Method method = ProfileMetric.class.getMethod("requiresFrequentSampling");
      assertNotNull(method, "requiresFrequentSampling method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "requiresFrequentSampling should return boolean");
    }
  }

  @Nested
  @DisplayName("Overhead Classification Tests")
  class OverheadClassificationTests {

    @Test
    @DisplayName("CACHE_PERFORMANCE should be low overhead")
    void cachePerformanceShouldBeLowOverhead() {
      assertTrue(
          ProfileMetric.CACHE_PERFORMANCE.isLowOverhead(),
          "CACHE_PERFORMANCE should have low overhead");
    }

    @Test
    @DisplayName("FUNCTION_CALLS should have higher overhead")
    void functionCallsShouldHaveHigherOverhead() {
      assertFalse(
          ProfileMetric.FUNCTION_CALLS.isLowOverhead(),
          "FUNCTION_CALLS should not have low overhead");
    }
  }

  @Nested
  @DisplayName("Metric Classification Tests")
  class MetricClassificationTests {

    @Test
    @DisplayName("CPU_USAGE should be a resource metric")
    void cpuUsageShouldBeResourceMetric() {
      assertTrue(
          ProfileMetric.CPU_USAGE.isResourceMetric(), "CPU_USAGE should be a resource metric");
    }

    @Test
    @DisplayName("COMPILATION_TIME should be a compilation metric")
    void compilationTimeShouldBeCompilationMetric() {
      assertTrue(
          ProfileMetric.COMPILATION_TIME.isCompilationMetric(),
          "COMPILATION_TIME should be a compilation metric");
    }

    @Test
    @DisplayName("FUNCTION_CALLS should require frequent sampling")
    void functionCallsShouldRequireFrequentSampling() {
      assertTrue(
          ProfileMetric.FUNCTION_CALLS.requiresFrequentSampling(),
          "FUNCTION_CALLS should require frequent sampling");
    }
  }

  @Nested
  @DisplayName("Display Name Tests")
  class DisplayNameTests {

    @Test
    @DisplayName("CPU_USAGE should have correct display name")
    void cpuUsageShouldHaveCorrectDisplayName() {
      assertEquals(
          "CPU Usage", ProfileMetric.CPU_USAGE.getDisplayName(), "Display name should match");
    }

    @Test
    @DisplayName("MEMORY_USAGE should have correct display name")
    void memoryUsageShouldHaveCorrectDisplayName() {
      assertEquals(
          "Memory Usage", ProfileMetric.MEMORY_USAGE.getDisplayName(), "Display name should match");
    }
  }
}
