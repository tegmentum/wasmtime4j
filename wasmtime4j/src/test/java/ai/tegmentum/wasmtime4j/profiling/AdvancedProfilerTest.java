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

package ai.tegmentum.wasmtime4j.profiling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.profiling.AdvancedProfiler.ProfilerConfiguration;
import ai.tegmentum.wasmtime4j.profiling.AdvancedProfiler.ProfilingSession;
import ai.tegmentum.wasmtime4j.profiling.AdvancedProfiler.ProfilingStatistics;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for AdvancedProfiler. */
@DisplayName("AdvancedProfiler Tests")
class AdvancedProfilerTest {

  private static final Logger LOGGER = Logger.getLogger(AdvancedProfilerTest.class.getName());

  private AdvancedProfiler profiler;
  private ProfilerConfiguration config;

  @BeforeEach
  void setUp() {
    LOGGER.info("Setting up AdvancedProfiler test");
    config =
        ProfilerConfiguration.builder()
            .samplingInterval(Duration.ofMillis(10))
            .maxSamples(1000)
            .enableMemoryProfiling(true)
            .enableFlameGraphs(true)
            .build();
    profiler = new AdvancedProfiler(config);
  }

  @AfterEach
  void tearDown() {
    LOGGER.info("Tearing down AdvancedProfiler test");
    if (profiler != null && !profiler.isClosed()) {
      profiler.close();
    }
  }

  @Nested
  @DisplayName("Construction Tests")
  class ConstructionTests {

    @Test
    @DisplayName("Should create profiler with valid configuration")
    void shouldCreateProfilerWithValidConfiguration() {
      LOGGER.info("Testing profiler creation with valid configuration");

      assertThat(profiler).isNotNull();
      assertThat(profiler.isClosed()).isFalse();
      assertThat(profiler.getConfiguration()).isEqualTo(config);
    }

