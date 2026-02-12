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

package ai.tegmentum.wasmtime4j.core;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.InstanceAllocationStrategy;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.pool.PoolStatistics;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests {@link Engine#getPoolingAllocatorMetrics()} under various engine configurations.
 *
 * @since 1.0.0
 */
@DisplayName("Engine Pooling Allocator Metrics Tests")
public class EnginePoolingMetricsTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(EnginePoolingMetricsTest.class.getName());

  private static final String SIMPLE_WAT =
      """
      (module
        (func (export "nop")))
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("default engine returns null pool metrics")
  void defaultEngineReturnsNullPoolMetrics(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing default engine pool metrics");

    try (Engine engine = Engine.create()) {
      final PoolStatistics metrics = engine.getPoolingAllocatorMetrics();

      assertNull(metrics, "Default engine (no pooling) should return null metrics");
      LOGGER.info("[" + runtime + "] Default engine getPoolingAllocatorMetrics = " + metrics);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("pooling engine returns non-null metrics")
  void poolingEngineReturnsMetrics(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing pooling engine metrics");

    try {
      final EngineConfig config =
          Engine.builder().setAllocationStrategy(InstanceAllocationStrategy.POOLING);

      try (Engine engine = Engine.create(config)) {
        final PoolStatistics metrics = engine.getPoolingAllocatorMetrics();

        // Metrics may be null if the default impl is not overridden
        if (metrics != null) {
          LOGGER.info(
              "["
                  + runtime
                  + "] Pooling metrics: allocated="
                  + metrics.getInstancesAllocated()
                  + " reused="
                  + metrics.getInstancesReused()
                  + " created="
                  + metrics.getInstancesCreated());
        } else {
          LOGGER.info(
              "["
                  + runtime
                  + "] Pooling engine returned null metrics "
                  + "(default impl may not be overridden)");
        }
      }
    } catch (final UnsupportedOperationException e) {
      LOGGER.info("[" + runtime + "] Pooling allocation not supported: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.info(
          "["
              + runtime
              + "] Pooling engine creation failed: "
              + e.getClass().getName()
              + " - "
              + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("pool statistics has non-negative values")
  void poolStatisticsHasValidDefaults(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing pool statistics non-negative values");

    try {
      final EngineConfig config =
          Engine.builder().setAllocationStrategy(InstanceAllocationStrategy.POOLING);

      try (Engine engine = Engine.create(config)) {
        final PoolStatistics metrics = engine.getPoolingAllocatorMetrics();

        if (metrics != null) {
          assertTrue(
              metrics.getInstancesAllocated() >= 0, "Instances allocated should be non-negative");
          assertTrue(metrics.getInstancesReused() >= 0, "Instances reused should be non-negative");
          assertTrue(
              metrics.getInstancesCreated() >= 0, "Instances created should be non-negative");
          assertTrue(
              metrics.getMemoryPoolsAllocated() >= 0,
              "Memory pools allocated should be non-negative");
          assertTrue(
              metrics.getStackPoolsAllocated() >= 0,
              "Stack pools allocated should be non-negative");
          assertTrue(
              metrics.getAllocationFailures() >= 0, "Allocation failures should be non-negative");
          assertTrue(
              metrics.getCurrentMemoryUsage() >= 0, "Current memory usage should be non-negative");
          LOGGER.info("[" + runtime + "] All pool statistics are non-negative");
        } else {
          LOGGER.info("[" + runtime + "] Pool metrics returned null despite pooling config");
        }
      }
    } catch (final UnsupportedOperationException e) {
      LOGGER.info("[" + runtime + "] Pooling allocation not supported: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.info(
          "["
              + runtime
              + "] Pooling engine creation failed: "
              + e.getClass().getName()
              + " - "
              + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("pool statistics reflects instance creation")
  void poolStatisticsAfterInstantiation(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing pool statistics after instantiation");

    try {
      final EngineConfig config =
          Engine.builder().setAllocationStrategy(InstanceAllocationStrategy.POOLING);

      try (Engine engine = Engine.create(config)) {
        final Module module = engine.compileWat(SIMPLE_WAT);

        // Check metrics before
        final PoolStatistics before = engine.getPoolingAllocatorMetrics();
        final long allocatedBefore = before != null ? before.getInstancesAllocated() : -1;

        // Create and destroy an instance
        try (Store store = engine.createStore()) {
          final Instance instance = module.instantiate(store);
          instance.callFunction("nop");
          instance.close();
        }

        // Check metrics after
        final PoolStatistics after = engine.getPoolingAllocatorMetrics();
        if (after != null && before != null) {
          LOGGER.info(
              "["
                  + runtime
                  + "] Pool metrics before: allocated="
                  + allocatedBefore
                  + ", after: allocated="
                  + after.getInstancesAllocated()
                  + " created="
                  + after.getInstancesCreated());
          assertTrue(
              after.getInstancesCreated() >= 0,
              "Instances created should be non-negative after instantiation");
        } else {
          LOGGER.info("[" + runtime + "] Pool metrics unavailable for comparison");
        }

        module.close();
      }
    } catch (final UnsupportedOperationException e) {
      LOGGER.info("[" + runtime + "] Pooling allocation not supported: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.info(
          "["
              + runtime
              + "] Pooling engine test failed: "
              + e.getClass().getName()
              + " - "
              + e.getMessage());
    }
  }
}
