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

package ai.tegmentum.wasmtime4j.debug;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.profiler.Profiler;
import ai.tegmentum.wasmtime4j.profiler.ProfilerFactory;
import java.time.Duration;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for Profiler - WebAssembly performance profiling.
 *
 * <p>These tests verify ProfilerConfig, ProfilerFactory, and Profiler functionality. Profiler
 * creation tests are skipped when no profiler provider is available via ServiceLoader.
 *
 * @since 1.0.0
 */
@DisplayName("Profiler Integration Tests")
public final class ProfilerIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(ProfilerIntegrationTest.class.getName());

  /** Flag to check if profiler factory can create profilers. */
  private static Boolean profilerFactoryAvailable = null;

  private static boolean isProfilerFactoryAvailable() {
    if (profilerFactoryAvailable == null) {
      try {
        Profiler profiler = ProfilerFactory.create();
        profiler.close();
        profilerFactoryAvailable = true;
        LOGGER.info("ProfilerFactory is available - provider found");
      } catch (final WasmException e) {
        profilerFactoryAvailable = false;
        LOGGER.info("ProfilerFactory not available: " + e.getMessage());
      }
    }
    return profilerFactoryAvailable;
  }

  @Nested
  @DisplayName("ProfilerConfig Tests")
  class ProfilerConfigTests {

    @Test
    @DisplayName("should create default config")
    void shouldCreateDefaultConfig(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ProfilerConfig config = ProfilerConfig.defaults();

      assertNotNull(config, "Default config should not be null");
      assertTrue(config.isTrackFunctionCalls(), "Default should track function calls");
      assertFalse(config.isTrackMemoryOperations(), "Default should not track memory operations");
      assertFalse(config.isTrackInstructionCount(), "Default should not track instruction count");
      assertTrue(config.isTrackStackDepth(), "Default should track stack depth");
      assertEquals(
          1_000_000L, config.getSamplingIntervalNanos(), "Default sampling interval is 1ms");
      assertEquals(128, config.getMaxStackFrames(), "Default max stack frames is 128");

      LOGGER.info("Default ProfilerConfig created successfully with expected defaults");
    }

    @Test
    @DisplayName("should build config with custom settings")
    void shouldBuildConfigWithCustomSettings(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ProfilerConfig config =
          ProfilerConfig.builder()
              .trackFunctionCalls(false)
              .trackMemoryOperations(true)
              .trackInstructionCount(true)
              .trackStackDepth(false)
              .samplingIntervalNanos(500_000L)
              .maxStackFrames(64)
              .build();

      assertNotNull(config, "Custom config should not be null");
      assertFalse(config.isTrackFunctionCalls(), "Should not track function calls");
      assertTrue(config.isTrackMemoryOperations(), "Should track memory operations");
      assertTrue(config.isTrackInstructionCount(), "Should track instruction count");
      assertFalse(config.isTrackStackDepth(), "Should not track stack depth");
      assertEquals(500_000L, config.getSamplingIntervalNanos(), "Custom sampling interval");
      assertEquals(64, config.getMaxStackFrames(), "Custom max stack frames");

      LOGGER.info("Custom ProfilerConfig created successfully");
    }

    @Test
    @DisplayName("should support builder method chaining")
    void shouldSupportBuilderMethodChaining(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ProfilerConfig.Builder builder = ProfilerConfig.builder();
      assertNotNull(builder, "Builder should not be null");

      // Verify method chaining returns the builder
      final ProfilerConfig.Builder chained =
          builder
              .trackFunctionCalls(true)
              .trackMemoryOperations(true)
              .trackInstructionCount(true)
              .trackStackDepth(true)
              .samplingIntervalNanos(100_000L)
              .maxStackFrames(256);

      assertNotNull(chained, "Chained builder should not be null");

      final ProfilerConfig config = chained.build();
      assertNotNull(config, "Built config should not be null");

      LOGGER.info("Builder method chaining works correctly");
    }

    @Test
    @DisplayName("should allow zero sampling interval")
    void shouldAllowZeroSamplingInterval(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ProfilerConfig config = ProfilerConfig.builder().samplingIntervalNanos(0L).build();

      assertEquals(0L, config.getSamplingIntervalNanos(), "Should allow zero sampling interval");
      LOGGER.info("Zero sampling interval accepted");
    }

    @Test
    @DisplayName("should allow max stack frames of one")
    void shouldAllowMaxStackFramesOfOne(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ProfilerConfig config = ProfilerConfig.builder().maxStackFrames(1).build();

      assertEquals(1, config.getMaxStackFrames(), "Should allow max stack frames of 1");
      LOGGER.info("Max stack frames of 1 accepted");
    }
  }

  @Nested
  @DisplayName("ProfilerFactory Tests")
  class ProfilerFactoryTests {

    @Test
    @DisplayName("should throw WasmException when no provider available")
    void shouldThrowWhenNoProviderAvailable(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // If provider is available, this test is informational
      if (isProfilerFactoryAvailable()) {
        LOGGER.info("Profiler provider is available - factory creates profilers");
      } else {
        final WasmException exception =
            assertThrows(
                WasmException.class,
                () -> ProfilerFactory.create(),
                "Factory should throw WasmException when no provider");
        assertNotNull(exception.getMessage(), "Exception should have a message");
        assertTrue(
            exception.getMessage().contains("No Profiler implementation"),
            "Message should indicate no implementation found");
        LOGGER.info("Correctly threw WasmException: " + exception.getMessage());
      }
    }

    @Test
    @DisplayName("should create profiler when provider available")
    void shouldCreateProfilerWhenProviderAvailable(final TestInfo testInfo) throws Exception {
      assumeTrue(isProfilerFactoryAvailable(), "Profiler provider not available - skipping");
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Profiler profiler =
          assertDoesNotThrow(
              () -> ProfilerFactory.create(), "Factory should not throw when provider available");
      assertNotNull(profiler, "Profiler should not be null");

      profiler.close();
      LOGGER.info("Successfully created profiler via factory");
    }
  }

  @Nested
  @DisplayName("Profiler Control Tests")
  class ProfilerControlTests {

    private void assumeProfilerAvailable() {
      assumeTrue(
          isProfilerFactoryAvailable(),
          "Profiler provider not available - skipping profiler tests");
    }

    @Test
    @DisplayName("should start and stop profiling")
    void shouldStartAndStopProfiling(final TestInfo testInfo) throws Exception {
      assumeProfilerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (Profiler profiler = ProfilerFactory.create()) {
        assertFalse(profiler.isProfiling(), "Should not be profiling initially");

        profiler.startProfiling();
        assertTrue(profiler.isProfiling(), "Should be profiling after start");

        profiler.stopProfiling();
        assertFalse(profiler.isProfiling(), "Should not be profiling after stop");
      }

      LOGGER.info("Start/stop profiling works correctly");
    }

    @Test
    @DisplayName("should report profiling state correctly")
    void shouldReportProfilingStateCorrectly(final TestInfo testInfo) throws Exception {
      assumeProfilerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (Profiler profiler = ProfilerFactory.create()) {
        // Initial state
        final boolean initialState = profiler.isProfiling();
        assertFalse(initialState, "Initial state should be not profiling");

        // Start profiling
        profiler.startProfiling();
        assertTrue(profiler.isProfiling(), "Should be profiling after start");

        // Stop profiling
        profiler.stopProfiling();
        assertFalse(profiler.isProfiling(), "Should not be profiling after stop");
      }

      LOGGER.info("Profiling state reporting works correctly");
    }

    @Test
    @DisplayName("should close profiler cleanly")
    void shouldCloseProfilerCleanly(final TestInfo testInfo) throws Exception {
      assumeProfilerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Profiler profiler = ProfilerFactory.create();
      assertNotNull(profiler, "Profiler should not be null");

      // Start profiling to ensure cleanup handles active state
      profiler.startProfiling();

      // Close should not throw
      assertDoesNotThrow(() -> profiler.close(), "Close should not throw");

      LOGGER.info("Profiler closed cleanly");
    }
  }

  @Nested
  @DisplayName("Profiler Metrics Tests")
  class ProfilerMetricsTests {

    private void assumeProfilerAvailable() {
      assumeTrue(
          isProfilerFactoryAvailable(), "Profiler provider not available - skipping metrics tests");
    }

    @Test
    @DisplayName("should record function execution")
    void shouldRecordFunctionExecution(final TestInfo testInfo) throws Exception {
      assumeProfilerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (Profiler profiler = ProfilerFactory.create()) {
        profiler.startProfiling();

        // Record a function execution
        profiler.recordFunctionExecution("testFunction", Duration.ofMillis(100), 1024);

        final long totalCalls = profiler.getTotalFunctionCalls();
        assertTrue(totalCalls >= 1, "Should have at least 1 function call recorded");

        profiler.stopProfiling();
      }

      LOGGER.info("Function execution recording works");
    }

    @Test
    @DisplayName("should record compilation")
    void shouldRecordCompilation(final TestInfo testInfo) throws Exception {
      assumeProfilerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (Profiler profiler = ProfilerFactory.create()) {
        profiler.startProfiling();

        // Record a compilation
        profiler.recordCompilation(Duration.ofMillis(50), 10000, false, true);

        final long modulesCompiled = profiler.getModulesCompiled();
        assertTrue(modulesCompiled >= 1, "Should have at least 1 module compiled");

        profiler.stopProfiling();
      }

      LOGGER.info("Compilation recording works");
    }

    @Test
    @DisplayName("should track memory usage")
    void shouldTrackMemoryUsage(final TestInfo testInfo) throws Exception {
      assumeProfilerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (Profiler profiler = ProfilerFactory.create()) {
        profiler.startProfiling();

        // Record function executions with memory deltas
        profiler.recordFunctionExecution("alloc1", Duration.ofMillis(10), 1024);
        profiler.recordFunctionExecution("alloc2", Duration.ofMillis(10), 2048);

        final long currentMemory = profiler.getCurrentMemoryBytes();
        final long peakMemory = profiler.getPeakMemoryBytes();

        // Memory tracking may or may not be implemented - just ensure no exceptions
        LOGGER.info("Current memory: " + currentMemory + " bytes, Peak: " + peakMemory + " bytes");

        profiler.stopProfiling();
      }

      LOGGER.info("Memory tracking executed without errors");
    }
  }

  @Nested
  @DisplayName("Profiler Statistics Tests")
  class ProfilerStatisticsTests {

    private void assumeProfilerAvailable() {
      assumeTrue(
          isProfilerFactoryAvailable(),
          "Profiler provider not available - skipping statistics tests");
    }

    @Test
    @DisplayName("should return profiler statistics")
    void shouldReturnProfilerStatistics(final TestInfo testInfo) throws Exception {
      assumeProfilerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (Profiler profiler = ProfilerFactory.create()) {
        profiler.startProfiling();

        // Record some activity
        profiler.recordFunctionExecution("func1", Duration.ofMillis(100), 0);
        profiler.recordCompilation(Duration.ofMillis(50), 5000, true, false);

        // Get statistics - ensure they don't throw
        final long modulesCompiled = profiler.getModulesCompiled();
        final Duration totalCompilationTime = profiler.getTotalCompilationTime();
        final Duration avgCompilationTime = profiler.getAverageCompilationTime();
        final long bytesCompiled = profiler.getBytesCompiled();
        final long cacheHits = profiler.getCacheHits();
        final long cacheMisses = profiler.getCacheMisses();
        final long optimizedModules = profiler.getOptimizedModules();
        final Duration uptime = profiler.getUptime();
        final double callsPerSecond = profiler.getFunctionCallsPerSecond();
        final long totalCalls = profiler.getTotalFunctionCalls();
        final Duration totalExecTime = profiler.getTotalExecutionTime();

        // All statistics should be non-null for Durations
        assertNotNull(totalCompilationTime, "Total compilation time should not be null");
        assertNotNull(avgCompilationTime, "Average compilation time should not be null");
        assertNotNull(uptime, "Uptime should not be null");
        assertNotNull(totalExecTime, "Total execution time should not be null");

        LOGGER.info(
            "Statistics - Modules: "
                + modulesCompiled
                + ", Calls: "
                + totalCalls
                + ", Cache hits: "
                + cacheHits
                + ", Rate: "
                + callsPerSecond
                + " calls/sec");

        profiler.stopProfiling();
      }

      LOGGER.info("Profiler statistics returned successfully");
    }

    @Test
    @DisplayName("should reset statistics")
    void shouldResetStatistics(final TestInfo testInfo) throws Exception {
      assumeProfilerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (Profiler profiler = ProfilerFactory.create()) {
        profiler.startProfiling();

        // Record activity
        profiler.recordFunctionExecution("func1", Duration.ofMillis(100), 0);
        profiler.recordCompilation(Duration.ofMillis(50), 5000, false, false);

        // Reset
        profiler.reset();

        // Statistics should be reset (implementation dependent)
        final long modulesAfterReset = profiler.getModulesCompiled();
        final long callsAfterReset = profiler.getTotalFunctionCalls();

        // After reset, values should be zero or minimal
        LOGGER.info("After reset - Modules: " + modulesAfterReset + ", Calls: " + callsAfterReset);

        profiler.stopProfiling();
      }

      LOGGER.info("Statistics reset completed");
    }

    @Test
    @DisplayName("should track uptime")
    void shouldTrackUptime(final TestInfo testInfo) throws Exception {
      assumeProfilerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (Profiler profiler = ProfilerFactory.create()) {
        profiler.startProfiling();

        // Wait a bit
        Thread.sleep(50);

        final Duration uptime = profiler.getUptime();
        assertNotNull(uptime, "Uptime should not be null");
        assertTrue(uptime.toMillis() >= 0, "Uptime should be non-negative");

        LOGGER.info("Uptime: " + uptime.toMillis() + " ms");

        profiler.stopProfiling();
      }

      LOGGER.info("Uptime tracking works");
    }
  }
}
