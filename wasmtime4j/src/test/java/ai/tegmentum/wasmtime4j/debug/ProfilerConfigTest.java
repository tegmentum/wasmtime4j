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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for ProfilerConfig class.
 *
 * <p>Verifies the builder pattern and configuration options for the guest profiler.
 */
@DisplayName("ProfilerConfig Tests")
class ProfilerConfigTest {

  @Nested
  @DisplayName("Defaults Factory Tests")
  class DefaultsFactoryTests {

    @Test
    @DisplayName("should create config with default values")
    void shouldCreateConfigWithDefaultValues() {
      ProfilerConfig config = ProfilerConfig.defaults();

      assertNotNull(config, "Config should not be null");
      assertTrue(config.isTrackFunctionCalls(), "Track function calls should be true by default");
      assertFalse(config.isTrackMemoryOperations(), "Track memory ops should be false by default");
      assertFalse(config.isTrackInstructionCount(), "Track instruction count should be false");
      assertTrue(config.isTrackStackDepth(), "Track stack depth should be true by default");
      assertEquals(1_000_000L, config.getSamplingIntervalNanos(), "Default sampling interval");
      assertEquals(128, config.getMaxStackFrames(), "Default max stack frames should be 128");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("should create builder")
    void shouldCreateBuilder() {
      ProfilerConfig.Builder builder = ProfilerConfig.builder();

      assertNotNull(builder, "Builder should not be null");
    }

    @Test
    @DisplayName("should build config with all options set")
    void shouldBuildConfigWithAllOptionsSet() {
      ProfilerConfig config =
          ProfilerConfig.builder()
              .trackFunctionCalls(false)
              .trackMemoryOperations(true)
              .trackInstructionCount(true)
              .trackStackDepth(false)
              .samplingIntervalNanos(500_000L)
              .maxStackFrames(64)
              .build();

      assertFalse(config.isTrackFunctionCalls(), "Track function calls should be false");
      assertTrue(config.isTrackMemoryOperations(), "Track memory ops should be true");
      assertTrue(config.isTrackInstructionCount(), "Track instruction count should be true");
      assertFalse(config.isTrackStackDepth(), "Track stack depth should be false");
      assertEquals(500_000L, config.getSamplingIntervalNanos(), "Sampling interval should match");
      assertEquals(64, config.getMaxStackFrames(), "Max stack frames should match");
    }

    @Test
    @DisplayName("should support fluent builder pattern")
    void shouldSupportFluentBuilderPattern() {
      ProfilerConfig.Builder builder =
          ProfilerConfig.builder()
              .trackFunctionCalls(true)
              .trackMemoryOperations(true)
              .trackInstructionCount(true)
              .trackStackDepth(true)
              .samplingIntervalNanos(100_000L)
              .maxStackFrames(256);

      ProfilerConfig config = builder.build();

      assertNotNull(config, "Config should not be null");
      assertTrue(config.isTrackFunctionCalls(), "Track function calls");
      assertTrue(config.isTrackMemoryOperations(), "Track memory ops");
      assertTrue(config.isTrackInstructionCount(), "Track instruction count");
      assertTrue(config.isTrackStackDepth(), "Track stack depth");
      assertEquals(100_000L, config.getSamplingIntervalNanos(), "Sampling interval");
      assertEquals(256, config.getMaxStackFrames(), "Max stack frames");
    }
  }

  @Nested
  @DisplayName("TrackFunctionCalls Tests")
  class TrackFunctionCallsTests {

    @Test
    @DisplayName("should enable function call tracking")
    void shouldEnableFunctionCallTracking() {
      ProfilerConfig config = ProfilerConfig.builder().trackFunctionCalls(true).build();

      assertTrue(config.isTrackFunctionCalls(), "Function call tracking should be enabled");
    }

    @Test
    @DisplayName("should disable function call tracking")
    void shouldDisableFunctionCallTracking() {
      ProfilerConfig config = ProfilerConfig.builder().trackFunctionCalls(false).build();

      assertFalse(config.isTrackFunctionCalls(), "Function call tracking should be disabled");
    }
  }

  @Nested
  @DisplayName("TrackMemoryOperations Tests")
  class TrackMemoryOperationsTests {

    @Test
    @DisplayName("should enable memory operations tracking")
    void shouldEnableMemoryOperationsTracking() {
      ProfilerConfig config = ProfilerConfig.builder().trackMemoryOperations(true).build();

      assertTrue(config.isTrackMemoryOperations(), "Memory operations tracking should be enabled");
    }

    @Test
    @DisplayName("should disable memory operations tracking")
    void shouldDisableMemoryOperationsTracking() {
      ProfilerConfig config = ProfilerConfig.builder().trackMemoryOperations(false).build();

      assertFalse(
          config.isTrackMemoryOperations(), "Memory operations tracking should be disabled");
    }
  }

  @Nested
  @DisplayName("TrackInstructionCount Tests")
  class TrackInstructionCountTests {

    @Test
    @DisplayName("should enable instruction count tracking")
    void shouldEnableInstructionCountTracking() {
      ProfilerConfig config = ProfilerConfig.builder().trackInstructionCount(true).build();

      assertTrue(config.isTrackInstructionCount(), "Instruction count tracking should be enabled");
    }

    @Test
    @DisplayName("should disable instruction count tracking")
    void shouldDisableInstructionCountTracking() {
      ProfilerConfig config = ProfilerConfig.builder().trackInstructionCount(false).build();

      assertFalse(
          config.isTrackInstructionCount(), "Instruction count tracking should be disabled");
    }
  }

