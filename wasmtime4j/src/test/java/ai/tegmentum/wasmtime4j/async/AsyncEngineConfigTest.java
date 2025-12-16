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

package ai.tegmentum.wasmtime4j.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for AsyncEngineConfig class.
 *
 * <p>Verifies configuration options for asynchronous WebAssembly engine operations.
 */
@DisplayName("AsyncEngineConfig Tests")
class AsyncEngineConfigTest {

  @Nested
  @DisplayName("Constructor and Default Values Tests")
  class ConstructorAndDefaultValuesTests {

    @Test
    @DisplayName("should create config with default values")
    void shouldCreateConfigWithDefaultValues() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      assertNotNull(config, "Config should not be null");
      assertEquals(
          Runtime.getRuntime().availableProcessors(),
          config.getMaxParallelThreads(),
          "Default threads should be available processors");
      assertEquals(
          Duration.ofMinutes(5), config.getCompilationTimeout(), "Default timeout should be 5 min");
      assertTrue(
          config.isProgressTrackingEnabled(), "Progress tracking should be enabled by default");
      assertEquals(
          Duration.ofMillis(100),
          config.getProgressUpdateInterval(),
          "Default update interval should be 100ms");
      assertNotNull(config.getDefaultExecutor(), "Default executor should not be null");
      assertEquals(100, config.getMaxQueuedCompilations(), "Default max queued should be 100");
      assertTrue(config.isCompilationCachingEnabled(), "Caching should be enabled by default");
      assertEquals(
          100L * 1024 * 1024, config.getMaxCacheSize(), "Default cache size should be 100MB");
      assertTrue(
          config.isBatchCompilationEnabled(), "Batch compilation should be enabled by default");
      assertEquals(4, config.getBatchSize(), "Default batch size should be 4");
      assertTrue(config.isResourceMonitoringEnabled(), "Resource monitoring should be enabled");
      assertEquals(
          512L * 1024 * 1024, config.getMaxMemoryUsage(), "Default max memory should be 512MB");
      assertTrue(config.isAdaptiveThreadingEnabled(), "Adaptive threading should be enabled");
      assertEquals(
          1.5, config.getThreadScalingFactor(), 0.001, "Default scaling factor should be 1.5");
      assertTrue(
          config.isCompilationStatisticsEnabled(), "Compilation statistics should be enabled");
      assertTrue(config.isCancellationEnabled(), "Cancellation should be enabled by default");
    }

