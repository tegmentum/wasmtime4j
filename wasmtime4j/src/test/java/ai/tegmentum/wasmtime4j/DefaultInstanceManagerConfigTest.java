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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DefaultInstanceManagerConfig}.
 *
 * <p>DefaultInstanceManagerConfig is a package-private final class that implements {@link
 * InstanceManager.InstanceManagerConfig}. It stores 9 configuration fields set by the builder and
 * exposes them via getter methods.
 */
@DisplayName("DefaultInstanceManagerConfig Tests")
class DefaultInstanceManagerConfigTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(DefaultInstanceManagerConfig.class.getModifiers()),
          "DefaultInstanceManagerConfig should be final");
    }

    @Test
    @DisplayName("should not be public")
    void shouldNotBePublic() {
      assertFalse(
          Modifier.isPublic(DefaultInstanceManagerConfig.class.getModifiers()),
          "DefaultInstanceManagerConfig should be package-private");
    }

    @Test
    @DisplayName("should implement InstanceManagerConfig")
    void shouldImplementInstanceManagerConfig() {
      assertTrue(
          InstanceManager.InstanceManagerConfig.class.isAssignableFrom(
              DefaultInstanceManagerConfig.class),
          "DefaultInstanceManagerConfig should implement InstanceManager.InstanceManagerConfig");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("should return configured default pool size")
    void shouldReturnConfiguredDefaultPoolSize() {
      final InstanceManager.InstanceManagerConfig config =
          InstanceManager.InstanceManagerConfig.builder().defaultPoolSize(25).build();
      assertEquals(25, config.getDefaultPoolSize(), "getDefaultPoolSize should return 25");
    }

    @Test
    @DisplayName("should return configured max pool size")
    void shouldReturnConfiguredMaxPoolSize() {
      final InstanceManager.InstanceManagerConfig config =
          InstanceManager.InstanceManagerConfig.builder().maxPoolSize(500).build();
      assertEquals(500, config.getMaxPoolSize(), "getMaxPoolSize should return 500");
    }

    @Test
    @DisplayName("should return configured auto scaling enabled")
    void shouldReturnConfiguredAutoScalingEnabled() {
      final InstanceManager.InstanceManagerConfig config =
          InstanceManager.InstanceManagerConfig.builder().autoScalingEnabled(false).build();
      assertFalse(config.isAutoScalingEnabled(), "isAutoScalingEnabled should return false");
    }

    @Test
    @DisplayName("should return configured scaling threshold")
    void shouldReturnConfiguredScalingThreshold() {
      final InstanceManager.InstanceManagerConfig config =
          InstanceManager.InstanceManagerConfig.builder().scalingThreshold(0.5).build();
      assertEquals(
          0.5, config.getScalingThreshold(), 0.001, "getScalingThreshold should return 0.5");
    }

    @Test
    @DisplayName("should return configured health monitoring enabled")
    void shouldReturnConfiguredHealthMonitoringEnabled() {
      final InstanceManager.InstanceManagerConfig config =
          InstanceManager.InstanceManagerConfig.builder().healthMonitoringEnabled(false).build();
      assertFalse(
          config.isHealthMonitoringEnabled(), "isHealthMonitoringEnabled should return false");
    }

    @Test
    @DisplayName("should return configured health check interval")
    void shouldReturnConfiguredHealthCheckInterval() {
      final Duration interval = Duration.ofSeconds(30);
      final InstanceManager.InstanceManagerConfig config =
          InstanceManager.InstanceManagerConfig.builder().healthCheckInterval(interval).build();
      assertEquals(
          interval,
          config.getHealthCheckInterval(),
          "getHealthCheckInterval should return 30 seconds");
    }

    @Test
    @DisplayName("should return configured migration enabled")
    void shouldReturnConfiguredMigrationEnabled() {
      final InstanceManager.InstanceManagerConfig config =
          InstanceManager.InstanceManagerConfig.builder().migrationEnabled(false).build();
      assertFalse(config.isMigrationEnabled(), "isMigrationEnabled should return false");
    }

    @Test
    @DisplayName("should return configured checkpointing enabled")
    void shouldReturnConfiguredCheckpointingEnabled() {
      final InstanceManager.InstanceManagerConfig config =
          InstanceManager.InstanceManagerConfig.builder().checkpointingEnabled(true).build();
      assertTrue(config.isCheckpointingEnabled(), "isCheckpointingEnabled should return true");
    }

    @Test
    @DisplayName("should return configured instance timeout")
    void shouldReturnConfiguredInstanceTimeout() {
      final Duration timeout = Duration.ofMinutes(10);
      final InstanceManager.InstanceManagerConfig config =
          InstanceManager.InstanceManagerConfig.builder().instanceTimeout(timeout).build();
      assertEquals(
          timeout, config.getInstanceTimeout(), "getInstanceTimeout should return 10 minutes");
    }
  }

  @Nested
  @DisplayName("Default Values Tests")
  class DefaultValuesTests {

    @Test
    @DisplayName("default pool size should be 10")
    void defaultPoolSizeShouldBe10() {
      final InstanceManager.InstanceManagerConfig config =
          InstanceManager.InstanceManagerConfig.builder().build();
      assertEquals(10, config.getDefaultPoolSize(), "Default pool size should be 10");
    }

    @Test
    @DisplayName("default max pool size should be 100")
    void defaultMaxPoolSizeShouldBe100() {
      final InstanceManager.InstanceManagerConfig config =
          InstanceManager.InstanceManagerConfig.builder().build();
      assertEquals(100, config.getMaxPoolSize(), "Default max pool size should be 100");
    }

    @Test
    @DisplayName("default auto scaling should be enabled")
    void defaultAutoScalingShouldBeEnabled() {
      final InstanceManager.InstanceManagerConfig config =
          InstanceManager.InstanceManagerConfig.builder().build();
      assertTrue(config.isAutoScalingEnabled(), "Auto scaling should be enabled by default");
    }

    @Test
    @DisplayName("default scaling threshold should be 0.8")
    void defaultScalingThresholdShouldBe08() {
      final InstanceManager.InstanceManagerConfig config =
          InstanceManager.InstanceManagerConfig.builder().build();
      assertEquals(
          0.8, config.getScalingThreshold(), 0.001, "Default scaling threshold should be 0.8");
    }

    @Test
    @DisplayName("default health monitoring should be enabled")
    void defaultHealthMonitoringShouldBeEnabled() {
      final InstanceManager.InstanceManagerConfig config =
          InstanceManager.InstanceManagerConfig.builder().build();
      assertTrue(
          config.isHealthMonitoringEnabled(), "Health monitoring should be enabled by default");
    }

    @Test
    @DisplayName("default health check interval should be 1 minute")
    void defaultHealthCheckIntervalShouldBe1Minute() {
      final InstanceManager.InstanceManagerConfig config =
          InstanceManager.InstanceManagerConfig.builder().build();
      assertEquals(
          Duration.ofMinutes(1),
          config.getHealthCheckInterval(),
          "Default health check interval should be 1 minute");
    }

    @Test
    @DisplayName("default migration should be enabled")
    void defaultMigrationShouldBeEnabled() {
      final InstanceManager.InstanceManagerConfig config =
          InstanceManager.InstanceManagerConfig.builder().build();
      assertTrue(config.isMigrationEnabled(), "Migration should be enabled by default");
    }

    @Test
    @DisplayName("default checkpointing should be disabled")
    void defaultCheckpointingShouldBeDisabled() {
      final InstanceManager.InstanceManagerConfig config =
          InstanceManager.InstanceManagerConfig.builder().build();
      assertFalse(config.isCheckpointingEnabled(), "Checkpointing should be disabled by default");
    }

    @Test
    @DisplayName("default instance timeout should be 5 minutes")
    void defaultInstanceTimeoutShouldBe5Minutes() {
      final InstanceManager.InstanceManagerConfig config =
          InstanceManager.InstanceManagerConfig.builder().build();
      assertEquals(
          Duration.ofMinutes(5),
          config.getInstanceTimeout(),
          "Default instance timeout should be 5 minutes");
    }

    @Test
    @DisplayName("defaultConfig should produce same values as builder defaults")
    void defaultConfigShouldMatchBuilderDefaults() {
      final InstanceManager.InstanceManagerConfig defaultConfig =
          InstanceManager.InstanceManagerConfig.defaultConfig();
      final InstanceManager.InstanceManagerConfig builderConfig =
          InstanceManager.InstanceManagerConfig.builder().build();

      assertNotNull(defaultConfig, "defaultConfig() should not return null");
      assertEquals(
          builderConfig.getDefaultPoolSize(),
          defaultConfig.getDefaultPoolSize(),
          "Default pool sizes should match");
      assertEquals(
          builderConfig.getMaxPoolSize(),
          defaultConfig.getMaxPoolSize(),
          "Max pool sizes should match");
      assertEquals(
          builderConfig.isAutoScalingEnabled(),
          defaultConfig.isAutoScalingEnabled(),
          "Auto scaling should match");
      assertEquals(
          builderConfig.getScalingThreshold(),
          defaultConfig.getScalingThreshold(),
          0.001,
          "Scaling thresholds should match");
      assertEquals(
          builderConfig.isHealthMonitoringEnabled(),
          defaultConfig.isHealthMonitoringEnabled(),
          "Health monitoring should match");
      assertEquals(
          builderConfig.getHealthCheckInterval(),
          defaultConfig.getHealthCheckInterval(),
          "Health check intervals should match");
      assertEquals(
          builderConfig.isMigrationEnabled(),
          defaultConfig.isMigrationEnabled(),
          "Migration should match");
      assertEquals(
          builderConfig.isCheckpointingEnabled(),
          defaultConfig.isCheckpointingEnabled(),
          "Checkpointing should match");
      assertEquals(
          builderConfig.getInstanceTimeout(),
          defaultConfig.getInstanceTimeout(),
          "Instance timeouts should match");
    }
  }
}