  @Nested
  @DisplayName("TrackStackDepth Tests")
  class TrackStackDepthTests {

    @Test
    @DisplayName("should enable stack depth tracking")
    void shouldEnableStackDepthTracking() {
      ProfilerConfig config = ProfilerConfig.builder().trackStackDepth(true).build();

      assertTrue(config.isTrackStackDepth(), "Stack depth tracking should be enabled");
    }

    @Test
    @DisplayName("should disable stack depth tracking")
    void shouldDisableStackDepthTracking() {
      ProfilerConfig config = ProfilerConfig.builder().trackStackDepth(false).build();

      assertFalse(config.isTrackStackDepth(), "Stack depth tracking should be disabled");
    }
  }

  @Nested
  @DisplayName("SamplingIntervalNanos Tests")
  class SamplingIntervalNanosTests {

    @Test
    @DisplayName("should set sampling interval")
    void shouldSetSamplingInterval() {
      ProfilerConfig config = ProfilerConfig.builder().samplingIntervalNanos(2_000_000L).build();

      assertEquals(2_000_000L, config.getSamplingIntervalNanos(), "Sampling interval should match");
    }

    @Test
    @DisplayName("should accept zero sampling interval")
    void shouldAcceptZeroSamplingInterval() {
      ProfilerConfig config = ProfilerConfig.builder().samplingIntervalNanos(0L).build();

      assertEquals(0L, config.getSamplingIntervalNanos(), "Sampling interval should be 0");
    }

    @Test
    @DisplayName("should accept large sampling interval")
    void shouldAcceptLargeSamplingInterval() {
      long largeInterval = 10_000_000_000L; // 10 seconds
      ProfilerConfig config = ProfilerConfig.builder().samplingIntervalNanos(largeInterval).build();

      assertEquals(largeInterval, config.getSamplingIntervalNanos(), "Large interval should match");
    }
  }

  @Nested
  @DisplayName("MaxStackFrames Tests")
  class MaxStackFramesTests {

    @Test
    @DisplayName("should set max stack frames")
    void shouldSetMaxStackFrames() {
      ProfilerConfig config = ProfilerConfig.builder().maxStackFrames(512).build();

      assertEquals(512, config.getMaxStackFrames(), "Max stack frames should match");
    }

    @Test
    @DisplayName("should accept zero max stack frames")
    void shouldAcceptZeroMaxStackFrames() {
      ProfilerConfig config = ProfilerConfig.builder().maxStackFrames(0).build();

      assertEquals(0, config.getMaxStackFrames(), "Max stack frames should be 0");
    }

    @Test
    @DisplayName("should accept large max stack frames")
    void shouldAcceptLargeMaxStackFrames() {
      ProfilerConfig config = ProfilerConfig.builder().maxStackFrames(10000).build();

      assertEquals(10000, config.getMaxStackFrames(), "Large max stack frames should match");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should create high performance profiler config")
    void shouldCreateHighPerformanceProfilerConfig() {
      // Minimal tracking for performance
      ProfilerConfig config =
          ProfilerConfig.builder()
              .trackFunctionCalls(true)
              .trackMemoryOperations(false)
              .trackInstructionCount(false)
              .trackStackDepth(false)
              .samplingIntervalNanos(10_000_000L) // 10ms
              .maxStackFrames(32)
              .build();

      assertTrue(config.isTrackFunctionCalls(), "Should track function calls");
      assertFalse(config.isTrackMemoryOperations(), "Should not track memory");
      assertFalse(config.isTrackInstructionCount(), "Should not track instructions");
      assertFalse(config.isTrackStackDepth(), "Should not track stack depth");
      assertEquals(10_000_000L, config.getSamplingIntervalNanos(), "10ms sampling interval");
      assertEquals(32, config.getMaxStackFrames(), "Small max stack frames");
    }

    @Test
    @DisplayName("should create detailed profiler config")
    void shouldCreateDetailedProfilerConfig() {
      // Full tracking for detailed analysis
      ProfilerConfig config =
          ProfilerConfig.builder()
              .trackFunctionCalls(true)
              .trackMemoryOperations(true)
              .trackInstructionCount(true)
              .trackStackDepth(true)
              .samplingIntervalNanos(100_000L) // 0.1ms
              .maxStackFrames(1024)
              .build();

      assertTrue(config.isTrackFunctionCalls(), "Should track function calls");
      assertTrue(config.isTrackMemoryOperations(), "Should track memory");
      assertTrue(config.isTrackInstructionCount(), "Should track instructions");
      assertTrue(config.isTrackStackDepth(), "Should track stack depth");
      assertEquals(100_000L, config.getSamplingIntervalNanos(), "0.1ms sampling interval");
      assertEquals(1024, config.getMaxStackFrames(), "Large max stack frames");
    }

    @Test
    @DisplayName("should build multiple configs from same builder reference")
    void shouldBuildMultipleConfigsFromSameBuilderReference() {
      ProfilerConfig.Builder builder = ProfilerConfig.builder().trackFunctionCalls(true);

      ProfilerConfig config1 = builder.maxStackFrames(64).build();
      ProfilerConfig config2 = builder.maxStackFrames(128).build();

      assertEquals(64, config1.getMaxStackFrames(), "Config1 should have 64 frames");
      assertEquals(128, config2.getMaxStackFrames(), "Config2 should have 128 frames");
    }
  }
}
