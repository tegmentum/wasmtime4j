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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.config.MarshalingConfiguration;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MarshalingConfiguration} class.
 *
 * <p>MarshalingConfiguration provides settings for complex parameter marshaling including thresholds,
 * memory alignment, circular reference detection, type validation, and performance optimizations.
 */
@DisplayName("MarshalingConfiguration Tests")
class MarshalingConfigurationTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(MarshalingConfiguration.class.getModifiers()),
          "MarshalingConfiguration should be public");
      assertTrue(
          Modifier.isFinal(MarshalingConfiguration.class.getModifiers()),
          "MarshalingConfiguration should be final");
    }

    @Test
    @DisplayName("should expose public constants")
    void shouldExposePublicConstants() {
      assertEquals(
          512, MarshalingConfiguration.DEFAULT_VALUE_THRESHOLD,
          "DEFAULT_VALUE_THRESHOLD should be 512");
      assertEquals(
          2048, MarshalingConfiguration.DEFAULT_HYBRID_THRESHOLD,
          "DEFAULT_HYBRID_THRESHOLD should be 2048");
      assertEquals(
          100, MarshalingConfiguration.DEFAULT_MAX_DEPTH,
          "DEFAULT_MAX_DEPTH should be 100");
      assertEquals(
          8, MarshalingConfiguration.DEFAULT_MEMORY_ALIGNMENT,
          "DEFAULT_MEMORY_ALIGNMENT should be 8");
    }
  }

  @Nested
  @DisplayName("Default Configuration Tests")
  class DefaultConfigurationTests {

    @Test
    @DisplayName("defaultConfiguration should use constant defaults")
    void defaultConfigurationShouldUseDefaults() {
      final MarshalingConfiguration config = MarshalingConfiguration.defaultConfiguration();

      assertNotNull(config, "defaultConfiguration should not return null");
      assertEquals(
          MarshalingConfiguration.DEFAULT_VALUE_THRESHOLD,
          config.getValueMarshalingThreshold(),
          "Default value threshold should match constant");
      assertEquals(
          MarshalingConfiguration.DEFAULT_HYBRID_THRESHOLD,
          config.getHybridMarshalingThreshold(),
          "Default hybrid threshold should match constant");
      assertEquals(
          MarshalingConfiguration.DEFAULT_MAX_DEPTH,
          config.getMaxObjectGraphDepth(),
          "Default max depth should match constant");
      assertEquals(
          MarshalingConfiguration.DEFAULT_MEMORY_ALIGNMENT,
          config.getMemoryAlignment(),
          "Default memory alignment should match constant");
      assertTrue(
          config.isCircularReferenceDetectionEnabled(),
          "Circular reference detection should be enabled by default");
      assertTrue(
          config.isTypeValidationEnabled(),
          "Type validation should be enabled by default");
      assertFalse(
          config.arePerformanceOptimizationsEnabled(),
          "Performance optimizations should be disabled by default");
    }
  }

  @Nested
  @DisplayName("Performance Optimized Configuration Tests")
  class PerformanceOptimizedTests {

    @Test
    @DisplayName("performanceOptimized should enable performance and disable circular ref")
    void performanceOptimizedShouldConfigure() {
      final MarshalingConfiguration config = MarshalingConfiguration.performanceOptimized();

      assertNotNull(config, "performanceOptimized should not return null");
      assertEquals(
          1024, config.getValueMarshalingThreshold(),
          "Performance value threshold should be 1024");
      assertEquals(
          4096, config.getHybridMarshalingThreshold(),
          "Performance hybrid threshold should be 4096");
      assertTrue(
          config.arePerformanceOptimizationsEnabled(),
          "Performance optimizations should be enabled");
      assertFalse(
          config.isCircularReferenceDetectionEnabled(),
          "Circular ref detection should be disabled for performance");
    }
  }

  @Nested
  @DisplayName("Safety Optimized Configuration Tests")
  class SafetyOptimizedTests {

    @Test
    @DisplayName("safetyOptimized should enable safety features")
    void safetyOptimizedShouldConfigureSafety() {
      final MarshalingConfiguration config = MarshalingConfiguration.safetyOptimized();

      assertNotNull(config, "safetyOptimized should not return null");
      assertEquals(
          256, config.getValueMarshalingThreshold(),
          "Safety value threshold should be 256");
      assertEquals(
          1024, config.getHybridMarshalingThreshold(),
          "Safety hybrid threshold should be 1024");
      assertEquals(
          50, config.getMaxObjectGraphDepth(),
          "Safety max depth should be 50");
      assertTrue(
          config.isCircularReferenceDetectionEnabled(),
          "Circular ref detection should be enabled");
      assertTrue(
          config.isTypeValidationEnabled(),
          "Type validation should be enabled");
    }
  }

  @Nested
  @DisplayName("Builder Fluent API Tests")
  class BuilderFluentApiTests {

    @Test
    @DisplayName("should set value marshaling threshold")
    void shouldSetValueMarshalingThreshold() {
      final MarshalingConfiguration config = MarshalingConfiguration.builder()
          .withValueMarshalingThreshold(100)
          .build();

      assertEquals(
          100, config.getValueMarshalingThreshold(),
          "Value threshold should be 100");
    }

    @Test
    @DisplayName("should set hybrid marshaling threshold")
    void shouldSetHybridMarshalingThreshold() {
      final MarshalingConfiguration config = MarshalingConfiguration.builder()
          .withValueMarshalingThreshold(100)
          .withHybridMarshalingThreshold(500)
          .build();

      assertEquals(
          500, config.getHybridMarshalingThreshold(),
          "Hybrid threshold should be 500");
    }

    @Test
    @DisplayName("should set max object graph depth")
    void shouldSetMaxObjectGraphDepth() {
      final MarshalingConfiguration config = MarshalingConfiguration.builder()
          .withMaxObjectGraphDepth(200)
          .build();

      assertEquals(
          200, config.getMaxObjectGraphDepth(),
          "Max depth should be 200");
    }

    @Test
    @DisplayName("should set memory alignment as power of 2")
    void shouldSetMemoryAlignment() {
      final MarshalingConfiguration config = MarshalingConfiguration.builder()
          .withMemoryAlignment(16)
          .build();

      assertEquals(16, config.getMemoryAlignment(), "Memory alignment should be 16");
    }

    @Test
    @DisplayName("should toggle circular reference detection")
    void shouldToggleCircularReferenceDetection() {
      final MarshalingConfiguration config = MarshalingConfiguration.builder()
          .withCircularReferenceDetection(false)
          .build();

      assertFalse(
          config.isCircularReferenceDetectionEnabled(),
          "Circular ref detection should be disabled");
    }

    @Test
    @DisplayName("should toggle type validation")
    void shouldToggleTypeValidation() {
      final MarshalingConfiguration config = MarshalingConfiguration.builder()
          .withTypeValidation(false)
          .build();

      assertFalse(config.isTypeValidationEnabled(), "Type validation should be disabled");
    }

    @Test
    @DisplayName("should toggle performance optimizations")
    void shouldTogglePerformanceOptimizations() {
      final MarshalingConfiguration config = MarshalingConfiguration.builder()
          .withPerformanceOptimizations(true)
          .build();

      assertTrue(
          config.arePerformanceOptimizationsEnabled(),
          "Performance optimizations should be enabled");
    }
  }

  @Nested
  @DisplayName("Builder Validation Tests")
  class BuilderValidationTests {

    @Test
    @DisplayName("should reject non-positive value threshold")
    void shouldRejectNonPositiveValueThreshold() {
      assertThrows(
          IllegalArgumentException.class,
          () -> MarshalingConfiguration.builder().withValueMarshalingThreshold(0),
          "Zero value threshold should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("should reject non-positive hybrid threshold")
    void shouldRejectNonPositiveHybridThreshold() {
      assertThrows(
          IllegalArgumentException.class,
          () -> MarshalingConfiguration.builder().withHybridMarshalingThreshold(-1),
          "Negative hybrid threshold should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("should reject non-positive max depth")
    void shouldRejectNonPositiveMaxDepth() {
      assertThrows(
          IllegalArgumentException.class,
          () -> MarshalingConfiguration.builder().withMaxObjectGraphDepth(0),
          "Zero max depth should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("should reject non-power-of-2 memory alignment")
    void shouldRejectNonPowerOf2Alignment() {
      assertThrows(
          IllegalArgumentException.class,
          () -> MarshalingConfiguration.builder().withMemoryAlignment(3),
          "Non-power-of-2 alignment should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("should reject hybrid threshold <= value threshold on build")
    void shouldRejectHybridNotGreaterThanValue() {
      assertThrows(
          IllegalStateException.class,
          () -> MarshalingConfiguration.builder()
              .withValueMarshalingThreshold(1000)
              .withHybridMarshalingThreshold(500)
              .build(),
          "Hybrid threshold <= value threshold should throw on build");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equal configurations should be equal")
    void equalConfigsShouldBeEqual() {
      final MarshalingConfiguration config1 = MarshalingConfiguration.defaultConfiguration();
      final MarshalingConfiguration config2 = MarshalingConfiguration.defaultConfiguration();

      assertEquals(config1, config2, "Default configs should be equal");
      assertEquals(
          config1.hashCode(), config2.hashCode(),
          "Equal objects should have same hashCode");
    }

    @Test
    @DisplayName("different configurations should not be equal")
    void differentConfigsShouldNotBeEqual() {
      final MarshalingConfiguration config1 = MarshalingConfiguration.defaultConfiguration();
      final MarshalingConfiguration config2 = MarshalingConfiguration.performanceOptimized();

      assertNotEquals(config1, config2, "Different configs should not be equal");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain key field values")
    void toStringShouldContainFieldValues() {
      final MarshalingConfiguration config = MarshalingConfiguration.defaultConfiguration();
      final String result = config.toString();

      assertNotNull(result, "toString should not return null");
      assertTrue(
          result.contains("MarshalingConfiguration"),
          "toString should contain class name");
      assertTrue(result.contains("512"), "toString should contain value threshold");
      assertTrue(result.contains("2048"), "toString should contain hybrid threshold");
    }
  }
}
