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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentPipelineConfig} class.
 *
 * <p>ComponentPipelineConfig provides configuration for component pipeline processing.
 */
@DisplayName("ComponentPipelineConfig Tests")
class ComponentPipelineConfigTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(ComponentPipelineConfig.class.getModifiers()),
          "ComponentPipelineConfig should be public");
      assertTrue(
          Modifier.isFinal(ComponentPipelineConfig.class.getModifiers()),
          "ComponentPipelineConfig should be final");
    }

    @Test
    @DisplayName("should have builder method")
    void shouldHaveBuilderMethod() throws NoSuchMethodException {
      final Method method = ComponentPipelineConfig.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
    }
  }

  @Nested
  @DisplayName("Default Constructor Tests")
  class DefaultConstructorTests {

    @Test
    @DisplayName("default constructor should set max stages to 10")
    void defaultConstructorShouldSetMaxStagesToTen() {
      final var config = new ComponentPipelineConfig();

      assertEquals(10, config.getMaxStages(), "Default max stages should be 10");
    }

    @Test
    @DisplayName("default constructor should set buffer size to 100")
    void defaultConstructorShouldSetBufferSizeTo100() {
      final var config = new ComponentPipelineConfig();

      assertEquals(100, config.getBufferSize(), "Default buffer size should be 100");
    }

    @Test
    @DisplayName("default constructor should set stage timeout to 60 seconds")
    void defaultConstructorShouldSetStageTimeoutTo60Seconds() {
      final var config = new ComponentPipelineConfig();

      assertEquals(
          Duration.ofSeconds(60), config.getStageTimeout(), "Default timeout should be 60 seconds");
    }

    @Test
    @DisplayName("default constructor should enable parallel processing")
    void defaultConstructorShouldEnableParallelProcessing() {
      final var config = new ComponentPipelineConfig();

      assertTrue(config.isParallelProcessingEnabled(), "Parallel processing should be enabled");
    }

    @Test
    @DisplayName("default constructor should enable backpressure")
    void defaultConstructorShouldEnableBackpressure() {
      final var config = new ComponentPipelineConfig();

      assertTrue(config.isBackpressureEnabled(), "Backpressure should be enabled");
    }

    @Test
    @DisplayName("default constructor should set max concurrency to 4")
    void defaultConstructorShouldSetMaxConcurrencyTo4() {
      final var config = new ComponentPipelineConfig();

      assertEquals(4, config.getMaxConcurrency(), "Default max concurrency should be 4");
    }
  }

  @Nested
  @DisplayName("Parameterized Constructor Tests")
  class ParameterizedConstructorTests {

    @Test
    @DisplayName("should create config with specified values")
    void shouldCreateConfigWithSpecifiedValues() {
      final var config =
          new ComponentPipelineConfig(20, 200, Duration.ofSeconds(120), false, false, 8);

      assertEquals(20, config.getMaxStages(), "Max stages should be 20");
      assertEquals(200, config.getBufferSize(), "Buffer size should be 200");
      assertEquals(Duration.ofSeconds(120), config.getStageTimeout(), "Timeout should be 120s");
      assertFalse(config.isParallelProcessingEnabled(), "Parallel processing should be disabled");
      assertFalse(config.isBackpressureEnabled(), "Backpressure should be disabled");
      assertEquals(8, config.getMaxConcurrency(), "Max concurrency should be 8");
    }

    @Test
    @DisplayName("should throw exception for zero max stages")
    void shouldThrowExceptionForZeroMaxStages() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new ComponentPipelineConfig(0, 100, Duration.ofSeconds(60), true, true, 4),
          "Should throw for zero max stages");
    }

    @Test
    @DisplayName("should throw exception for negative max stages")
    void shouldThrowExceptionForNegativeMaxStages() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new ComponentPipelineConfig(-1, 100, Duration.ofSeconds(60), true, true, 4),
          "Should throw for negative max stages");
    }

    @Test
    @DisplayName("should throw exception for zero buffer size")
    void shouldThrowExceptionForZeroBufferSize() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new ComponentPipelineConfig(10, 0, Duration.ofSeconds(60), true, true, 4),
          "Should throw for zero buffer size");
    }

    @Test
    @DisplayName("should throw exception for negative buffer size")
    void shouldThrowExceptionForNegativeBufferSize() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new ComponentPipelineConfig(10, -1, Duration.ofSeconds(60), true, true, 4),
          "Should throw for negative buffer size");
    }

    @Test
    @DisplayName("should throw exception for zero max concurrency")
    void shouldThrowExceptionForZeroMaxConcurrency() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new ComponentPipelineConfig(10, 100, Duration.ofSeconds(60), true, true, 0),
          "Should throw for zero max concurrency");
    }

    @Test
    @DisplayName("should throw exception for negative max concurrency")
    void shouldThrowExceptionForNegativeMaxConcurrency() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new ComponentPipelineConfig(10, 100, Duration.ofSeconds(60), true, true, -1),
          "Should throw for negative max concurrency");
    }

    @Test
    @DisplayName("should throw exception for null stage timeout")
    void shouldThrowExceptionForNullStageTimeout() {
      assertThrows(
          NullPointerException.class,
          () -> new ComponentPipelineConfig(10, 100, null, true, true, 4),
          "Should throw for null stage timeout");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("builder should create config with defaults")
    void builderShouldCreateConfigWithDefaults() {
      final var config = ComponentPipelineConfig.builder().build();

      assertEquals(10, config.getMaxStages(), "Default max stages should be 10");
      assertEquals(100, config.getBufferSize(), "Default buffer size should be 100");
      assertEquals(
          Duration.ofSeconds(60), config.getStageTimeout(), "Default timeout should be 60s");
      assertTrue(config.isParallelProcessingEnabled(), "Parallel processing should be enabled");
      assertTrue(config.isBackpressureEnabled(), "Backpressure should be enabled");
      assertEquals(4, config.getMaxConcurrency(), "Default max concurrency should be 4");
    }

    @Test
    @DisplayName("builder should set max stages")
    void builderShouldSetMaxStages() {
      final var config = ComponentPipelineConfig.builder().maxStages(50).build();

      assertEquals(50, config.getMaxStages(), "Max stages should be 50");
    }

    @Test
    @DisplayName("builder should set buffer size")
    void builderShouldSetBufferSize() {
      final var config = ComponentPipelineConfig.builder().bufferSize(500).build();

      assertEquals(500, config.getBufferSize(), "Buffer size should be 500");
    }

    @Test
    @DisplayName("builder should set stage timeout")
    void builderShouldSetStageTimeout() {
      final var config =
          ComponentPipelineConfig.builder().stageTimeout(Duration.ofMinutes(5)).build();

      assertEquals(Duration.ofMinutes(5), config.getStageTimeout(), "Timeout should be 5 minutes");
    }

    @Test
    @DisplayName("builder should set parallel processing")
    void builderShouldSetParallelProcessing() {
      final var config = ComponentPipelineConfig.builder().enableParallelProcessing(false).build();

      assertFalse(config.isParallelProcessingEnabled(), "Parallel processing should be disabled");
    }

    @Test
    @DisplayName("builder should set backpressure")
    void builderShouldSetBackpressure() {
      final var config = ComponentPipelineConfig.builder().enableBackpressure(false).build();

      assertFalse(config.isBackpressureEnabled(), "Backpressure should be disabled");
    }

    @Test
    @DisplayName("builder should set max concurrency")
    void builderShouldSetMaxConcurrency() {
      final var config = ComponentPipelineConfig.builder().maxConcurrency(16).build();

      assertEquals(16, config.getMaxConcurrency(), "Max concurrency should be 16");
    }

    @Test
    @DisplayName("builder should allow chaining")
    void builderShouldAllowChaining() {
      final var config =
          ComponentPipelineConfig.builder()
              .maxStages(25)
              .bufferSize(250)
              .stageTimeout(Duration.ofSeconds(90))
              .enableParallelProcessing(false)
              .enableBackpressure(false)
              .maxConcurrency(12)
              .build();

      assertEquals(25, config.getMaxStages(), "Max stages should be 25");
      assertEquals(250, config.getBufferSize(), "Buffer size should be 250");
      assertEquals(Duration.ofSeconds(90), config.getStageTimeout(), "Timeout should be 90s");
      assertFalse(config.isParallelProcessingEnabled(), "Parallel processing should be disabled");
      assertFalse(config.isBackpressureEnabled(), "Backpressure should be disabled");
      assertEquals(12, config.getMaxConcurrency(), "Max concurrency should be 12");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle max int values")
    void shouldHandleMaxIntValues() {
      final var config =
          new ComponentPipelineConfig(
              Integer.MAX_VALUE,
              Integer.MAX_VALUE,
              Duration.ofSeconds(60),
              true,
              true,
              Integer.MAX_VALUE);

      assertEquals(Integer.MAX_VALUE, config.getMaxStages(), "Should handle max int stages");
      assertEquals(Integer.MAX_VALUE, config.getBufferSize(), "Should handle max int buffer");
      assertEquals(
          Integer.MAX_VALUE, config.getMaxConcurrency(), "Should handle max int concurrency");
    }

    @Test
    @DisplayName("should handle very short timeout")
    void shouldHandleVeryShortTimeout() {
      final var config = new ComponentPipelineConfig(10, 100, Duration.ofMillis(1), true, true, 4);

      assertEquals(Duration.ofMillis(1), config.getStageTimeout(), "Should handle 1ms timeout");
    }

    @Test
    @DisplayName("should handle very long timeout")
    void shouldHandleVeryLongTimeout() {
      final var config = new ComponentPipelineConfig(10, 100, Duration.ofDays(365), true, true, 4);

      assertEquals(Duration.ofDays(365), config.getStageTimeout(), "Should handle 365 day timeout");
    }

    @Test
    @DisplayName("multiple builders should create independent configs")
    void multipleBuildsShouldCreateIndependentConfigs() {
      final var config1 =
          ComponentPipelineConfig.builder().maxStages(10).enableParallelProcessing(true).build();

      final var config2 =
          ComponentPipelineConfig.builder().maxStages(20).enableParallelProcessing(false).build();

      assertEquals(10, config1.getMaxStages(), "Config1 should have 10 stages");
      assertEquals(20, config2.getMaxStages(), "Config2 should have 20 stages");
      assertTrue(config1.isParallelProcessingEnabled(), "Config1 should have parallel enabled");
      assertFalse(config2.isParallelProcessingEnabled(), "Config2 should have parallel disabled");
    }
  }
}