    @Test
    @DisplayName("Should reject null configuration")
    void shouldRejectNullConfiguration() {
      LOGGER.info("Testing profiler rejection of null configuration");

      assertThatThrownBy(() -> new AdvancedProfiler(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Configuration cannot be null");
    }

    @Test
    @DisplayName("Should create profiler with default configuration values")
    void shouldCreateProfilerWithDefaultConfigurationValues() {
      LOGGER.info("Testing profiler with default configuration values");

      ProfilerConfiguration defaultConfig = ProfilerConfiguration.builder().build();
      AdvancedProfiler defaultProfiler = new AdvancedProfiler(defaultConfig);

      try {
        assertThat(defaultConfig.getSamplingInterval()).isEqualTo(Duration.ofMillis(10));
        assertThat(defaultConfig.getMaxSamples()).isEqualTo(10000);
        assertThat(defaultConfig.isEnableMemoryProfiling()).isFalse();
        assertThat(defaultConfig.isEnableFlameGraphs()).isFalse();
        assertThat(defaultConfig.isEnableJfrIntegration()).isFalse();
        assertThat(defaultConfig.isEnableStackTraceCollection()).isFalse();
      } finally {
        defaultProfiler.close();
      }
    }
  }

  @Nested
  @DisplayName("Profiling Session Tests")
  class ProfilingSessionTests {

    @Test
    @DisplayName("Should start profiling session with valid timeout")
    void shouldStartProfilingSessionWithValidTimeout() {
      LOGGER.info("Testing profiling session start");

      ProfilingSession session = profiler.startProfiling(Duration.ofMinutes(5));

      assertThat(session).isNotNull();
      assertThat(session.isClosed()).isFalse();
      assertThat(session.getStartTime()).isNotNull();
      assertThat(session.isTimedOut()).isFalse();

      session.close();
      assertThat(session.isClosed()).isTrue();
    }

    @Test
    @DisplayName("Should reject null timeout")
    void shouldRejectNullTimeout() {
      LOGGER.info("Testing rejection of null timeout");

      assertThatThrownBy(() -> profiler.startProfiling(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("positive duration");
    }

    @Test
    @DisplayName("Should reject negative timeout")
    void shouldRejectNegativeTimeout() {
      LOGGER.info("Testing rejection of negative timeout");

      assertThatThrownBy(() -> profiler.startProfiling(Duration.ofSeconds(-1)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("positive duration");
    }

    @Test
    @DisplayName("Should reject session start on closed profiler")
    void shouldRejectSessionStartOnClosedProfiler() {
      LOGGER.info("Testing rejection of session start on closed profiler");

      profiler.close();

      assertThatThrownBy(() -> profiler.startProfiling(Duration.ofMinutes(1)))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("closed");
    }

    @Test
    @DisplayName("Should track elapsed time correctly")
    void shouldTrackElapsedTimeCorrectly() throws InterruptedException {
      LOGGER.info("Testing elapsed time tracking");

      ProfilingSession session = profiler.startProfiling(Duration.ofMinutes(5));
      Thread.sleep(100);

      Duration elapsed = session.getElapsedTime();
      assertThat(elapsed.toMillis()).isGreaterThanOrEqualTo(100);

      session.close();
    }

    @Test
    @DisplayName("Should detect timeout correctly")
    void shouldDetectTimeoutCorrectly() throws InterruptedException {
      LOGGER.info("Testing timeout detection");

      ProfilingSession session = profiler.startProfiling(Duration.ofMillis(50));
      assertThat(session.isTimedOut()).isFalse();

      Thread.sleep(100);
      assertThat(session.isTimedOut()).isTrue();

      session.close();
    }
  }

  @Nested
  @DisplayName("Profile Operation Tests")
  class ProfileOperationTests {

    @Test
    @DisplayName("Should profile operation and return result")
    void shouldProfileOperationAndReturnResult() {
      LOGGER.info("Testing operation profiling");

      AtomicInteger counter = new AtomicInteger(0);
      Integer result =
          profiler.profileOperation(
              "testOperation",
              () -> {
                counter.incrementAndGet();
                return 42;
              },
              "JNI");

      assertThat(result).isEqualTo(42);
      assertThat(counter.get()).isEqualTo(1);
      assertThat(profiler.getSampleCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should reject null operation name")
    void shouldRejectNullOperationName() {
      LOGGER.info("Testing rejection of null operation name");

      assertThatThrownBy(() -> profiler.profileOperation(null, () -> "result", "JNI"))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Name cannot be null");
    }

    @Test
    @DisplayName("Should reject null operation")
    void shouldRejectNullOperation() {
      LOGGER.info("Testing rejection of null operation");

      assertThatThrownBy(() -> profiler.profileOperation("test", null, "JNI"))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Operation cannot be null");
    }

    @Test
    @DisplayName("Should still execute operation when profiler is closed")
    void shouldStillExecuteOperationWhenProfilerIsClosed() {
      LOGGER.info("Testing operation execution when profiler is closed");

      profiler.close();

      AtomicInteger counter = new AtomicInteger(0);
      Integer result =
          profiler.profileOperation(
              "testOp",
              () -> {
                counter.incrementAndGet();
                return 99;
              },
              "PANAMA");

      assertThat(result).isEqualTo(99);
      assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle null runtime type")
    void shouldHandleNullRuntimeType() {
      LOGGER.info("Testing null runtime type handling");

      Integer result = profiler.profileOperation("testOp", () -> 123, null);

      assertThat(result).isEqualTo(123);
      assertThat(profiler.getSampleCount()).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("Function Recording Tests")
  class FunctionRecordingTests {

    @Test
    @DisplayName("Should record function execution")
    void shouldRecordFunctionExecution() {
      LOGGER.info("Testing function execution recording");

      profiler.recordFunctionExecution("testFunc", Duration.ofMillis(50), 1024, "JNI");

      assertThat(profiler.getSampleCount()).isEqualTo(1);
      assertThat(profiler.getFunctionRecordsSnapshot()).hasSize(1);
    }

    @Test
    @DisplayName("Should enforce max samples limit")
    void shouldEnforceMaxSamplesLimit() {
      LOGGER.info("Testing max samples limit enforcement");

      ProfilerConfiguration smallConfig = ProfilerConfiguration.builder().maxSamples(5).build();
      AdvancedProfiler smallProfiler = new AdvancedProfiler(smallConfig);

      try {
        for (int i = 0; i < 10; i++) {
          smallProfiler.recordFunctionExecution("func" + i, Duration.ofMillis(1), 100, "JNI");
        }

        assertThat(smallProfiler.getFunctionRecordsSnapshot()).hasSize(5);
      } finally {
        smallProfiler.close();
      }
    }

    @Test
    @DisplayName("Should not record when profiler is closed")
    void shouldNotRecordWhenProfilerIsClosed() {
      LOGGER.info("Testing recording when profiler is closed");

      profiler.close();
      profiler.recordFunctionExecution("testFunc", Duration.ofMillis(10), 512, "JNI");

      assertThat(profiler.getFunctionRecordsSnapshot()).isEmpty();
    }
  }

  @Nested
  @DisplayName("Memory Allocation Tests")
  class MemoryAllocationTests {

    @Test
    @DisplayName("Should record memory allocation when enabled")
    void shouldRecordMemoryAllocationWhenEnabled() {
      LOGGER.info("Testing memory allocation recording");

      long allocId = profiler.recordMemoryAllocation(4096, "testAlloc");

      assertThat(allocId).isPositive();
      assertThat(profiler.getAllocationCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should track allocation and deallocation")
    void shouldTrackAllocationAndDeallocation() {
      LOGGER.info("Testing allocation and deallocation tracking");

      long allocId = profiler.recordMemoryAllocation(8192, "buffer");
      assertThat(profiler.getAllocationCount()).isEqualTo(1);

      profiler.recordMemoryDeallocation(allocId);
      // Allocation count still increments (it's a counter, not current count)
      assertThat(profiler.getAllocationCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return -1 when profiler is closed")
    void shouldReturnNegativeOneWhenProfilerIsClosed() {
      LOGGER.info("Testing allocation when profiler is closed");

      profiler.close();
      long allocId = profiler.recordMemoryAllocation(1024, "test");

      assertThat(allocId).isEqualTo(-1);
    }

    @Test
    @DisplayName("Should not record allocation when memory profiling is disabled")
    void shouldNotRecordAllocationWhenMemoryProfilingIsDisabled() {
      LOGGER.info("Testing allocation when memory profiling is disabled");

      ProfilerConfiguration noMemConfig =
          ProfilerConfiguration.builder().enableMemoryProfiling(false).build();
      AdvancedProfiler noMemProfiler = new AdvancedProfiler(noMemConfig);

      try {
        long allocId = noMemProfiler.recordMemoryAllocation(1024, "test");
        // Allocation ID is still generated, but not tracked
        assertThat(allocId).isPositive();
      } finally {
        noMemProfiler.close();
      }
    }
  }

  @Nested
  @DisplayName("Statistics Tests")
  class StatisticsTests {

    @Test
    @DisplayName("Should collect statistics from session")
    void shouldCollectStatisticsFromSession() {
      LOGGER.info("Testing statistics collection");

      profiler.recordFunctionExecution("func1", Duration.ofMillis(10), 100, "JNI");
      profiler.recordFunctionExecution("func2", Duration.ofMillis(20), 200, "JNI");
      profiler.recordFunctionExecution("func1", Duration.ofMillis(15), 150, "PANAMA");

      ProfilingSession session = profiler.startProfiling(Duration.ofMinutes(1));
      ProfilingStatistics stats = session.getStatistics();

      assertThat(stats.getTotalCalls()).isEqualTo(3);
      assertThat(stats.getTotalTime()).isEqualTo(Duration.ofMillis(45));
      assertThat(stats.getTotalMemory()).isEqualTo(450);

      Map<String, Long> functionCalls = stats.getFunctionCalls();
      assertThat(functionCalls.get("func1")).isEqualTo(2);
      assertThat(functionCalls.get("func2")).isEqualTo(1);

      session.close();
    }

    @Test
    @DisplayName("Should generate flame graph from session")
    void shouldGenerateFlameGraphFromSession() {
      LOGGER.info("Testing flame graph generation from session");

      profiler.recordFunctionExecution("main", Duration.ofMillis(100), 1000, "JNI");
      profiler.recordFunctionExecution("compute", Duration.ofMillis(50), 500, "JNI");

      ProfilingSession session = profiler.startProfiling(Duration.ofMinutes(1));
      FlameGraphGenerator.FlameFrame root = session.generateFlameGraph();

      assertThat(root).isNotNull();
      assertThat(root.getName()).isEqualTo("root");
      assertThat(root.getTotalTime()).isEqualTo(Duration.ofMillis(150));
      assertThat(root.getChildren()).hasSize(2);

      session.close();
    }
  }

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("Should close profiler and clear resources")
    void shouldCloseProfilerAndClearResources() {
      LOGGER.info("Testing profiler close");

      profiler.recordFunctionExecution("test", Duration.ofMillis(10), 100, "JNI");
      profiler.recordMemoryAllocation(1024, "buffer");

      assertThat(profiler.isClosed()).isFalse();
      assertThat(profiler.getFunctionRecordsSnapshot()).isNotEmpty();

      profiler.close();

      assertThat(profiler.isClosed()).isTrue();
      assertThat(profiler.getFunctionRecordsSnapshot()).isEmpty();
    }

    @Test
    @DisplayName("Should be idempotent on multiple closes")
    void shouldBeIdempotentOnMultipleCloses() {
      LOGGER.info("Testing idempotent close");

      profiler.close();
      profiler.close();
      profiler.close();

      assertThat(profiler.isClosed()).isTrue();
    }
  }

  @Nested
  @DisplayName("Configuration Builder Tests")
  class ConfigurationBuilderTests {

    @Test
    @DisplayName("Should build configuration with all options")
    void shouldBuildConfigurationWithAllOptions() {
      LOGGER.info("Testing configuration builder");

      ProfilerConfiguration config =
          ProfilerConfiguration.builder()
              .samplingInterval(Duration.ofNanos(500_000)) // 500 microseconds
              .maxSamples(5000)
              .enableMemoryProfiling(true)
              .enableFlameGraphs(true)
              .enableJfrIntegration(true)
              .enableStackTraceCollection(true)
              .build();

      assertThat(config.getSamplingInterval()).isEqualTo(Duration.ofNanos(500_000));
      assertThat(config.getMaxSamples()).isEqualTo(5000);
      assertThat(config.isEnableMemoryProfiling()).isTrue();
      assertThat(config.isEnableFlameGraphs()).isTrue();
      assertThat(config.isEnableJfrIntegration()).isTrue();
      assertThat(config.isEnableStackTraceCollection()).isTrue();
    }

    @Test
    @DisplayName("Should reject null sampling interval")
    void shouldRejectNullSamplingInterval() {
      LOGGER.info("Testing rejection of null sampling interval");

      assertThatThrownBy(() -> ProfilerConfiguration.builder().samplingInterval(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Interval cannot be null");
    }

    @Test
    @DisplayName("Should reject non-positive max samples")
    void shouldRejectNonPositiveMaxSamples() {
      LOGGER.info("Testing rejection of non-positive max samples");

      assertThatThrownBy(() -> ProfilerConfiguration.builder().maxSamples(0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("positive");

      assertThatThrownBy(() -> ProfilerConfiguration.builder().maxSamples(-1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("positive");
    }
  }
}
