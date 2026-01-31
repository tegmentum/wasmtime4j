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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DefaultInstanceManagerConfigBuilder}.
 *
 * <p>DefaultInstanceManagerConfigBuilder is a package-private final class that implements {@link
 * InstanceManager.InstanceManagerConfig.Builder}. It provides a fluent API with validation for
 * building {@link InstanceManager.InstanceManagerConfig} instances.
 */
@DisplayName("DefaultInstanceManagerConfigBuilder Tests")
class DefaultInstanceManagerConfigBuilderTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(DefaultInstanceManagerConfigBuilder.class.getModifiers()),
          "DefaultInstanceManagerConfigBuilder should be final");
    }

    @Test
    @DisplayName("should not be public")
    void shouldNotBePublic() {
      assertFalse(
          Modifier.isPublic(DefaultInstanceManagerConfigBuilder.class.getModifiers()),
          "DefaultInstanceManagerConfigBuilder should be package-private");
    }

    @Test
    @DisplayName("should implement Builder interface")
    void shouldImplementBuilderInterface() {
      assertTrue(
          InstanceManager.InstanceManagerConfig.Builder.class.isAssignableFrom(
              DefaultInstanceManagerConfigBuilder.class),
          "DefaultInstanceManagerConfigBuilder should implement"
              + " InstanceManager.InstanceManagerConfig.Builder");
    }

    @Test
    @DisplayName("builder factory method should return Builder instance")
    void builderFactoryMethodShouldReturnBuilderInstance() {
      final InstanceManager.InstanceManagerConfig.Builder builder =
          InstanceManager.InstanceManagerConfig.builder();
      assertNotNull(builder, "builder() should not return null");
      assertTrue(
          builder instanceof DefaultInstanceManagerConfigBuilder,
          "builder() should return a DefaultInstanceManagerConfigBuilder");
    }
  }

  @Nested
  @DisplayName("Fluent API Tests")
  class FluentApiTests {

    @Test
    @DisplayName("defaultPoolSize should return same builder for chaining")
    void defaultPoolSizeShouldReturnSameBuilder() {
      final InstanceManager.InstanceManagerConfig.Builder builder =
          InstanceManager.InstanceManagerConfig.builder();
      final InstanceManager.InstanceManagerConfig.Builder result = builder.defaultPoolSize(5);
      assertSame(builder, result, "defaultPoolSize should return the same builder instance");
    }

    @Test
    @DisplayName("maxPoolSize should return same builder for chaining")
    void maxPoolSizeShouldReturnSameBuilder() {
      final InstanceManager.InstanceManagerConfig.Builder builder =
          InstanceManager.InstanceManagerConfig.builder();
      final InstanceManager.InstanceManagerConfig.Builder result = builder.maxPoolSize(50);
      assertSame(builder, result, "maxPoolSize should return the same builder instance");
    }

    @Test
    @DisplayName("autoScalingEnabled should return same builder for chaining")
    void autoScalingEnabledShouldReturnSameBuilder() {
      final InstanceManager.InstanceManagerConfig.Builder builder =
          InstanceManager.InstanceManagerConfig.builder();
      final InstanceManager.InstanceManagerConfig.Builder result =
          builder.autoScalingEnabled(false);
      assertSame(builder, result, "autoScalingEnabled should return the same builder instance");
    }

    @Test
    @DisplayName("scalingThreshold should return same builder for chaining")
    void scalingThresholdShouldReturnSameBuilder() {
      final InstanceManager.InstanceManagerConfig.Builder builder =
          InstanceManager.InstanceManagerConfig.builder();
      final InstanceManager.InstanceManagerConfig.Builder result = builder.scalingThreshold(0.5);
      assertSame(builder, result, "scalingThreshold should return the same builder instance");
    }

    @Test
    @DisplayName("healthMonitoringEnabled should return same builder for chaining")
    void healthMonitoringEnabledShouldReturnSameBuilder() {
      final InstanceManager.InstanceManagerConfig.Builder builder =
          InstanceManager.InstanceManagerConfig.builder();
      final InstanceManager.InstanceManagerConfig.Builder result =
          builder.healthMonitoringEnabled(false);
      assertSame(
          builder, result, "healthMonitoringEnabled should return the same builder instance");
    }

    @Test
    @DisplayName("healthCheckInterval should return same builder for chaining")
    void healthCheckIntervalShouldReturnSameBuilder() {
      final InstanceManager.InstanceManagerConfig.Builder builder =
          InstanceManager.InstanceManagerConfig.builder();
      final InstanceManager.InstanceManagerConfig.Builder result =
          builder.healthCheckInterval(Duration.ofSeconds(30));
      assertSame(builder, result, "healthCheckInterval should return the same builder instance");
    }

    @Test
    @DisplayName("migrationEnabled should return same builder for chaining")
    void migrationEnabledShouldReturnSameBuilder() {
      final InstanceManager.InstanceManagerConfig.Builder builder =
          InstanceManager.InstanceManagerConfig.builder();
      final InstanceManager.InstanceManagerConfig.Builder result =
          builder.migrationEnabled(false);
      assertSame(builder, result, "migrationEnabled should return the same builder instance");
    }

    @Test
    @DisplayName("checkpointingEnabled should return same builder for chaining")
    void checkpointingEnabledShouldReturnSameBuilder() {
      final InstanceManager.InstanceManagerConfig.Builder builder =
          InstanceManager.InstanceManagerConfig.builder();
      final InstanceManager.InstanceManagerConfig.Builder result =
          builder.checkpointingEnabled(true);
      assertSame(builder, result, "checkpointingEnabled should return the same builder instance");
    }

    @Test
    @DisplayName("instanceTimeout should return same builder for chaining")
    void instanceTimeoutShouldReturnSameBuilder() {
      final InstanceManager.InstanceManagerConfig.Builder builder =
          InstanceManager.InstanceManagerConfig.builder();
      final InstanceManager.InstanceManagerConfig.Builder result =
          builder.instanceTimeout(Duration.ofMinutes(10));
      assertSame(builder, result, "instanceTimeout should return the same builder instance");
    }

    @Test
    @DisplayName("should support full chaining of all setters")
    void shouldSupportFullChaining() {
      final InstanceManager.InstanceManagerConfig config =
          InstanceManager.InstanceManagerConfig.builder()
              .defaultPoolSize(5)
              .maxPoolSize(50)
              .autoScalingEnabled(false)
              .scalingThreshold(0.6)
              .healthMonitoringEnabled(false)
              .healthCheckInterval(Duration.ofSeconds(30))
              .migrationEnabled(false)
              .checkpointingEnabled(true)
              .instanceTimeout(Duration.ofMinutes(10))
              .build();

      assertNotNull(config, "Fully chained build should produce a non-null config");
      assertEquals(5, config.getDefaultPoolSize(), "defaultPoolSize should be 5");
      assertEquals(50, config.getMaxPoolSize(), "maxPoolSize should be 50");
      assertFalse(config.isAutoScalingEnabled(), "autoScaling should be false");
      assertEquals(0.6, config.getScalingThreshold(), 0.001, "scalingThreshold should be 0.6");
      assertFalse(config.isHealthMonitoringEnabled(), "healthMonitoring should be false");
      assertEquals(
          Duration.ofSeconds(30),
          config.getHealthCheckInterval(),
          "healthCheckInterval should be 30s");
      assertFalse(config.isMigrationEnabled(), "migration should be false");
      assertTrue(config.isCheckpointingEnabled(), "checkpointing should be true");
      assertEquals(
          Duration.ofMinutes(10),
          config.getInstanceTimeout(),
          "instanceTimeout should be 10min");
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("should reject zero default pool size")
    void shouldRejectZeroDefaultPoolSize() {
      final InstanceManager.InstanceManagerConfig.Builder builder =
          InstanceManager.InstanceManagerConfig.builder();
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> builder.defaultPoolSize(0),
              "defaultPoolSize(0) should throw IllegalArgumentException");
      assertEquals(
          "Default pool size must be positive",
          exception.getMessage(),
          "Exception message should indicate pool size must be positive");
    }

    @Test
    @DisplayName("should reject negative default pool size")
    void shouldRejectNegativeDefaultPoolSize() {
      final InstanceManager.InstanceManagerConfig.Builder builder =
          InstanceManager.InstanceManagerConfig.builder();
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> builder.defaultPoolSize(-1),
              "defaultPoolSize(-1) should throw IllegalArgumentException");
      assertEquals(
          "Default pool size must be positive",
          exception.getMessage(),
          "Exception message should indicate pool size must be positive");
    }

    @Test
    @DisplayName("should reject zero max pool size")
    void shouldRejectZeroMaxPoolSize() {
      final InstanceManager.InstanceManagerConfig.Builder builder =
          InstanceManager.InstanceManagerConfig.builder();
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> builder.maxPoolSize(0),
              "maxPoolSize(0) should throw IllegalArgumentException");
      assertEquals(
          "Max pool size must be positive",
          exception.getMessage(),
          "Exception message should indicate max pool size must be positive");
    }

    @Test
    @DisplayName("should reject negative max pool size")
    void shouldRejectNegativeMaxPoolSize() {
      final InstanceManager.InstanceManagerConfig.Builder builder =
          InstanceManager.InstanceManagerConfig.builder();
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> builder.maxPoolSize(-1),
              "maxPoolSize(-1) should throw IllegalArgumentException");
      assertEquals(
          "Max pool size must be positive",
          exception.getMessage(),
          "Exception message should indicate max pool size must be positive");
    }

    @Test
    @DisplayName("should reject scaling threshold below 0.0")
    void shouldRejectScalingThresholdBelowZero() {
      final InstanceManager.InstanceManagerConfig.Builder builder =
          InstanceManager.InstanceManagerConfig.builder();
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> builder.scalingThreshold(-0.1),
              "scalingThreshold(-0.1) should throw IllegalArgumentException");
      assertEquals(
          "Scaling threshold must be between 0.0 and 1.0",
          exception.getMessage(),
          "Exception message should indicate valid range");
    }

    @Test
    @DisplayName("should reject scaling threshold above 1.0")
    void shouldRejectScalingThresholdAboveOne() {
      final InstanceManager.InstanceManagerConfig.Builder builder =
          InstanceManager.InstanceManagerConfig.builder();
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> builder.scalingThreshold(1.1),
              "scalingThreshold(1.1) should throw IllegalArgumentException");
      assertEquals(
          "Scaling threshold must be between 0.0 and 1.0",
          exception.getMessage(),
          "Exception message should indicate valid range");
    }

    @Test
    @DisplayName("should accept scaling threshold at boundary 0.0")
    void shouldAcceptScalingThresholdAtZero() {
      final InstanceManager.InstanceManagerConfig config =
          InstanceManager.InstanceManagerConfig.builder().scalingThreshold(0.0).build();
      assertEquals(
          0.0, config.getScalingThreshold(), 0.001, "scalingThreshold 0.0 should be accepted");
    }

    @Test
    @DisplayName("should accept scaling threshold at boundary 1.0")
    void shouldAcceptScalingThresholdAtOne() {
      final InstanceManager.InstanceManagerConfig config =
          InstanceManager.InstanceManagerConfig.builder().scalingThreshold(1.0).build();
      assertEquals(
          1.0, config.getScalingThreshold(), 0.001, "scalingThreshold 1.0 should be accepted");
    }

    @Test
    @DisplayName("should reject null health check interval")
    void shouldRejectNullHealthCheckInterval() {
      final InstanceManager.InstanceManagerConfig.Builder builder =
          InstanceManager.InstanceManagerConfig.builder();
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> builder.healthCheckInterval(null),
              "healthCheckInterval(null) should throw IllegalArgumentException");
      assertEquals(
          "Health check interval must be positive",
          exception.getMessage(),
          "Exception message should indicate interval must be positive");
    }

    @Test
    @DisplayName("should reject negative health check interval")
    void shouldRejectNegativeHealthCheckInterval() {
      final InstanceManager.InstanceManagerConfig.Builder builder =
          InstanceManager.InstanceManagerConfig.builder();
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> builder.healthCheckInterval(Duration.ofSeconds(-1)),
              "healthCheckInterval(negative) should throw IllegalArgumentException");
      assertEquals(
          "Health check interval must be positive",
          exception.getMessage(),
          "Exception message should indicate interval must be positive");
    }

    @Test
    @DisplayName("should accept zero health check interval")
    void shouldAcceptZeroHealthCheckInterval() {
      final InstanceManager.InstanceManagerConfig config =
          InstanceManager.InstanceManagerConfig.builder()
              .healthCheckInterval(Duration.ZERO)
              .build();
      assertEquals(
          Duration.ZERO,
          config.getHealthCheckInterval(),
          "Duration.ZERO should be accepted for health check interval");
    }

    @Test
    @DisplayName("should reject null instance timeout")
    void shouldRejectNullInstanceTimeout() {
      final InstanceManager.InstanceManagerConfig.Builder builder =
          InstanceManager.InstanceManagerConfig.builder();
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> builder.instanceTimeout(null),
              "instanceTimeout(null) should throw IllegalArgumentException");
      assertEquals(
          "Instance timeout must be positive",
          exception.getMessage(),
          "Exception message should indicate timeout must be positive");
    }

    @Test
    @DisplayName("should reject negative instance timeout")
    void shouldRejectNegativeInstanceTimeout() {
      final InstanceManager.InstanceManagerConfig.Builder builder =
          InstanceManager.InstanceManagerConfig.builder();
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> builder.instanceTimeout(Duration.ofSeconds(-1)),
              "instanceTimeout(negative) should throw IllegalArgumentException");
      assertEquals(
          "Instance timeout must be positive",
          exception.getMessage(),
          "Exception message should indicate timeout must be positive");
    }

    @Test
    @DisplayName("should accept zero instance timeout")
    void shouldAcceptZeroInstanceTimeout() {
      final InstanceManager.InstanceManagerConfig config =
          InstanceManager.InstanceManagerConfig.builder()
              .instanceTimeout(Duration.ZERO)
              .build();
      assertEquals(
          Duration.ZERO,
          config.getInstanceTimeout(),
          "Duration.ZERO should be accepted for instance timeout");
    }

    @Test
    @DisplayName("should reject default pool size exceeding max pool size on build")
    void shouldRejectDefaultPoolSizeExceedingMaxOnBuild() {
      final InstanceManager.InstanceManagerConfig.Builder builder =
          InstanceManager.InstanceManagerConfig.builder().defaultPoolSize(50).maxPoolSize(10);
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              builder::build,
              "build() should throw when defaultPoolSize > maxPoolSize");
      assertEquals(
          "Default pool size cannot exceed max pool size",
          exception.getMessage(),
          "Exception message should explain the constraint");
    }

    @Test
    @DisplayName("should accept default pool size equal to max pool size")
    void shouldAcceptDefaultPoolSizeEqualToMax() {
      final InstanceManager.InstanceManagerConfig config =
          InstanceManager.InstanceManagerConfig.builder()
              .defaultPoolSize(50)
              .maxPoolSize(50)
              .build();
      assertEquals(50, config.getDefaultPoolSize(), "defaultPoolSize should be 50");
      assertEquals(50, config.getMaxPoolSize(), "maxPoolSize should be 50");
    }
  }

  @Nested
  @DisplayName("Build Tests")
  class BuildTests {

    @Test
    @DisplayName("should build config with all custom values")
    void shouldBuildConfigWithAllCustomValues() {
      final InstanceManager.InstanceManagerConfig config =
          InstanceManager.InstanceManagerConfig.builder()
              .defaultPoolSize(20)
              .maxPoolSize(200)
              .autoScalingEnabled(false)
              .scalingThreshold(0.9)
              .healthMonitoringEnabled(false)
              .healthCheckInterval(Duration.ofSeconds(45))
              .migrationEnabled(false)
              .checkpointingEnabled(true)
              .instanceTimeout(Duration.ofMinutes(15))
              .build();

      assertEquals(20, config.getDefaultPoolSize(), "defaultPoolSize should be 20");
      assertEquals(200, config.getMaxPoolSize(), "maxPoolSize should be 200");
      assertFalse(config.isAutoScalingEnabled(), "autoScaling should be false");
      assertEquals(0.9, config.getScalingThreshold(), 0.001, "scalingThreshold should be 0.9");
      assertFalse(config.isHealthMonitoringEnabled(), "healthMonitoring should be false");
      assertEquals(
          Duration.ofSeconds(45),
          config.getHealthCheckInterval(),
          "healthCheckInterval should be 45s");
      assertFalse(config.isMigrationEnabled(), "migration should be false");
      assertTrue(config.isCheckpointingEnabled(), "checkpointing should be true");
      assertEquals(
          Duration.ofMinutes(15),
          config.getInstanceTimeout(),
          "instanceTimeout should be 15min");
    }

    @Test
    @DisplayName("builder should be reusable for multiple builds")
    void builderShouldBeReusableForMultipleBuilds() {
      final InstanceManager.InstanceManagerConfig.Builder builder =
          InstanceManager.InstanceManagerConfig.builder().defaultPoolSize(5).maxPoolSize(50);

      final InstanceManager.InstanceManagerConfig config1 = builder.build();
      final InstanceManager.InstanceManagerConfig config2 = builder.build();

      assertNotNull(config1, "First build should produce a config");
      assertNotNull(config2, "Second build should produce a config");
      assertEquals(
          config1.getDefaultPoolSize(),
          config2.getDefaultPoolSize(),
          "Both configs should have the same defaultPoolSize");
    }
  }
}
