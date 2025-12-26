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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EngineStatistics} interface.
 *
 * <p>EngineStatistics provides comprehensive statistics about engine operations.
 */
@DisplayName("EngineStatistics Tests")
class EngineStatisticsTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeInterface() {
      assertTrue(EngineStatistics.class.isInterface(), "EngineStatistics should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(EngineStatistics.class.getModifiers()),
          "EngineStatistics should be public");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have capture static method")
    void shouldHaveCaptureStaticMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("capture", Engine.class);
      assertNotNull(method, "capture method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "capture should be static");
      assertEquals(
          EngineStatistics.class, method.getReturnType(), "capture should return EngineStatistics");
    }

    @Test
    @DisplayName("should have captureAndReset static method")
    void shouldHaveCaptureAndResetStaticMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("captureAndReset", Engine.class);
      assertNotNull(method, "captureAndReset method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "captureAndReset should be static");
      assertEquals(
          EngineStatistics.class,
          method.getReturnType(),
          "captureAndReset should return EngineStatistics");
    }
  }

  @Nested
  @DisplayName("Compilation Metric Method Tests")
  class CompilationMetricMethodTests {

    @Test
    @DisplayName("should have getModulesCompiled method")
    void shouldHaveGetModulesCompiledMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("getModulesCompiled");
      assertNotNull(method, "getModulesCompiled method should exist");
      assertEquals(long.class, method.getReturnType(), "getModulesCompiled should return long");
    }

    @Test
    @DisplayName("should have getTotalCompilationTime method")
    void shouldHaveGetTotalCompilationTimeMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("getTotalCompilationTime");
      assertNotNull(method, "getTotalCompilationTime method should exist");
      assertEquals(
          Duration.class, method.getReturnType(), "getTotalCompilationTime should return Duration");
    }

    @Test
    @DisplayName("should have getAverageCompilationTime method")
    void shouldHaveGetAverageCompilationTimeMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("getAverageCompilationTime");
      assertNotNull(method, "getAverageCompilationTime method should exist");
      assertEquals(
          Duration.class,
          method.getReturnType(),
          "getAverageCompilationTime should return Duration");
    }

    @Test
    @DisplayName("should have getBytesCompiled method")
    void shouldHaveGetBytesCompiledMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("getBytesCompiled");
      assertNotNull(method, "getBytesCompiled method should exist");
      assertEquals(long.class, method.getReturnType(), "getBytesCompiled should return long");
    }

    @Test
    @DisplayName("should have getCompilationThroughput method")
    void shouldHaveGetCompilationThroughputMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("getCompilationThroughput");
      assertNotNull(method, "getCompilationThroughput method should exist");
      assertEquals(
          double.class, method.getReturnType(), "getCompilationThroughput should return double");
    }
  }

  @Nested
  @DisplayName("Execution Metric Method Tests")
  class ExecutionMetricMethodTests {

    @Test
    @DisplayName("should have getFunctionsExecuted method")
    void shouldHaveGetFunctionsExecutedMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("getFunctionsExecuted");
      assertNotNull(method, "getFunctionsExecuted method should exist");
      assertEquals(long.class, method.getReturnType(), "getFunctionsExecuted should return long");
    }

    @Test
    @DisplayName("should have getTotalExecutionTime method")
    void shouldHaveGetTotalExecutionTimeMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("getTotalExecutionTime");
      assertNotNull(method, "getTotalExecutionTime method should exist");
      assertEquals(
          Duration.class, method.getReturnType(), "getTotalExecutionTime should return Duration");
    }

    @Test
    @DisplayName("should have getInstructionsExecuted method")
    void shouldHaveGetInstructionsExecutedMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("getInstructionsExecuted");
      assertNotNull(method, "getInstructionsExecuted method should exist");
      assertEquals(
          long.class, method.getReturnType(), "getInstructionsExecuted should return long");
    }

    @Test
    @DisplayName("should have getExecutionThroughput method")
    void shouldHaveGetExecutionThroughputMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("getExecutionThroughput");
      assertNotNull(method, "getExecutionThroughput method should exist");
      assertEquals(
          double.class, method.getReturnType(), "getExecutionThroughput should return double");
    }
  }

  @Nested
  @DisplayName("Memory Metric Method Tests")
  class MemoryMetricMethodTests {

    @Test
    @DisplayName("should have getPeakMemoryUsage method")
    void shouldHaveGetPeakMemoryUsageMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("getPeakMemoryUsage");
      assertNotNull(method, "getPeakMemoryUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "getPeakMemoryUsage should return long");
    }

    @Test
    @DisplayName("should have getCurrentMemoryUsage method")
    void shouldHaveGetCurrentMemoryUsageMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("getCurrentMemoryUsage");
      assertNotNull(method, "getCurrentMemoryUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "getCurrentMemoryUsage should return long");
    }

    @Test
    @DisplayName("should have getTotalAllocations method")
    void shouldHaveGetTotalAllocationsMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("getTotalAllocations");
      assertNotNull(method, "getTotalAllocations method should exist");
      assertEquals(long.class, method.getReturnType(), "getTotalAllocations should return long");
    }

    @Test
    @DisplayName("should have getTotalDeallocations method")
    void shouldHaveGetTotalDeallocationsMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("getTotalDeallocations");
      assertNotNull(method, "getTotalDeallocations method should exist");
      assertEquals(long.class, method.getReturnType(), "getTotalDeallocations should return long");
    }
  }

  @Nested
  @DisplayName("Cache Metric Method Tests")
  class CacheMetricMethodTests {

    @Test
    @DisplayName("should have getCacheHits method")
    void shouldHaveGetCacheHitsMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("getCacheHits");
      assertNotNull(method, "getCacheHits method should exist");
      assertEquals(long.class, method.getReturnType(), "getCacheHits should return long");
    }

    @Test
    @DisplayName("should have getCacheMisses method")
    void shouldHaveGetCacheMissesMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("getCacheMisses");
      assertNotNull(method, "getCacheMisses method should exist");
      assertEquals(long.class, method.getReturnType(), "getCacheMisses should return long");
    }

    @Test
    @DisplayName("should have getCacheHitRatio method")
    void shouldHaveGetCacheHitRatioMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("getCacheHitRatio");
      assertNotNull(method, "getCacheHitRatio method should exist");
      assertEquals(double.class, method.getReturnType(), "getCacheHitRatio should return double");
    }
  }

  @Nested
  @DisplayName("JIT Metric Method Tests")
  class JitMetricMethodTests {

    @Test
    @DisplayName("should have getJitCompilations method")
    void shouldHaveGetJitCompilationsMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("getJitCompilations");
      assertNotNull(method, "getJitCompilations method should exist");
      assertEquals(long.class, method.getReturnType(), "getJitCompilations should return long");
    }

    @Test
    @DisplayName("should have getJitCompilationTime method")
    void shouldHaveGetJitCompilationTimeMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("getJitCompilationTime");
      assertNotNull(method, "getJitCompilationTime method should exist");
      assertEquals(
          Duration.class, method.getReturnType(), "getJitCompilationTime should return Duration");
    }

    @Test
    @DisplayName("should have getJitCodeSize method")
    void shouldHaveGetJitCodeSizeMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("getJitCodeSize");
      assertNotNull(method, "getJitCodeSize method should exist");
      assertEquals(long.class, method.getReturnType(), "getJitCodeSize should return long");
    }
  }

  @Nested
  @DisplayName("Metadata Method Tests")
  class MetadataMethodTests {

    @Test
    @DisplayName("should have getCaptureTime method")
    void shouldHaveGetCaptureTimeMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("getCaptureTime");
      assertNotNull(method, "getCaptureTime method should exist");
      assertEquals(Instant.class, method.getReturnType(), "getCaptureTime should return Instant");
    }

    @Test
    @DisplayName("should have getUptime method")
    void shouldHaveGetUptimeMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("getUptime");
      assertNotNull(method, "getUptime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "getUptime should return Duration");
    }

    @Test
    @DisplayName("should have reset method")
    void shouldHaveResetMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("reset");
      assertNotNull(method, "reset method should exist");
      assertEquals(void.class, method.getReturnType(), "reset should return void");
    }

    @Test
    @DisplayName("should have getExtendedStatistics method")
    void shouldHaveGetExtendedStatisticsMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("getExtendedStatistics");
      assertNotNull(method, "getExtendedStatistics method should exist");
      assertEquals(Map.class, method.getReturnType(), "getExtendedStatistics should return Map");
    }
  }

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("should have hasHighCompilationOverhead default method")
    void shouldHaveHasHighCompilationOverheadMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("hasHighCompilationOverhead");
      assertNotNull(method, "hasHighCompilationOverhead method should exist");
      assertTrue(method.isDefault(), "hasHighCompilationOverhead should be a default method");
      assertEquals(
          boolean.class,
          method.getReturnType(),
          "hasHighCompilationOverhead should return boolean");
    }

    @Test
    @DisplayName("should have hasEfficientMemoryUtilization default method")
    void shouldHaveHasEfficientMemoryUtilizationMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("hasEfficientMemoryUtilization");
      assertNotNull(method, "hasEfficientMemoryUtilization method should exist");
      assertTrue(method.isDefault(), "hasEfficientMemoryUtilization should be a default method");
      assertEquals(
          boolean.class,
          method.getReturnType(),
          "hasEfficientMemoryUtilization should return boolean");
    }

    @Test
    @DisplayName("should have getPerformanceScore default method")
    void shouldHaveGetPerformanceScoreMethod() throws NoSuchMethodException {
      final Method method = EngineStatistics.class.getMethod("getPerformanceScore");
      assertNotNull(method, "getPerformanceScore method should exist");
      assertTrue(method.isDefault(), "getPerformanceScore should be a default method");
      assertEquals(
          double.class, method.getReturnType(), "getPerformanceScore should return double");
    }
  }
}
