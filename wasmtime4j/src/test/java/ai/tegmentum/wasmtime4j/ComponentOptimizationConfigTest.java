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

import ai.tegmentum.wasmtime4j.ComponentOptimizationConfig.EvictionPolicy;
import ai.tegmentum.wasmtime4j.ComponentOptimizationConfig.MetricType;
import ai.tegmentum.wasmtime4j.ComponentOptimizationConfig.OptimizationLevel;
import ai.tegmentum.wasmtime4j.ComponentOptimizationConfig.OptimizationStrategy;
import ai.tegmentum.wasmtime4j.ComponentOptimizationConfig.PrefetchStrategy;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentOptimizationConfig} interface.
 *
 * <p>ComponentOptimizationConfig provides configuration for component optimization settings.
 */
@DisplayName("ComponentOptimizationConfig Tests")
class ComponentOptimizationConfigTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentOptimizationConfig.class.getModifiers()),
          "ComponentOptimizationConfig should be public");
      assertTrue(
          ComponentOptimizationConfig.class.isInterface(),
          "ComponentOptimizationConfig should be an interface");
    }

    @Test
    @DisplayName("should have CompilationOptimization nested interface")
    void shouldHaveCompilationOptimizationNestedInterface() {
      final var nestedClasses = ComponentOptimizationConfig.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("CompilationOptimization")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "CompilationOptimization should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have CompilationOptimization nested interface");
    }

    @Test
    @DisplayName("should have RuntimeOptimization nested interface")
    void shouldHaveRuntimeOptimizationNestedInterface() {
      final var nestedClasses = ComponentOptimizationConfig.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("RuntimeOptimization")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "RuntimeOptimization should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have RuntimeOptimization nested interface");
    }

    @Test
    @DisplayName("should have MemoryOptimization nested interface")
    void shouldHaveMemoryOptimizationNestedInterface() {
      final var nestedClasses = ComponentOptimizationConfig.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("MemoryOptimization")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "MemoryOptimization should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have MemoryOptimization nested interface");
    }

    @Test
    @DisplayName("should have PerformanceTuning nested interface")
    void shouldHavePerformanceTuningNestedInterface() {
      final var nestedClasses = ComponentOptimizationConfig.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("PerformanceTuning")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "PerformanceTuning should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have PerformanceTuning nested interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have isEnabled method")
    void shouldHaveIsEnabledMethod() throws NoSuchMethodException {
      final Method method = ComponentOptimizationConfig.class.getMethod("isEnabled");
      assertNotNull(method, "isEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getLevel method")
    void shouldHaveGetLevelMethod() throws NoSuchMethodException {
      final Method method = ComponentOptimizationConfig.class.getMethod("getLevel");
      assertNotNull(method, "getLevel method should exist");
      assertEquals(
          OptimizationLevel.class, method.getReturnType(), "Should return OptimizationLevel");
    }

    @Test
    @DisplayName("should have getStrategies method")
    void shouldHaveGetStrategiesMethod() throws NoSuchMethodException {
      final Method method = ComponentOptimizationConfig.class.getMethod("getStrategies");
      assertNotNull(method, "getStrategies method should exist");
      assertEquals(java.util.Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have getCompilationOptimization method")
    void shouldHaveGetCompilationOptimizationMethod() throws NoSuchMethodException {
      final Method method =
          ComponentOptimizationConfig.class.getMethod("getCompilationOptimization");
      assertNotNull(method, "getCompilationOptimization method should exist");
    }

    @Test
    @DisplayName("should have getRuntimeOptimization method")
    void shouldHaveGetRuntimeOptimizationMethod() throws NoSuchMethodException {
      final Method method = ComponentOptimizationConfig.class.getMethod("getRuntimeOptimization");
      assertNotNull(method, "getRuntimeOptimization method should exist");
    }

    @Test
    @DisplayName("should have getMemoryOptimization method")
    void shouldHaveGetMemoryOptimizationMethod() throws NoSuchMethodException {
      final Method method = ComponentOptimizationConfig.class.getMethod("getMemoryOptimization");
      assertNotNull(method, "getMemoryOptimization method should exist");
    }

    @Test
    @DisplayName("should have getPerformanceTuning method")
    void shouldHaveGetPerformanceTuningMethod() throws NoSuchMethodException {
      final Method method = ComponentOptimizationConfig.class.getMethod("getPerformanceTuning");
      assertNotNull(method, "getPerformanceTuning method should exist");
    }

    @Test
    @DisplayName("should have getMetrics method")
    void shouldHaveGetMetricsMethod() throws NoSuchMethodException {
      final Method method = ComponentOptimizationConfig.class.getMethod("getMetrics");
      assertNotNull(method, "getMetrics method should exist");
    }
  }

  @Nested
  @DisplayName("OptimizationLevel Enum Tests")
  class OptimizationLevelEnumTests {

    @Test
    @DisplayName("should have all optimization levels")
    void shouldHaveAllOptimizationLevels() {
      final var levels = OptimizationLevel.values();
      assertEquals(5, levels.length, "Should have 5 optimization levels");
    }

    @Test
    @DisplayName("should have NONE level")
    void shouldHaveNoneLevel() {
      assertEquals(OptimizationLevel.NONE, OptimizationLevel.valueOf("NONE"));
    }

    @Test
    @DisplayName("should have BASIC level")
    void shouldHaveBasicLevel() {
      assertEquals(OptimizationLevel.BASIC, OptimizationLevel.valueOf("BASIC"));
    }

    @Test
    @DisplayName("should have STANDARD level")
    void shouldHaveStandardLevel() {
      assertEquals(OptimizationLevel.STANDARD, OptimizationLevel.valueOf("STANDARD"));
    }

    @Test
    @DisplayName("should have AGGRESSIVE level")
    void shouldHaveAggressiveLevel() {
      assertEquals(OptimizationLevel.AGGRESSIVE, OptimizationLevel.valueOf("AGGRESSIVE"));
    }

    @Test
    @DisplayName("should have MAXIMUM level")
    void shouldHaveMaximumLevel() {
      assertEquals(OptimizationLevel.MAXIMUM, OptimizationLevel.valueOf("MAXIMUM"));
    }
  }

  @Nested
  @DisplayName("OptimizationStrategy Enum Tests")
  class OptimizationStrategyEnumTests {

    @Test
    @DisplayName("should have all optimization strategies")
    void shouldHaveAllOptimizationStrategies() {
      final var strategies = OptimizationStrategy.values();
      assertEquals(5, strategies.length, "Should have 5 optimization strategies");
    }

    @Test
    @DisplayName("should have SPEED strategy")
    void shouldHaveSpeedStrategy() {
      assertEquals(OptimizationStrategy.SPEED, OptimizationStrategy.valueOf("SPEED"));
    }

    @Test
    @DisplayName("should have SIZE strategy")
    void shouldHaveSizeStrategy() {
      assertEquals(OptimizationStrategy.SIZE, OptimizationStrategy.valueOf("SIZE"));
    }

    @Test
    @DisplayName("should have MEMORY strategy")
    void shouldHaveMemoryStrategy() {
      assertEquals(OptimizationStrategy.MEMORY, OptimizationStrategy.valueOf("MEMORY"));
    }

    @Test
    @DisplayName("should have POWER strategy")
    void shouldHavePowerStrategy() {
      assertEquals(OptimizationStrategy.POWER, OptimizationStrategy.valueOf("POWER"));
    }

    @Test
    @DisplayName("should have BALANCED strategy")
    void shouldHaveBalancedStrategy() {
      assertEquals(OptimizationStrategy.BALANCED, OptimizationStrategy.valueOf("BALANCED"));
    }
  }

  @Nested
  @DisplayName("EvictionPolicy Enum Tests")
  class EvictionPolicyEnumTests {

    @Test
    @DisplayName("should have all eviction policies")
    void shouldHaveAllEvictionPolicies() {
      final var policies = EvictionPolicy.values();
      assertEquals(4, policies.length, "Should have 4 eviction policies");
    }

    @Test
    @DisplayName("should have LRU policy")
    void shouldHaveLruPolicy() {
      assertEquals(EvictionPolicy.LRU, EvictionPolicy.valueOf("LRU"));
    }

    @Test
    @DisplayName("should have LFU policy")
    void shouldHaveLfuPolicy() {
      assertEquals(EvictionPolicy.LFU, EvictionPolicy.valueOf("LFU"));
    }

    @Test
    @DisplayName("should have FIFO policy")
    void shouldHaveFifoPolicy() {
      assertEquals(EvictionPolicy.FIFO, EvictionPolicy.valueOf("FIFO"));
    }

    @Test
    @DisplayName("should have RANDOM policy")
    void shouldHaveRandomPolicy() {
      assertEquals(EvictionPolicy.RANDOM, EvictionPolicy.valueOf("RANDOM"));
    }
  }

  @Nested
  @DisplayName("PrefetchStrategy Enum Tests")
  class PrefetchStrategyEnumTests {

    @Test
    @DisplayName("should have all prefetch strategies")
    void shouldHaveAllPrefetchStrategies() {
      final var strategies = PrefetchStrategy.values();
      assertEquals(4, strategies.length, "Should have 4 prefetch strategies");
    }

    @Test
    @DisplayName("should have SEQUENTIAL strategy")
    void shouldHaveSequentialStrategy() {
      assertEquals(PrefetchStrategy.SEQUENTIAL, PrefetchStrategy.valueOf("SEQUENTIAL"));
    }

    @Test
    @DisplayName("should have PREDICTIVE strategy")
    void shouldHavePredictiveStrategy() {
      assertEquals(PrefetchStrategy.PREDICTIVE, PrefetchStrategy.valueOf("PREDICTIVE"));
    }

    @Test
    @DisplayName("should have ADAPTIVE strategy")
    void shouldHaveAdaptiveStrategy() {
      assertEquals(PrefetchStrategy.ADAPTIVE, PrefetchStrategy.valueOf("ADAPTIVE"));
    }

    @Test
    @DisplayName("should have HISTORICAL strategy")
    void shouldHaveHistoricalStrategy() {
      assertEquals(PrefetchStrategy.HISTORICAL, PrefetchStrategy.valueOf("HISTORICAL"));
    }
  }

  @Nested
  @DisplayName("MetricType Enum Tests")
  class MetricTypeEnumTests {

    @Test
    @DisplayName("should have all metric types")
    void shouldHaveAllMetricTypes() {
      final var types = MetricType.values();
      assertEquals(5, types.length, "Should have 5 metric types");
    }

    @Test
    @DisplayName("should have EXECUTION_TIME type")
    void shouldHaveExecutionTimeType() {
      assertEquals(MetricType.EXECUTION_TIME, MetricType.valueOf("EXECUTION_TIME"));
    }

    @Test
    @DisplayName("should have MEMORY_USAGE type")
    void shouldHaveMemoryUsageType() {
      assertEquals(MetricType.MEMORY_USAGE, MetricType.valueOf("MEMORY_USAGE"));
    }

    @Test
    @DisplayName("should have CACHE_HIT_RATE type")
    void shouldHaveCacheHitRateType() {
      assertEquals(MetricType.CACHE_HIT_RATE, MetricType.valueOf("CACHE_HIT_RATE"));
    }

    @Test
    @DisplayName("should have OPTIMIZATION_EFFECTIVENESS type")
    void shouldHaveOptimizationEffectivenessType() {
      assertEquals(
          MetricType.OPTIMIZATION_EFFECTIVENESS, MetricType.valueOf("OPTIMIZATION_EFFECTIVENESS"));
    }

    @Test
    @DisplayName("should have THROUGHPUT type")
    void shouldHaveThroughputType() {
      assertEquals(MetricType.THROUGHPUT, MetricType.valueOf("THROUGHPUT"));
    }
  }

  @Nested
  @DisplayName("Nested Interface Structure Tests")
  class NestedInterfaceStructureTests {

    @Test
    @DisplayName("should have all expected nested interfaces")
    void shouldHaveAllExpectedNestedInterfaces() {
      final var nestedClasses = ComponentOptimizationConfig.class.getDeclaredClasses();
      final var classNames =
          java.util.Arrays.stream(nestedClasses)
              .map(Class::getSimpleName)
              .collect(java.util.stream.Collectors.toSet());

      assertTrue(
          classNames.contains("CompilationOptimization"), "Should have CompilationOptimization");
      assertTrue(classNames.contains("RuntimeOptimization"), "Should have RuntimeOptimization");
      assertTrue(classNames.contains("MemoryOptimization"), "Should have MemoryOptimization");
      assertTrue(classNames.contains("PerformanceTuning"), "Should have PerformanceTuning");
      assertTrue(classNames.contains("TimeoutConfig"), "Should have TimeoutConfig");
      assertTrue(classNames.contains("CacheConfig"), "Should have CacheConfig");
      assertTrue(classNames.contains("PrefetchConfig"), "Should have PrefetchConfig");
      assertTrue(classNames.contains("OptimizationMetrics"), "Should have OptimizationMetrics");
      assertTrue(classNames.contains("OptimizationPass"), "Should have OptimizationPass");
    }

    @Test
    @DisplayName("should have all expected enums")
    void shouldHaveAllExpectedEnums() {
      final var nestedClasses = ComponentOptimizationConfig.class.getDeclaredClasses();
      final var enumNames =
          java.util.Arrays.stream(nestedClasses)
              .filter(Class::isEnum)
              .map(Class::getSimpleName)
              .collect(java.util.stream.Collectors.toSet());

      assertTrue(enumNames.contains("OptimizationLevel"), "Should have OptimizationLevel enum");
      assertTrue(
          enumNames.contains("OptimizationStrategy"), "Should have OptimizationStrategy enum");
      assertTrue(enumNames.contains("EvictionPolicy"), "Should have EvictionPolicy enum");
      assertTrue(enumNames.contains("PrefetchStrategy"), "Should have PrefetchStrategy enum");
      assertTrue(enumNames.contains("MetricType"), "Should have MetricType enum");
    }
  }
}