    @Test
    @DisplayName("should create default config via factory method")
    void shouldCreateDefaultConfigViaFactoryMethod() {
      AsyncEngineConfig config = AsyncEngineConfig.defaultConfig();

      assertNotNull(config, "Default config should not be null");
      assertEquals(
          Runtime.getRuntime().availableProcessors(),
          config.getMaxParallelThreads(),
          "Default threads should match available processors");
    }
  }

  @Nested
  @DisplayName("SetMaxParallelThreads Tests")
  class SetMaxParallelThreadsTests {

    @Test
    @DisplayName("should set max parallel threads")
    void shouldSetMaxParallelThreads() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      AsyncEngineConfig result = config.setMaxParallelThreads(8);

      assertEquals(config, result, "Should return same instance for chaining");
      assertEquals(8, config.getMaxParallelThreads(), "Max threads should be 8");
    }

    @Test
    @DisplayName("should set unlimited threads when 0")
    void shouldSetUnlimitedThreadsWhenZero() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      config.setMaxParallelThreads(0);

      assertEquals(
          Integer.MAX_VALUE, config.getMaxParallelThreads(), "0 should set unlimited (MAX_VALUE)");
    }

    @Test
    @DisplayName("should reject negative threads")
    void shouldRejectNegativeThreads() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> config.setMaxParallelThreads(-1),
              "Should throw for negative threads");

      assertTrue(
          exception.getMessage().contains("negative"),
          "Exception should mention negative: " + exception.getMessage());
    }
  }

  @Nested
  @DisplayName("SetCompilationTimeout Tests")
  class SetCompilationTimeoutTests {

    @Test
    @DisplayName("should set compilation timeout")
    void shouldSetCompilationTimeout() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      config.setCompilationTimeout(Duration.ofMinutes(10));

      assertEquals(
          Duration.ofMinutes(10), config.getCompilationTimeout(), "Timeout should be 10 minutes");
    }

    @Test
    @DisplayName("should reject null timeout")
    void shouldRejectNullTimeout() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      assertThrows(
          IllegalArgumentException.class,
          () -> config.setCompilationTimeout(null),
          "Should throw for null timeout");
    }

    @Test
    @DisplayName("should reject negative timeout")
    void shouldRejectNegativeTimeout() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      assertThrows(
          IllegalArgumentException.class,
          () -> config.setCompilationTimeout(Duration.ofMillis(-1)),
          "Should throw for negative timeout");
    }
  }

  @Nested
  @DisplayName("SetProgressTracking Tests")
  class SetProgressTrackingTests {

    @Test
    @DisplayName("should enable progress tracking")
    void shouldEnableProgressTracking() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      config.setProgressTracking(true);

      assertTrue(config.isProgressTrackingEnabled(), "Progress tracking should be enabled");
    }

    @Test
    @DisplayName("should disable progress tracking")
    void shouldDisableProgressTracking() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      config.setProgressTracking(false);

      assertFalse(config.isProgressTrackingEnabled(), "Progress tracking should be disabled");
    }
  }

  @Nested
  @DisplayName("SetProgressUpdateInterval Tests")
  class SetProgressUpdateIntervalTests {

    @Test
    @DisplayName("should set progress update interval")
    void shouldSetProgressUpdateInterval() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      config.setProgressUpdateInterval(Duration.ofMillis(500));

      assertEquals(
          Duration.ofMillis(500),
          config.getProgressUpdateInterval(),
          "Update interval should be 500ms");
    }

    @Test
    @DisplayName("should reject null interval")
    void shouldRejectNullInterval() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      assertThrows(
          IllegalArgumentException.class,
          () -> config.setProgressUpdateInterval(null),
          "Should throw for null interval");
    }

    @Test
    @DisplayName("should reject negative interval")
    void shouldRejectNegativeInterval() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      assertThrows(
          IllegalArgumentException.class,
          () -> config.setProgressUpdateInterval(Duration.ofMillis(-1)),
          "Should throw for negative interval");
    }
  }

  @Nested
  @DisplayName("SetDefaultExecutor Tests")
  class SetDefaultExecutorTests {

    @Test
    @DisplayName("should set default executor")
    void shouldSetDefaultExecutor() {
      AsyncEngineConfig config = new AsyncEngineConfig();
      Executor customExecutor = Executors.newFixedThreadPool(2);

      config.setDefaultExecutor(customExecutor);

      assertEquals(
          customExecutor, config.getDefaultExecutor(), "Executor should be custom executor");
    }

    @Test
    @DisplayName("should reject null executor")
    void shouldRejectNullExecutor() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      assertThrows(
          IllegalArgumentException.class,
          () -> config.setDefaultExecutor(null),
          "Should throw for null executor");
    }
  }

  @Nested
  @DisplayName("SetMaxQueuedCompilations Tests")
  class SetMaxQueuedCompilationsTests {

    @Test
    @DisplayName("should set max queued compilations")
    void shouldSetMaxQueuedCompilations() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      config.setMaxQueuedCompilations(50);

      assertEquals(50, config.getMaxQueuedCompilations(), "Max queued should be 50");
    }

    @Test
    @DisplayName("should accept zero queued compilations")
    void shouldAcceptZeroQueuedCompilations() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      config.setMaxQueuedCompilations(0);

      assertEquals(0, config.getMaxQueuedCompilations(), "Max queued should be 0");
    }

    @Test
    @DisplayName("should reject negative queued compilations")
    void shouldRejectNegativeQueuedCompilations() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      assertThrows(
          IllegalArgumentException.class,
          () -> config.setMaxQueuedCompilations(-1),
          "Should throw for negative queued");
    }
  }

  @Nested
  @DisplayName("SetCompilationCaching Tests")
  class SetCompilationCachingTests {

    @Test
    @DisplayName("should enable compilation caching")
    void shouldEnableCompilationCaching() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      config.setCompilationCaching(true);

      assertTrue(config.isCompilationCachingEnabled(), "Caching should be enabled");
    }

    @Test
    @DisplayName("should disable compilation caching")
    void shouldDisableCompilationCaching() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      config.setCompilationCaching(false);

      assertFalse(config.isCompilationCachingEnabled(), "Caching should be disabled");
    }
  }

  @Nested
  @DisplayName("SetMaxCacheSize Tests")
  class SetMaxCacheSizeTests {

    @Test
    @DisplayName("should set max cache size")
    void shouldSetMaxCacheSize() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      config.setMaxCacheSize(256L * 1024 * 1024);

      assertEquals(256L * 1024 * 1024, config.getMaxCacheSize(), "Cache size should be 256MB");
    }

    @Test
    @DisplayName("should accept zero cache size")
    void shouldAcceptZeroCacheSize() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      config.setMaxCacheSize(0);

      assertEquals(0, config.getMaxCacheSize(), "Cache size should be 0");
    }

    @Test
    @DisplayName("should reject negative cache size")
    void shouldRejectNegativeCacheSize() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      assertThrows(
          IllegalArgumentException.class,
          () -> config.setMaxCacheSize(-1),
          "Should throw for negative cache size");
    }
  }

  @Nested
  @DisplayName("SetBatchCompilation Tests")
  class SetBatchCompilationTests {

    @Test
    @DisplayName("should enable batch compilation")
    void shouldEnableBatchCompilation() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      config.setBatchCompilation(true);

      assertTrue(config.isBatchCompilationEnabled(), "Batch compilation should be enabled");
    }

    @Test
    @DisplayName("should disable batch compilation")
    void shouldDisableBatchCompilation() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      config.setBatchCompilation(false);

      assertFalse(config.isBatchCompilationEnabled(), "Batch compilation should be disabled");
    }
  }

  @Nested
  @DisplayName("SetBatchSize Tests")
  class SetBatchSizeTests {

    @Test
    @DisplayName("should set batch size")
    void shouldSetBatchSize() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      config.setBatchSize(8);

      assertEquals(8, config.getBatchSize(), "Batch size should be 8");
    }

    @Test
    @DisplayName("should accept minimum batch size of 1")
    void shouldAcceptMinimumBatchSize() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      config.setBatchSize(1);

      assertEquals(1, config.getBatchSize(), "Batch size should be 1");
    }

    @Test
    @DisplayName("should reject batch size less than 1")
    void shouldRejectBatchSizeLessThanOne() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> config.setBatchSize(0),
              "Should throw for batch size 0");

      assertTrue(
          exception.getMessage().contains("at least 1"),
          "Exception should mention at least 1: " + exception.getMessage());
    }
  }

  @Nested
  @DisplayName("SetResourceMonitoring Tests")
  class SetResourceMonitoringTests {

    @Test
    @DisplayName("should enable resource monitoring")
    void shouldEnableResourceMonitoring() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      config.setResourceMonitoring(true);

      assertTrue(config.isResourceMonitoringEnabled(), "Resource monitoring should be enabled");
    }

    @Test
    @DisplayName("should disable resource monitoring")
    void shouldDisableResourceMonitoring() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      config.setResourceMonitoring(false);

      assertFalse(config.isResourceMonitoringEnabled(), "Resource monitoring should be disabled");
    }
  }

  @Nested
  @DisplayName("SetMaxMemoryUsage Tests")
  class SetMaxMemoryUsageTests {

    @Test
    @DisplayName("should set max memory usage")
    void shouldSetMaxMemoryUsage() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      config.setMaxMemoryUsage(1024L * 1024 * 1024);

      assertEquals(1024L * 1024 * 1024, config.getMaxMemoryUsage(), "Max memory should be 1GB");
    }

    @Test
    @DisplayName("should accept zero memory usage")
    void shouldAcceptZeroMemoryUsage() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      config.setMaxMemoryUsage(0);

      assertEquals(0, config.getMaxMemoryUsage(), "Max memory should be 0");
    }

    @Test
    @DisplayName("should reject negative memory usage")
    void shouldRejectNegativeMemoryUsage() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      assertThrows(
          IllegalArgumentException.class,
          () -> config.setMaxMemoryUsage(-1),
          "Should throw for negative memory");
    }
  }

  @Nested
  @DisplayName("SetAdaptiveThreading Tests")
  class SetAdaptiveThreadingTests {

    @Test
    @DisplayName("should enable adaptive threading")
    void shouldEnableAdaptiveThreading() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      config.setAdaptiveThreading(true);

      assertTrue(config.isAdaptiveThreadingEnabled(), "Adaptive threading should be enabled");
    }

    @Test
    @DisplayName("should disable adaptive threading")
    void shouldDisableAdaptiveThreading() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      config.setAdaptiveThreading(false);

      assertFalse(config.isAdaptiveThreadingEnabled(), "Adaptive threading should be disabled");
    }
  }

  @Nested
  @DisplayName("SetThreadScalingFactor Tests")
  class SetThreadScalingFactorTests {

    @Test
    @DisplayName("should set thread scaling factor")
    void shouldSetThreadScalingFactor() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      config.setThreadScalingFactor(2.0);

      assertEquals(2.0, config.getThreadScalingFactor(), 0.001, "Scaling factor should be 2.0");
    }

    @Test
    @DisplayName("should accept factor of 1.0")
    void shouldAcceptFactorOfOne() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      config.setThreadScalingFactor(1.0);

      assertEquals(1.0, config.getThreadScalingFactor(), 0.001, "Scaling factor should be 1.0");
    }

    @Test
    @DisplayName("should reject zero scaling factor")
    void shouldRejectZeroScalingFactor() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      assertThrows(
          IllegalArgumentException.class,
          () -> config.setThreadScalingFactor(0.0),
          "Should throw for zero scaling factor");
    }

    @Test
    @DisplayName("should reject negative scaling factor")
    void shouldRejectNegativeScalingFactor() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      assertThrows(
          IllegalArgumentException.class,
          () -> config.setThreadScalingFactor(-1.0),
          "Should throw for negative scaling factor");
    }
  }

  @Nested
  @DisplayName("SetCompilationStatistics Tests")
  class SetCompilationStatisticsTests {

    @Test
    @DisplayName("should enable compilation statistics")
    void shouldEnableCompilationStatistics() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      config.setCompilationStatistics(true);

      assertTrue(config.isCompilationStatisticsEnabled(), "Statistics should be enabled");
    }

    @Test
    @DisplayName("should disable compilation statistics")
    void shouldDisableCompilationStatistics() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      config.setCompilationStatistics(false);

      assertFalse(config.isCompilationStatisticsEnabled(), "Statistics should be disabled");
    }
  }

  @Nested
  @DisplayName("SetCancellation Tests")
  class SetCancellationTests {

    @Test
    @DisplayName("should enable cancellation")
    void shouldEnableCancellation() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      config.setCancellation(true);

      assertTrue(config.isCancellationEnabled(), "Cancellation should be enabled");
    }

    @Test
    @DisplayName("should disable cancellation")
    void shouldDisableCancellation() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      config.setCancellation(false);

      assertFalse(config.isCancellationEnabled(), "Cancellation should be disabled");
    }
  }

  @Nested
  @DisplayName("Factory Methods Tests")
  class FactoryMethodsTests {

    @Test
    @DisplayName("should create high performance config")
    void shouldCreateHighPerformanceConfig() {
      AsyncEngineConfig config = AsyncEngineConfig.highPerformance();

      assertNotNull(config, "High performance config should not be null");
      assertEquals(
          Runtime.getRuntime().availableProcessors() * 2,
          config.getMaxParallelThreads(),
          "High performance should use 2x processors");
      assertTrue(config.isBatchCompilationEnabled(), "Batch compilation should be enabled");
      assertEquals(8, config.getBatchSize(), "Batch size should be 8");
      assertTrue(config.isAdaptiveThreadingEnabled(), "Adaptive threading should be enabled");
      assertTrue(config.isCompilationCachingEnabled(), "Caching should be enabled");
      assertEquals(256L * 1024 * 1024, config.getMaxCacheSize(), "Cache size should be 256MB");
    }

    @Test
    @DisplayName("should create low resource config")
    void shouldCreateLowResourceConfig() {
      AsyncEngineConfig config = AsyncEngineConfig.lowResource();

      assertNotNull(config, "Low resource config should not be null");
      assertEquals(2, config.getMaxParallelThreads(), "Low resource should use 2 threads");
      assertFalse(config.isBatchCompilationEnabled(), "Batch compilation should be disabled");
      assertFalse(config.isProgressTrackingEnabled(), "Progress tracking should be disabled");
      assertFalse(config.isResourceMonitoringEnabled(), "Resource monitoring should be disabled");
      assertFalse(config.isCompilationCachingEnabled(), "Caching should be disabled");
      assertEquals(64L * 1024 * 1024, config.getMaxMemoryUsage(), "Max memory should be 64MB");
    }
  }

  @Nested
  @DisplayName("Method Chaining Tests")
  class MethodChainingTests {

    @Test
    @DisplayName("should support method chaining")
    void shouldSupportMethodChaining() {
      AsyncEngineConfig config =
          new AsyncEngineConfig()
              .setMaxParallelThreads(4)
              .setCompilationTimeout(Duration.ofMinutes(2))
              .setProgressTracking(true)
              .setBatchCompilation(true)
              .setBatchSize(8);

      assertEquals(4, config.getMaxParallelThreads(), "Threads should be 4");
      assertEquals(
          Duration.ofMinutes(2), config.getCompilationTimeout(), "Timeout should be 2 min");
      assertTrue(config.isProgressTrackingEnabled(), "Progress tracking should be enabled");
      assertTrue(config.isBatchCompilationEnabled(), "Batch compilation should be enabled");
      assertEquals(8, config.getBatchSize(), "Batch size should be 8");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should produce non-null toString output")
    void shouldProduceNonNullToStringOutput() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      assertNotNull(config.toString(), "toString should not return null");
    }

    @Test
    @DisplayName("should include class name in toString")
    void shouldIncludeClassNameInToString() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      assertTrue(
          config.toString().contains("AsyncEngineConfig"), "toString should contain class name");
    }

    @Test
    @DisplayName("should include key fields in toString")
    void shouldIncludeKeyFieldsInToString() {
      AsyncEngineConfig config = new AsyncEngineConfig();

      String str = config.toString();
      assertTrue(str.contains("threads="), "toString should contain threads");
      assertTrue(str.contains("timeout="), "toString should contain timeout");
      assertTrue(str.contains("caching="), "toString should contain caching");
      assertTrue(str.contains("batching="), "toString should contain batching");
    }
  }
}
