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
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentSwapConfig} class.
 *
 * <p>ComponentSwapConfig configures hot-swapping behavior including strategy, max swap time, state
 * preservation, rollback, graceful shutdown, compatibility checking, and interface migration.
 */
@DisplayName("ComponentSwapConfig Tests")
class ComponentSwapConfigTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(ComponentSwapConfig.class.getModifiers()),
          "ComponentSwapConfig should be public");
      assertTrue(
          Modifier.isFinal(ComponentSwapConfig.class.getModifiers()),
          "ComponentSwapConfig should be final");
    }
  }

  @Nested
  @DisplayName("Default Builder Values Tests")
  class DefaultBuilderValuesTests {

    @Test
    @DisplayName("should have default builder values")
    void shouldHaveDefaultValues() {
      final ComponentSwapConfig config = ComponentSwapConfig.builder().build();

      assertNotNull(config, "Config should not be null");
      assertEquals(
          ComponentSwapConfig.SwapStrategy.ROLLING_UPDATE, config.getStrategy(),
          "Default strategy should be ROLLING_UPDATE");
      assertEquals(
          Duration.ofMinutes(1), config.getMaxSwapTime(),
          "Default maxSwapTime should be 1 minute");
      assertTrue(config.isPreserveState(), "Default preserveState should be true");
      assertEquals(
          ComponentSwapConfig.RollbackStrategy.AUTOMATIC,
          config.getRollbackStrategy(),
          "Default rollbackStrategy should be AUTOMATIC");
      assertNotNull(config.getStateScopes(), "stateScopes should not be null");
      assertTrue(config.getSwapParameters().isEmpty(), "Default swapParameters should be empty");
      assertTrue(
          config.isGracefulShutdownEnabled(),
          "Default graceful shutdown should be enabled");
      assertEquals(
          Duration.ofSeconds(30), config.getGracefulShutdownTimeout(),
          "Default graceful shutdown timeout should be 30 seconds");
      assertEquals(
          ComponentSwapConfig.CompatibilityCheckLevel.MODERATE,
          config.getCompatibilityCheck(),
          "Default compatibility check should be MODERATE");
      assertFalse(
          config.isInterfaceMigrationAllowed(),
          "Default interface migration should be false");
    }
  }

  @Nested
  @DisplayName("Safe Production Configuration Tests")
  class SafeProductionConfigTests {

    @Test
    @DisplayName("safeProductionConfig should use safe defaults")
    void safeProductionConfigShouldUseSafeDefaults() {
      final ComponentSwapConfig config = ComponentSwapConfig.safeProductionConfig();

      assertEquals(
          ComponentSwapConfig.SwapStrategy.BLUE_GREEN, config.getStrategy(),
          "Production strategy should be BLUE_GREEN");
      assertEquals(
          Duration.ofSeconds(30), config.getMaxSwapTime(),
          "Production maxSwapTime should be 30 seconds");
      assertTrue(config.isPreserveState(), "Production should preserve state");
      assertEquals(
          ComponentSwapConfig.RollbackStrategy.AUTOMATIC,
          config.getRollbackStrategy(),
          "Production rollback should be AUTOMATIC");
      assertTrue(
          config.isGracefulShutdownEnabled(),
          "Production should enable graceful shutdown");
      assertEquals(
          ComponentSwapConfig.CompatibilityCheckLevel.STRICT,
          config.getCompatibilityCheck(),
          "Production compatibility should be STRICT");
    }
  }

  @Nested
  @DisplayName("Fast Development Configuration Tests")
  class FastDevelopmentConfigTests {

    @Test
    @DisplayName("fastDevelopmentConfig should use fast development defaults")
    void fastDevConfigShouldUseFastDefaults() {
      final ComponentSwapConfig config = ComponentSwapConfig.fastDevelopmentConfig();

      assertEquals(
          ComponentSwapConfig.SwapStrategy.DIRECT_REPLACEMENT, config.getStrategy(),
          "Development strategy should be DIRECT_REPLACEMENT");
      assertEquals(
          Duration.ofSeconds(5), config.getMaxSwapTime(),
          "Development maxSwapTime should be 5 seconds");
      assertFalse(config.isPreserveState(), "Development should not preserve state");
      assertEquals(
          ComponentSwapConfig.RollbackStrategy.MANUAL,
          config.getRollbackStrategy(),
          "Development rollback should be MANUAL");
      assertEquals(
          ComponentSwapConfig.CompatibilityCheckLevel.BASIC,
          config.getCompatibilityCheck(),
          "Development compatibility should be BASIC");
      assertTrue(
          config.isInterfaceMigrationAllowed(),
          "Development should allow interface migration");
    }
  }

  @Nested
  @DisplayName("Builder Fluent API Tests")
  class BuilderFluentApiTests {

    @Test
    @DisplayName("should set strategy")
    void shouldSetStrategy() {
      final ComponentSwapConfig config = ComponentSwapConfig.builder()
          .strategy(ComponentSwapConfig.SwapStrategy.CANARY)
          .build();

      assertEquals(
          ComponentSwapConfig.SwapStrategy.CANARY, config.getStrategy(),
          "Strategy should be CANARY");
    }

    @Test
    @DisplayName("should set maxSwapTime")
    void shouldSetMaxSwapTime() {
      final ComponentSwapConfig config = ComponentSwapConfig.builder()
          .maxSwapTime(Duration.ofSeconds(10))
          .build();

      assertEquals(
          Duration.ofSeconds(10), config.getMaxSwapTime(),
          "maxSwapTime should be 10 seconds");
    }

    @Test
    @DisplayName("should set preserveState")
    void shouldSetPreserveState() {
      final ComponentSwapConfig config = ComponentSwapConfig.builder()
          .preserveState(false)
          .build();

      assertFalse(config.isPreserveState(), "preserveState should be false");
    }

    @Test
    @DisplayName("should set rollbackStrategy")
    void shouldSetRollbackStrategy() {
      final ComponentSwapConfig config = ComponentSwapConfig.builder()
          .rollbackStrategy(ComponentSwapConfig.RollbackStrategy.AUTOMATIC_WITH_HEALTH_CHECKS)
          .build();

      assertEquals(
          ComponentSwapConfig.RollbackStrategy.AUTOMATIC_WITH_HEALTH_CHECKS,
          config.getRollbackStrategy(),
          "Rollback strategy should be AUTOMATIC_WITH_HEALTH_CHECKS");
    }

    @Test
    @DisplayName("should set stateScopes")
    void shouldSetStateScopes() {
      final Set<ComponentSwapConfig.StatePreservationScope> scopes = Set.of(
          ComponentSwapConfig.StatePreservationScope.CACHE,
          ComponentSwapConfig.StatePreservationScope.CONNECTIONS);
      final ComponentSwapConfig config = ComponentSwapConfig.builder()
          .stateScopes(scopes)
          .build();

      assertEquals(2, config.getStateScopes().size(), "Should have 2 state scopes");
      assertTrue(
          config.getStateScopes().contains(ComponentSwapConfig.StatePreservationScope.CACHE),
          "Should contain CACHE scope");
    }

    @Test
    @DisplayName("should set swapParameters")
    void shouldSetSwapParameters() {
      final ComponentSwapConfig config = ComponentSwapConfig.builder()
          .swapParameters(Map.of("param1", "val1"))
          .build();

      assertEquals(
          1, config.getSwapParameters().size(),
          "Should have 1 swap parameter");
    }

    @Test
    @DisplayName("should set graceful shutdown settings")
    void shouldSetGracefulShutdownSettings() {
      final ComponentSwapConfig config = ComponentSwapConfig.builder()
          .enableGracefulShutdown(false)
          .gracefulShutdownTimeout(Duration.ofSeconds(60))
          .build();

      assertFalse(
          config.isGracefulShutdownEnabled(),
          "Graceful shutdown should be disabled");
      assertEquals(
          Duration.ofSeconds(60), config.getGracefulShutdownTimeout(),
          "Graceful shutdown timeout should be 60 seconds");
    }
  }

  @Nested
  @DisplayName("Enum Tests")
  class EnumTests {

    @Test
    @DisplayName("SwapStrategy should have all expected values")
    void swapStrategyShouldHaveAllValues() {
      final ComponentSwapConfig.SwapStrategy[] values =
          ComponentSwapConfig.SwapStrategy.values();
      assertEquals(5, values.length, "SwapStrategy should have 5 values");
      assertNotNull(
          ComponentSwapConfig.SwapStrategy.valueOf("AB_TESTING"),
          "AB_TESTING should exist");
    }

    @Test
    @DisplayName("RollbackStrategy should have all expected values")
    void rollbackStrategyShouldHaveAllValues() {
      final ComponentSwapConfig.RollbackStrategy[] values =
          ComponentSwapConfig.RollbackStrategy.values();
      assertEquals(4, values.length, "RollbackStrategy should have 4 values");
    }

    @Test
    @DisplayName("StatePreservationScope should have all expected values")
    void statePreservationScopeShouldHaveAllValues() {
      final ComponentSwapConfig.StatePreservationScope[] values =
          ComponentSwapConfig.StatePreservationScope.values();
      assertEquals(6, values.length, "StatePreservationScope should have 6 values");
      assertNotNull(
          ComponentSwapConfig.StatePreservationScope.valueOf("ALL"),
          "ALL scope should exist");
    }

    @Test
    @DisplayName("CompatibilityCheckLevel should have all expected values")
    void compatibilityCheckLevelShouldHaveAllValues() {
      final ComponentSwapConfig.CompatibilityCheckLevel[] values =
          ComponentSwapConfig.CompatibilityCheckLevel.values();
      assertEquals(5, values.length, "CompatibilityCheckLevel should have 5 values");
    }
  }
}
