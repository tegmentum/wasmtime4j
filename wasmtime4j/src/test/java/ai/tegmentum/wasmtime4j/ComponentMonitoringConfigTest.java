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

import ai.tegmentum.wasmtime4j.ComponentMonitoringConfig.AlertThresholds;
import ai.tegmentum.wasmtime4j.ComponentMonitoringConfig.MonitoredMetric;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentMonitoringConfig} interface.
 *
 * <p>ComponentMonitoringConfig provides configuration for component monitoring.
 */
@DisplayName("ComponentMonitoringConfig Tests")
class ComponentMonitoringConfigTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentMonitoringConfig.class.getModifiers()),
          "ComponentMonitoringConfig should be public");
      assertTrue(
          ComponentMonitoringConfig.class.isInterface(),
          "ComponentMonitoringConfig should be an interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have isEnabled method")
    void shouldHaveIsEnabledMethod() throws NoSuchMethodException {
      final Method method = ComponentMonitoringConfig.class.getMethod("isEnabled");
      assertNotNull(method, "isEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getMetrics method")
    void shouldHaveGetMetricsMethod() throws NoSuchMethodException {
      final Method method = ComponentMonitoringConfig.class.getMethod("getMetrics");
      assertNotNull(method, "getMetrics method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have getInterval method")
    void shouldHaveGetIntervalMethod() throws NoSuchMethodException {
      final Method method = ComponentMonitoringConfig.class.getMethod("getInterval");
      assertNotNull(method, "getInterval method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getThresholds method")
    void shouldHaveGetThresholdsMethod() throws NoSuchMethodException {
      final Method method = ComponentMonitoringConfig.class.getMethod("getThresholds");
      assertNotNull(method, "getThresholds method should exist");
      assertEquals(AlertThresholds.class, method.getReturnType(), "Should return AlertThresholds");
    }

    @Test
    @DisplayName("should have getDestinations method")
    void shouldHaveGetDestinationsMethod() throws NoSuchMethodException {
      final Method method = ComponentMonitoringConfig.class.getMethod("getDestinations");
      assertNotNull(method, "getDestinations method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }
  }

  @Nested
  @DisplayName("MonitoredMetric Enum Tests")
  class MonitoredMetricEnumTests {

    @Test
    @DisplayName("should have all monitored metrics")
    void shouldHaveAllMonitoredMetrics() {
      final var metrics = MonitoredMetric.values();
      assertEquals(5, metrics.length, "Should have 5 monitored metrics");
    }

    @Test
    @DisplayName("should have MEMORY_USAGE metric")
    void shouldHaveMemoryUsageMetric() {
      assertEquals(MonitoredMetric.MEMORY_USAGE, MonitoredMetric.valueOf("MEMORY_USAGE"));
    }

    @Test
    @DisplayName("should have CPU_USAGE metric")
    void shouldHaveCpuUsageMetric() {
      assertEquals(MonitoredMetric.CPU_USAGE, MonitoredMetric.valueOf("CPU_USAGE"));
    }

    @Test
    @DisplayName("should have EXECUTION_TIME metric")
    void shouldHaveExecutionTimeMetric() {
      assertEquals(MonitoredMetric.EXECUTION_TIME, MonitoredMetric.valueOf("EXECUTION_TIME"));
    }

    @Test
    @DisplayName("should have ERROR_RATE metric")
    void shouldHaveErrorRateMetric() {
      assertEquals(MonitoredMetric.ERROR_RATE, MonitoredMetric.valueOf("ERROR_RATE"));
    }

    @Test
    @DisplayName("should have THROUGHPUT metric")
    void shouldHaveThroughputMetric() {
      assertEquals(MonitoredMetric.THROUGHPUT, MonitoredMetric.valueOf("THROUGHPUT"));
    }
  }

  @Nested
  @DisplayName("AlertThresholds Interface Tests")
  class AlertThresholdsInterfaceTests {

    @Test
    @DisplayName("AlertThresholds should be nested interface")
    void alertThresholdsShouldBeNestedInterface() {
      assertTrue(AlertThresholds.class.isInterface(), "AlertThresholds should be an interface");
    }

    @Test
    @DisplayName("AlertThresholds should have getMemoryThreshold method")
    void shouldHaveGetMemoryThresholdMethod() throws NoSuchMethodException {
      final Method method = AlertThresholds.class.getMethod("getMemoryThreshold");
      assertNotNull(method, "getMemoryThreshold method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }

    @Test
    @DisplayName("AlertThresholds should have getCpuThreshold method")
    void shouldHaveGetCpuThresholdMethod() throws NoSuchMethodException {
      final Method method = AlertThresholds.class.getMethod("getCpuThreshold");
      assertNotNull(method, "getCpuThreshold method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }

    @Test
    @DisplayName("AlertThresholds should have getErrorRateThreshold method")
    void shouldHaveGetErrorRateThresholdMethod() throws NoSuchMethodException {
      final Method method = AlertThresholds.class.getMethod("getErrorRateThreshold");
      assertNotNull(method, "getErrorRateThreshold method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }
  }

  @Nested
  @DisplayName("Stub Implementation Tests")
  class StubImplementationTests {

    /** Stub implementation for AlertThresholds. */
    private static class StubAlertThresholds implements AlertThresholds {
      private final double memoryThreshold;
      private final double cpuThreshold;
      private final double errorRateThreshold;

      StubAlertThresholds(
          final double memoryThreshold,
          final double cpuThreshold,
          final double errorRateThreshold) {
        this.memoryThreshold = memoryThreshold;
        this.cpuThreshold = cpuThreshold;
        this.errorRateThreshold = errorRateThreshold;
      }

      @Override
      public double getMemoryThreshold() {
        return memoryThreshold;
      }

      @Override
      public double getCpuThreshold() {
        return cpuThreshold;
      }

      @Override
      public double getErrorRateThreshold() {
        return errorRateThreshold;
      }
    }

    /** Stub implementation for ComponentMonitoringConfig. */
    private static class StubComponentMonitoringConfig implements ComponentMonitoringConfig {
      private final boolean enabled;
      private final Set<MonitoredMetric> metrics;
      private final long interval;
      private final AlertThresholds thresholds;
      private final List<String> destinations;

      StubComponentMonitoringConfig(
          final boolean enabled,
          final Set<MonitoredMetric> metrics,
          final long interval,
          final AlertThresholds thresholds,
          final List<String> destinations) {
        this.enabled = enabled;
        this.metrics = metrics;
        this.interval = interval;
        this.thresholds = thresholds;
        this.destinations = destinations;
      }

      @Override
      public boolean isEnabled() {
        return enabled;
      }

      @Override
      public Set<MonitoredMetric> getMetrics() {
        return metrics;
      }

      @Override
      public long getInterval() {
        return interval;
      }

      @Override
      public AlertThresholds getThresholds() {
        return thresholds;
      }

      @Override
      public List<String> getDestinations() {
        return destinations;
      }
    }

    @Test
    @DisplayName("stub should return correct enabled state")
    void stubShouldReturnCorrectEnabledState() {
      final var thresholds = new StubAlertThresholds(0.8, 0.9, 0.05);
      final var metrics = Set.of(MonitoredMetric.MEMORY_USAGE, MonitoredMetric.CPU_USAGE);
      final var destinations = List.of("console", "file:/var/log/metrics.log");

      final ComponentMonitoringConfig config =
          new StubComponentMonitoringConfig(true, metrics, 5000L, thresholds, destinations);

      assertTrue(config.isEnabled(), "Should be enabled");
    }

    @Test
    @DisplayName("stub should return correct metrics")
    void stubShouldReturnCorrectMetrics() {
      final var thresholds = new StubAlertThresholds(0.8, 0.9, 0.05);
      final var metrics =
          Set.of(
              MonitoredMetric.MEMORY_USAGE, MonitoredMetric.CPU_USAGE, MonitoredMetric.THROUGHPUT);
      final var destinations = List.of("console");

      final ComponentMonitoringConfig config =
          new StubComponentMonitoringConfig(true, metrics, 5000L, thresholds, destinations);

      assertEquals(3, config.getMetrics().size(), "Should have 3 metrics");
      assertTrue(
          config.getMetrics().contains(MonitoredMetric.MEMORY_USAGE),
          "Should contain MEMORY_USAGE");
      assertTrue(
          config.getMetrics().contains(MonitoredMetric.CPU_USAGE), "Should contain CPU_USAGE");
      assertTrue(
          config.getMetrics().contains(MonitoredMetric.THROUGHPUT), "Should contain THROUGHPUT");
    }

    @Test
    @DisplayName("stub should return correct interval")
    void stubShouldReturnCorrectInterval() {
      final var thresholds = new StubAlertThresholds(0.8, 0.9, 0.05);
      final var metrics = Set.of(MonitoredMetric.MEMORY_USAGE);
      final var destinations = List.of("console");

      final ComponentMonitoringConfig config =
          new StubComponentMonitoringConfig(true, metrics, 10000L, thresholds, destinations);

      assertEquals(10000L, config.getInterval(), "Interval should be 10 seconds");
    }

    @Test
    @DisplayName("stub should return correct thresholds")
    void stubShouldReturnCorrectThresholds() {
      final var thresholds = new StubAlertThresholds(0.75, 0.85, 0.10);
      final var metrics = Set.of(MonitoredMetric.MEMORY_USAGE);
      final var destinations = List.of("console");

      final ComponentMonitoringConfig config =
          new StubComponentMonitoringConfig(true, metrics, 5000L, thresholds, destinations);

      assertEquals(0.75, config.getThresholds().getMemoryThreshold(), 0.001, "Memory threshold");
      assertEquals(0.85, config.getThresholds().getCpuThreshold(), 0.001, "CPU threshold");
      assertEquals(0.10, config.getThresholds().getErrorRateThreshold(), 0.001, "Error rate");
    }

    @Test
    @DisplayName("stub should return correct destinations")
    void stubShouldReturnCorrectDestinations() {
      final var thresholds = new StubAlertThresholds(0.8, 0.9, 0.05);
      final var metrics = Set.of(MonitoredMetric.MEMORY_USAGE);
      final var destinations = List.of("console", "prometheus", "datadog");

      final ComponentMonitoringConfig config =
          new StubComponentMonitoringConfig(true, metrics, 5000L, thresholds, destinations);

      assertEquals(3, config.getDestinations().size(), "Should have 3 destinations");
      assertTrue(config.getDestinations().contains("console"), "Should contain console");
      assertTrue(config.getDestinations().contains("prometheus"), "Should contain prometheus");
      assertTrue(config.getDestinations().contains("datadog"), "Should contain datadog");
    }

    @Test
    @DisplayName("thresholds should handle boundary values")
    void thresholdsShouldHandleBoundaryValues() {
      final var zeroThresholds = new StubAlertThresholds(0.0, 0.0, 0.0);
      final var maxThresholds = new StubAlertThresholds(1.0, 1.0, 1.0);

      assertEquals(0.0, zeroThresholds.getMemoryThreshold(), 0.001, "Zero memory threshold");
      assertEquals(1.0, maxThresholds.getCpuThreshold(), 0.001, "Max CPU threshold");
    }
  }
}
