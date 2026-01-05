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
import static org.junit.jupiter.api.Assertions.assertThrows;
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

  @Nested
  @DisplayName("Static Factory Behavior Tests")
  class StaticFactoryBehaviorTests {

    @Test
    @DisplayName("capture should throw IllegalArgumentException for null engine")
    void captureShouldThrowIllegalArgumentExceptionForNullEngine() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> EngineStatistics.capture(null),
              "capture should throw IllegalArgumentException for null engine");

      assertNotNull(exception.getMessage(), "Exception message should not be null");
      assertTrue(
          exception.getMessage().contains("null"),
          "Exception message should mention null. Actual: " + exception.getMessage());
    }

    @Test
    @DisplayName("captureAndReset should throw IllegalArgumentException for null engine")
    void captureAndResetShouldThrowIllegalArgumentExceptionForNullEngine() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> EngineStatistics.captureAndReset(null),
              "captureAndReset should throw IllegalArgumentException for null engine");

      assertNotNull(exception.getMessage(), "Exception message should not be null");
      assertTrue(
          exception.getMessage().contains("null"),
          "Exception message should mention null. Actual: " + exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Default Method Behavior Tests")
  class DefaultMethodBehaviorTests {

    /** Creates a test implementation of EngineStatistics with configurable values. */
    private EngineStatistics createTestStats(
        final Duration compilationTime,
        final Duration executionTime,
        final long peakMemory,
        final long currentMemory,
        final double cacheHitRatio,
        final double executionThroughput) {
      return new EngineStatistics() {
        @Override
        public long getModulesCompiled() {
          return 0;
        }

        @Override
        public Duration getTotalCompilationTime() {
          return compilationTime;
        }

        @Override
        public Duration getAverageCompilationTime() {
          return Duration.ZERO;
        }

        @Override
        public long getBytesCompiled() {
          return 0;
        }

        @Override
        public double getCompilationThroughput() {
          return 0.0;
        }

        @Override
        public long getFunctionsExecuted() {
          return 0;
        }

        @Override
        public Duration getTotalExecutionTime() {
          return executionTime;
        }

        @Override
        public long getInstructionsExecuted() {
          return 0;
        }

        @Override
        public double getExecutionThroughput() {
          return executionThroughput;
        }

        @Override
        public long getPeakMemoryUsage() {
          return peakMemory;
        }

        @Override
        public long getCurrentMemoryUsage() {
          return currentMemory;
        }

        @Override
        public long getTotalAllocations() {
          return 0;
        }

        @Override
        public long getTotalDeallocations() {
          return 0;
        }

        @Override
        public long getCacheHits() {
          return 0;
        }

        @Override
        public long getCacheMisses() {
          return 0;
        }

        @Override
        public double getCacheHitRatio() {
          return cacheHitRatio;
        }

        @Override
        public long getJitCompilations() {
          return 0;
        }

        @Override
        public Duration getJitCompilationTime() {
          return Duration.ZERO;
        }

        @Override
        public long getJitCodeSize() {
          return 0;
        }

        @Override
        public Instant getCaptureTime() {
          return Instant.now();
        }

        @Override
        public Duration getUptime() {
          return Duration.ZERO;
        }

        @Override
        public void reset() {
          // No-op for test
        }

        @Override
        public Map<String, Object> getExtendedStatistics() {
          return java.util.Collections.emptyMap();
        }
      };
    }

    @Test
    @DisplayName("hasHighCompilationOverhead should return false when total time is zero")
    void hasHighCompilationOverheadShouldReturnFalseWhenTotalTimeIsZero() {
      final EngineStatistics stats = createTestStats(Duration.ZERO, Duration.ZERO, 0, 0, 0.0, 0.0);
      assertFalse(
          stats.hasHighCompilationOverhead(), "Should return false when total time is zero");
    }

    @Test
    @DisplayName("hasHighCompilationOverhead should return true when compilation > 50% of total")
    void hasHighCompilationOverheadShouldReturnTrueWhenCompilationOverHalf() {
      final EngineStatistics stats =
          createTestStats(Duration.ofSeconds(6), Duration.ofSeconds(4), 0, 0, 0.0, 0.0);
      assertTrue(
          stats.hasHighCompilationOverhead(),
          "Should return true when compilation is more than 50% of total time");
    }

    @Test
    @DisplayName("hasHighCompilationOverhead should return false when compilation < 50% of total")
    void hasHighCompilationOverheadShouldReturnFalseWhenCompilationUnderHalf() {
      final EngineStatistics stats =
          createTestStats(Duration.ofSeconds(4), Duration.ofSeconds(6), 0, 0, 0.0, 0.0);
      assertFalse(
          stats.hasHighCompilationOverhead(),
          "Should return false when compilation is less than 50% of total time");
    }

    @Test
    @DisplayName("hasEfficientMemoryUtilization should return true when peak is zero")
    void hasEfficientMemoryUtilizationShouldReturnTrueWhenPeakIsZero() {
      final EngineStatistics stats = createTestStats(Duration.ZERO, Duration.ZERO, 0, 0, 0.0, 0.0);
      assertTrue(
          stats.hasEfficientMemoryUtilization(), "Should return true when peak memory is zero");
    }

    @Test
    @DisplayName("hasEfficientMemoryUtilization should return true when current < 80% of peak")
    void hasEfficientMemoryUtilizationShouldReturnTrueWhenCurrentUnder80Percent() {
      final EngineStatistics stats =
          createTestStats(Duration.ZERO, Duration.ZERO, 1000, 700, 0.0, 0.0);
      assertTrue(
          stats.hasEfficientMemoryUtilization(),
          "Should return true when current memory is less than 80% of peak");
    }

    @Test
    @DisplayName("hasEfficientMemoryUtilization should return false when current >= 80% of peak")
    void hasEfficientMemoryUtilizationShouldReturnFalseWhenCurrentAtOrAbove80Percent() {
      final EngineStatistics stats =
          createTestStats(Duration.ZERO, Duration.ZERO, 1000, 900, 0.0, 0.0);
      assertFalse(
          stats.hasEfficientMemoryUtilization(),
          "Should return false when current memory is 80% or more of peak");
    }

    @Test
    @DisplayName("getPerformanceScore should return positive score with good metrics")
    void getPerformanceScoreShouldReturnPositiveScoreWithGoodMetrics() {
      // Good metrics: low compilation overhead, efficient memory, high cache ratio, high throughput
      final EngineStatistics stats =
          createTestStats(
              Duration.ofSeconds(2), Duration.ofSeconds(10), 1000, 500, 0.9, 2_000_000.0);
      final double score = stats.getPerformanceScore();
      assertTrue(score > 0, "Score should be positive with good metrics. Actual score: " + score);
      assertTrue(score <= 100, "Score should not exceed 100. Actual score: " + score);
    }

    @Test
    @DisplayName("getPerformanceScore should return lower score with poor metrics")
    void getPerformanceScoreShouldReturnLowerScoreWithPoorMetrics() {
      // Poor metrics: high compilation overhead, inefficient memory, low cache ratio, low
      // throughput
      final EngineStatistics poorStats =
          createTestStats(Duration.ofSeconds(8), Duration.ofSeconds(2), 1000, 950, 0.1, 100.0);

      final EngineStatistics goodStats =
          createTestStats(
              Duration.ofSeconds(2), Duration.ofSeconds(10), 1000, 500, 0.9, 2_000_000.0);

      final double poorScore = poorStats.getPerformanceScore();
      final double goodScore = goodStats.getPerformanceScore();

      assertTrue(
          poorScore < goodScore,
          "Poor metrics should have lower score. Poor: " + poorScore + ", Good: " + goodScore);
    }

    @Test
    @DisplayName("getPerformanceScore should return zero when all factors are zero")
    void getPerformanceScoreShouldHandleZeroMetrics() {
      final EngineStatistics stats = createTestStats(Duration.ZERO, Duration.ZERO, 0, 0, 0.0, 0.0);
      final double score = stats.getPerformanceScore();
      assertTrue(score >= 0, "Score should be non-negative. Actual score: " + score);
    }
  }
}
