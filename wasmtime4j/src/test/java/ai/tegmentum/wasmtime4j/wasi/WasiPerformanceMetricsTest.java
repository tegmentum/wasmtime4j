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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiPerformanceMetrics} interface.
 *
 * <p>WasiPerformanceMetrics provides performance metrics for WASI components.
 */
@DisplayName("WasiPerformanceMetrics Tests")
class WasiPerformanceMetricsTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(WasiPerformanceMetrics.class.getModifiers()),
          "WasiPerformanceMetrics should be public");
      assertTrue(
          WasiPerformanceMetrics.class.isInterface(),
          "WasiPerformanceMetrics should be an interface");
    }
  }

  @Nested
  @DisplayName("Execution Time Method Tests")
  class ExecutionTimeMethodTests {

    @Test
    @DisplayName("should have getAverageExecutionTime method")
    void shouldHaveGetAverageExecutionTimeMethod() throws NoSuchMethodException {
      final Method method = WasiPerformanceMetrics.class.getMethod("getAverageExecutionTime");
      assertNotNull(method, "getAverageExecutionTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have getMedianExecutionTime method")
    void shouldHaveGetMedianExecutionTimeMethod() throws NoSuchMethodException {
      final Method method = WasiPerformanceMetrics.class.getMethod("getMedianExecutionTime");
      assertNotNull(method, "getMedianExecutionTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have getP95ExecutionTime method")
    void shouldHaveGetP95ExecutionTimeMethod() throws NoSuchMethodException {
      final Method method = WasiPerformanceMetrics.class.getMethod("getP95ExecutionTime");
      assertNotNull(method, "getP95ExecutionTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have getP99ExecutionTime method")
    void shouldHaveGetP99ExecutionTimeMethod() throws NoSuchMethodException {
      final Method method = WasiPerformanceMetrics.class.getMethod("getP99ExecutionTime");
      assertNotNull(method, "getP99ExecutionTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }
  }

  @Nested
  @DisplayName("Efficiency Method Tests")
  class EfficiencyMethodTests {

    @Test
    @DisplayName("should have getThroughput method")
    void shouldHaveGetThroughputMethod() throws NoSuchMethodException {
      final Method method = WasiPerformanceMetrics.class.getMethod("getThroughput");
      assertNotNull(method, "getThroughput method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }

    @Test
    @DisplayName("should have getMemoryEfficiency method")
    void shouldHaveGetMemoryEfficiencyMethod() throws NoSuchMethodException {
      final Method method = WasiPerformanceMetrics.class.getMethod("getMemoryEfficiency");
      assertNotNull(method, "getMemoryEfficiency method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }
  }

  @Nested
  @DisplayName("Implementation Behavior Tests")
  class ImplementationBehaviorTests {

    @Test
    @DisplayName("implementation should track execution time percentiles")
    void implementationShouldTrackExecutionTimePercentiles() {
      final WasiPerformanceMetrics metrics =
          createTestMetrics(
              Duration.ofMillis(10),
              Duration.ofMillis(8),
              Duration.ofMillis(25),
              Duration.ofMillis(50),
              1000.0,
              0.85);

      assertEquals(Duration.ofMillis(10), metrics.getAverageExecutionTime());
      assertEquals(Duration.ofMillis(8), metrics.getMedianExecutionTime());
      assertEquals(Duration.ofMillis(25), metrics.getP95ExecutionTime());
      assertEquals(Duration.ofMillis(50), metrics.getP99ExecutionTime());
    }

    @Test
    @DisplayName("percentiles should be ordered correctly")
    void percentilesShouldBeOrderedCorrectly() {
      final WasiPerformanceMetrics metrics =
          createTestMetrics(
              Duration.ofMillis(10),
              Duration.ofMillis(8),
              Duration.ofMillis(25),
              Duration.ofMillis(50),
              1000.0,
              0.85);

      assertTrue(
          metrics.getMedianExecutionTime().compareTo(metrics.getP95ExecutionTime()) <= 0,
          "Median should be <= P95");
      assertTrue(
          metrics.getP95ExecutionTime().compareTo(metrics.getP99ExecutionTime()) <= 0,
          "P95 should be <= P99");
    }

    @Test
    @DisplayName("implementation should track throughput")
    void implementationShouldTrackThroughput() {
      final WasiPerformanceMetrics metrics =
          createTestMetrics(
              Duration.ofMillis(10),
              Duration.ofMillis(8),
              Duration.ofMillis(25),
              Duration.ofMillis(50),
              2500.0,
              0.85);

      assertEquals(2500.0, metrics.getThroughput(), 0.01);
    }

    @Test
    @DisplayName("implementation should track memory efficiency")
    void implementationShouldTrackMemoryEfficiency() {
      final WasiPerformanceMetrics metrics =
          createTestMetrics(
              Duration.ofMillis(10),
              Duration.ofMillis(8),
              Duration.ofMillis(25),
              Duration.ofMillis(50),
              1000.0,
              0.92);

      assertEquals(0.92, metrics.getMemoryEfficiency(), 0.01);
    }

    private WasiPerformanceMetrics createTestMetrics(
        final Duration average,
        final Duration median,
        final Duration p95,
        final Duration p99,
        final double throughput,
        final double memoryEfficiency) {
      return new WasiPerformanceMetrics() {
        @Override
        public Duration getAverageExecutionTime() {
          return average;
        }

        @Override
        public Duration getMedianExecutionTime() {
          return median;
        }

        @Override
        public Duration getP95ExecutionTime() {
          return p95;
        }

        @Override
        public Duration getP99ExecutionTime() {
          return p99;
        }

        @Override
        public double getThroughput() {
          return throughput;
        }

        @Override
        public double getMemoryEfficiency() {
          return memoryEfficiency;
        }
      };
    }
  }
}
