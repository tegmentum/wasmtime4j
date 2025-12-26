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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link MarshalingConfiguration} class.
 *
 * <p>This test class verifies the construction and behavior of the MarshalingConfiguration,
 * including builder pattern, validation, and factory methods.
 */
@DisplayName("MarshalingConfiguration Tests")
class MarshalingConfigurationTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("MarshalingConfiguration should be final")
    void shouldBeFinal() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(MarshalingConfiguration.class.getModifiers()),
          "MarshalingConfiguration should be final");
    }
  }

  @Nested
  @DisplayName("Constants Tests")
  class ConstantsTests {

    @Test
    @DisplayName("DEFAULT_VALUE_THRESHOLD should be 512")
    void defaultValueThresholdShouldBe512() {
      assertEquals(
          512,
          MarshalingConfiguration.DEFAULT_VALUE_THRESHOLD,
          "DEFAULT_VALUE_THRESHOLD should be 512");
    }

    @Test
    @DisplayName("DEFAULT_HYBRID_THRESHOLD should be 2048")
    void defaultHybridThresholdShouldBe2048() {
      assertEquals(
          2048,
          MarshalingConfiguration.DEFAULT_HYBRID_THRESHOLD,
          "DEFAULT_HYBRID_THRESHOLD should be 2048");
    }

    @Test
    @DisplayName("DEFAULT_MAX_DEPTH should be 100")
    void defaultMaxDepthShouldBe100() {
      assertEquals(
          100, MarshalingConfiguration.DEFAULT_MAX_DEPTH, "DEFAULT_MAX_DEPTH should be 100");
    }

    @Test
    @DisplayName("DEFAULT_MEMORY_ALIGNMENT should be 8")
    void defaultMemoryAlignmentShouldBe8() {
      assertEquals(
          8,
          MarshalingConfiguration.DEFAULT_MEMORY_ALIGNMENT,
          "DEFAULT_MEMORY_ALIGNMENT should be 8");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("defaultConfiguration should use default values")
    void defaultConfigurationShouldUseDefaultValues() {
      final MarshalingConfiguration config = MarshalingConfiguration.defaultConfiguration();

      assertEquals(512, config.getValueMarshalingThreshold(), "Value threshold should be default");
      assertEquals(
          2048, config.getHybridMarshalingThreshold(), "Hybrid threshold should be default");
      assertEquals(100, config.getMaxObjectGraphDepth(), "Max depth should be default");
      assertEquals(8, config.getMemoryAlignment(), "Memory alignment should be default");
      assertTrue(
          config.isCircularReferenceDetectionEnabled(),
          "Circular reference detection should be enabled by default");
      assertTrue(config.isTypeValidationEnabled(), "Type validation should be enabled by default");
      assertFalse(
          config.arePerformanceOptimizationsEnabled(),
          "Performance optimizations should be disabled by default");
    }

    @Test
    @DisplayName("performanceOptimized should enable performance optimizations")
    void performanceOptimizedShouldEnablePerformanceOptimizations() {
      final MarshalingConfiguration config = MarshalingConfiguration.performanceOptimized();

      assertEquals(
          1024,
          config.getValueMarshalingThreshold(),
          "Value threshold should be 1024 for performance");
      assertEquals(
          4096,
          config.getHybridMarshalingThreshold(),
          "Hybrid threshold should be 4096 for performance");
      assertTrue(
          config.arePerformanceOptimizationsEnabled(),
          "Performance optimizations should be enabled");
      assertFalse(
          config.isCircularReferenceDetectionEnabled(),
          "Circular reference detection should be disabled for performance");
    }

    @Test
    @DisplayName("safetyOptimized should enable safety features")
    void safetyOptimizedShouldEnableSafetyFeatures() {
      final MarshalingConfiguration config = MarshalingConfiguration.safetyOptimized();

      assertEquals(
          256, config.getValueMarshalingThreshold(), "Value threshold should be 256 for safety");
      assertEquals(
          1024,
          config.getHybridMarshalingThreshold(),
          "Hybrid threshold should be 1024 for safety");
      assertEquals(50, config.getMaxObjectGraphDepth(), "Max depth should be 50 for safety");
      assertTrue(
          config.isCircularReferenceDetectionEnabled(),
          "Circular reference detection should be enabled for safety");
      assertTrue(config.isTypeValidationEnabled(), "Type validation should be enabled for safety");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("builder should return non-null builder")
    void builderShouldReturnNonNullBuilder() {
      assertNotNull(MarshalingConfiguration.builder(), "builder() should return non-null");
    }

    @Test
    @DisplayName("builder should build configuration with custom values")
    void builderShouldBuildConfigurationWithCustomValues() {
      final MarshalingConfiguration config =
          MarshalingConfiguration.builder()
              .withValueMarshalingThreshold(256)
              .withHybridMarshalingThreshold(1024)
              .withMaxObjectGraphDepth(50)
              .withMemoryAlignment(16)
              .withCircularReferenceDetection(false)
              .withTypeValidation(false)
              .withPerformanceOptimizations(true)
              .build();

      assertEquals(256, config.getValueMarshalingThreshold(), "Value threshold should be 256");
      assertEquals(1024, config.getHybridMarshalingThreshold(), "Hybrid threshold should be 1024");
      assertEquals(50, config.getMaxObjectGraphDepth(), "Max depth should be 50");
      assertEquals(16, config.getMemoryAlignment(), "Memory alignment should be 16");
      assertFalse(
          config.isCircularReferenceDetectionEnabled(),
          "Circular reference detection should be disabled");
      assertFalse(config.isTypeValidationEnabled(), "Type validation should be disabled");
      assertTrue(
          config.arePerformanceOptimizationsEnabled(),
          "Performance optimizations should be enabled");
    }

    @Test
    @DisplayName("builder should throw for non-positive value threshold")
    void builderShouldThrowForNonPositiveValueThreshold() {
      assertThrows(
          IllegalArgumentException.class,
          () -> MarshalingConfiguration.builder().withValueMarshalingThreshold(0),
          "Should throw for zero value threshold");

      assertThrows(
          IllegalArgumentException.class,
          () -> MarshalingConfiguration.builder().withValueMarshalingThreshold(-1),
          "Should throw for negative value threshold");
    }

    @Test
    @DisplayName("builder should throw for non-positive hybrid threshold")
    void builderShouldThrowForNonPositiveHybridThreshold() {
      assertThrows(
          IllegalArgumentException.class,
          () -> MarshalingConfiguration.builder().withHybridMarshalingThreshold(0),
          "Should throw for zero hybrid threshold");

      assertThrows(
          IllegalArgumentException.class,
          () -> MarshalingConfiguration.builder().withHybridMarshalingThreshold(-1),
          "Should throw for negative hybrid threshold");
    }

    @Test
    @DisplayName("builder should throw for non-positive max depth")
    void builderShouldThrowForNonPositiveMaxDepth() {
      assertThrows(
          IllegalArgumentException.class,
          () -> MarshalingConfiguration.builder().withMaxObjectGraphDepth(0),
          "Should throw for zero max depth");

      assertThrows(
          IllegalArgumentException.class,
          () -> MarshalingConfiguration.builder().withMaxObjectGraphDepth(-1),
          "Should throw for negative max depth");
    }

    @Test
    @DisplayName("builder should throw for non-power-of-2 alignment")
    void builderShouldThrowForNonPowerOf2Alignment() {
      assertThrows(
          IllegalArgumentException.class,
          () -> MarshalingConfiguration.builder().withMemoryAlignment(3),
          "Should throw for non-power-of-2 alignment (3)");

      assertThrows(
          IllegalArgumentException.class,
          () -> MarshalingConfiguration.builder().withMemoryAlignment(5),
          "Should throw for non-power-of-2 alignment (5)");

      assertThrows(
          IllegalArgumentException.class,
          () -> MarshalingConfiguration.builder().withMemoryAlignment(0),
          "Should throw for zero alignment");

      assertThrows(
          IllegalArgumentException.class,
          () -> MarshalingConfiguration.builder().withMemoryAlignment(-1),
          "Should throw for negative alignment");
    }

    @Test
    @DisplayName("builder should accept valid power-of-2 alignments")
    void builderShouldAcceptValidPowerOf2Alignments() {
      // Test various valid power-of-2 alignments
      assertEquals(
          1,
          MarshalingConfiguration.builder().withMemoryAlignment(1).build().getMemoryAlignment(),
          "Should accept alignment 1");
      assertEquals(
          2,
          MarshalingConfiguration.builder().withMemoryAlignment(2).build().getMemoryAlignment(),
          "Should accept alignment 2");
      assertEquals(
          4,
          MarshalingConfiguration.builder().withMemoryAlignment(4).build().getMemoryAlignment(),
          "Should accept alignment 4");
      assertEquals(
          8,
          MarshalingConfiguration.builder().withMemoryAlignment(8).build().getMemoryAlignment(),
          "Should accept alignment 8");
      assertEquals(
          16,
          MarshalingConfiguration.builder().withMemoryAlignment(16).build().getMemoryAlignment(),
          "Should accept alignment 16");
      assertEquals(
          32,
          MarshalingConfiguration.builder().withMemoryAlignment(32).build().getMemoryAlignment(),
          "Should accept alignment 32");
    }

    @Test
    @DisplayName("builder should throw if hybrid threshold <= value threshold")
    void builderShouldThrowIfHybridThresholdLessThanOrEqualValueThreshold() {
      assertThrows(
          IllegalStateException.class,
          () ->
              MarshalingConfiguration.builder()
                  .withValueMarshalingThreshold(1000)
                  .withHybridMarshalingThreshold(1000)
                  .build(),
          "Should throw if hybrid threshold equals value threshold");

      assertThrows(
          IllegalStateException.class,
          () ->
              MarshalingConfiguration.builder()
                  .withValueMarshalingThreshold(2000)
                  .withHybridMarshalingThreshold(1000)
                  .build(),
          "Should throw if hybrid threshold less than value threshold");
    }
  }

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("getValueMarshalingThreshold should return value threshold")
    void getValueMarshalingThresholdShouldReturnValueThreshold() {
      final MarshalingConfiguration config =
          MarshalingConfiguration.builder().withValueMarshalingThreshold(300).build();

      assertEquals(
          300,
          config.getValueMarshalingThreshold(),
          "getValueMarshalingThreshold should return 300");
    }

    @Test
    @DisplayName("getHybridMarshalingThreshold should return hybrid threshold")
    void getHybridMarshalingThresholdShouldReturnHybridThreshold() {
      final MarshalingConfiguration config =
          MarshalingConfiguration.builder().withHybridMarshalingThreshold(3000).build();

      assertEquals(
          3000,
          config.getHybridMarshalingThreshold(),
          "getHybridMarshalingThreshold should return 3000");
    }

    @Test
    @DisplayName("getMaxObjectGraphDepth should return max depth")
    void getMaxObjectGraphDepthShouldReturnMaxDepth() {
      final MarshalingConfiguration config =
          MarshalingConfiguration.builder().withMaxObjectGraphDepth(75).build();

      assertEquals(75, config.getMaxObjectGraphDepth(), "getMaxObjectGraphDepth should return 75");
    }

    @Test
    @DisplayName("getMemoryAlignment should return alignment")
    void getMemoryAlignmentShouldReturnAlignment() {
      final MarshalingConfiguration config =
          MarshalingConfiguration.builder().withMemoryAlignment(16).build();

      assertEquals(16, config.getMemoryAlignment(), "getMemoryAlignment should return 16");
    }

    @Test
    @DisplayName("isCircularReferenceDetectionEnabled should return flag value")
    void isCircularReferenceDetectionEnabledShouldReturnFlagValue() {
      final MarshalingConfiguration enabled =
          MarshalingConfiguration.builder().withCircularReferenceDetection(true).build();
      final MarshalingConfiguration disabled =
          MarshalingConfiguration.builder().withCircularReferenceDetection(false).build();

      assertTrue(enabled.isCircularReferenceDetectionEnabled(), "Should return true when enabled");
      assertFalse(
          disabled.isCircularReferenceDetectionEnabled(), "Should return false when disabled");
    }

    @Test
    @DisplayName("isTypeValidationEnabled should return flag value")
    void isTypeValidationEnabledShouldReturnFlagValue() {
      final MarshalingConfiguration enabled =
          MarshalingConfiguration.builder().withTypeValidation(true).build();
      final MarshalingConfiguration disabled =
          MarshalingConfiguration.builder().withTypeValidation(false).build();

      assertTrue(enabled.isTypeValidationEnabled(), "Should return true when enabled");
      assertFalse(disabled.isTypeValidationEnabled(), "Should return false when disabled");
    }

    @Test
    @DisplayName("arePerformanceOptimizationsEnabled should return flag value")
    void arePerformanceOptimizationsEnabledShouldReturnFlagValue() {
      final MarshalingConfiguration enabled =
          MarshalingConfiguration.builder().withPerformanceOptimizations(true).build();
      final MarshalingConfiguration disabled =
          MarshalingConfiguration.builder().withPerformanceOptimizations(false).build();

      assertTrue(enabled.arePerformanceOptimizationsEnabled(), "Should return true when enabled");
      assertFalse(
          disabled.arePerformanceOptimizationsEnabled(), "Should return false when disabled");
    }
  }

  @Nested
  @DisplayName("equals and hashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equals should return true for same object")
    void equalsShouldReturnTrueForSameObject() {
      final MarshalingConfiguration config = MarshalingConfiguration.defaultConfiguration();

      assertEquals(config, config, "Same object should be equal");
    }

    @Test
    @DisplayName("equals should return true for equivalent objects")
    void equalsShouldReturnTrueForEquivalentObjects() {
      final MarshalingConfiguration config1 =
          MarshalingConfiguration.builder()
              .withValueMarshalingThreshold(256)
              .withHybridMarshalingThreshold(1024)
              .build();
      final MarshalingConfiguration config2 =
          MarshalingConfiguration.builder()
              .withValueMarshalingThreshold(256)
              .withHybridMarshalingThreshold(1024)
              .build();

      assertEquals(config1, config2, "Equivalent objects should be equal");
    }

    @Test
    @DisplayName("equals should return false for different value thresholds")
    void equalsShouldReturnFalseForDifferentValueThresholds() {
      final MarshalingConfiguration config1 =
          MarshalingConfiguration.builder().withValueMarshalingThreshold(256).build();
      final MarshalingConfiguration config2 =
          MarshalingConfiguration.builder().withValueMarshalingThreshold(512).build();

      assertNotEquals(config1, config2, "Different value thresholds should not be equal");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final MarshalingConfiguration config = MarshalingConfiguration.defaultConfiguration();

      assertNotEquals(null, config, "Should not equal null");
    }

    @Test
    @DisplayName("equals should return false for different type")
    void equalsShouldReturnFalseForDifferentType() {
      final MarshalingConfiguration config = MarshalingConfiguration.defaultConfiguration();

      assertNotEquals("not a config", config, "Should not equal different type");
    }

    @Test
    @DisplayName("hashCode should be consistent for equivalent objects")
    void hashCodeShouldBeConsistentForEquivalentObjects() {
      final MarshalingConfiguration config1 =
          MarshalingConfiguration.builder()
              .withValueMarshalingThreshold(256)
              .withHybridMarshalingThreshold(1024)
              .build();
      final MarshalingConfiguration config2 =
          MarshalingConfiguration.builder()
              .withValueMarshalingThreshold(256)
              .withHybridMarshalingThreshold(1024)
              .build();

      assertEquals(
          config1.hashCode(), config2.hashCode(), "Equivalent objects should have same hashCode");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include value threshold")
    void toStringShouldIncludeValueThreshold() {
      final MarshalingConfiguration config =
          MarshalingConfiguration.builder().withValueMarshalingThreshold(256).build();

      assertTrue(config.toString().contains("256"), "toString should contain value threshold");
    }

    @Test
    @DisplayName("toString should include hybrid threshold")
    void toStringShouldIncludeHybridThreshold() {
      final MarshalingConfiguration config =
          MarshalingConfiguration.builder().withHybridMarshalingThreshold(3000).build();

      assertTrue(config.toString().contains("3000"), "toString should contain hybrid threshold");
    }

    @Test
    @DisplayName("toString should include MarshalingConfiguration identifier")
    void toStringShouldIncludeMarshalingConfigurationIdentifier() {
      final MarshalingConfiguration config = MarshalingConfiguration.defaultConfiguration();

      assertTrue(
          config.toString().contains("MarshalingConfiguration"),
          "toString should contain 'MarshalingConfiguration'");
    }

    @Test
    @DisplayName("toString should return non-null value")
    void toStringShouldReturnNonNullValue() {
      final MarshalingConfiguration config = MarshalingConfiguration.defaultConfiguration();

      assertNotNull(config.toString(), "toString should not return null");
    }
  }
}
