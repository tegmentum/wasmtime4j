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

import ai.tegmentum.wasmtime4j.ComponentSwapConfig.CompatibilityCheckLevel;
import ai.tegmentum.wasmtime4j.ComponentSwapConfig.RollbackStrategy;
import ai.tegmentum.wasmtime4j.ComponentSwapConfig.StatePreservationScope;
import ai.tegmentum.wasmtime4j.ComponentSwapConfig.SwapStrategy;
import java.lang.reflect.Method;
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
 * <p>ComponentSwapConfig provides configuration for component hot-swapping operations.
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

    @Test
    @DisplayName("should have builder method")
    void shouldHaveBuilderMethod() throws NoSuchMethodException {
      final Method method = ComponentSwapConfig.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
    }

    @Test
    @DisplayName("should have safeProductionConfig factory method")
    void shouldHaveSafeProductionConfigFactoryMethod() throws NoSuchMethodException {
      final Method method = ComponentSwapConfig.class.getMethod("safeProductionConfig");
      assertNotNull(method, "safeProductionConfig method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "safeProductionConfig should be static");
    }

    @Test
    @DisplayName("should have fastDevelopmentConfig factory method")
    void shouldHaveFastDevelopmentConfigFactoryMethod() throws NoSuchMethodException {
      final Method method = ComponentSwapConfig.class.getMethod("fastDevelopmentConfig");
      assertNotNull(method, "fastDevelopmentConfig method should exist");
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "fastDevelopmentConfig should be static");
    }
  }

  @Nested
  @DisplayName("Default Builder Configuration Tests")
  class DefaultBuilderConfigurationTests {

    @Test
    @DisplayName("default builder should set rolling update strategy")
    void defaultBuilderShouldSetRollingUpdateStrategy() {
      final var config = ComponentSwapConfig.builder().build();

      assertEquals(
          SwapStrategy.ROLLING_UPDATE,
          config.getStrategy(),
          "Default strategy should be ROLLING_UPDATE");
    }

    @Test
    @DisplayName("default builder should set 1 minute max swap time")
    void defaultBuilderShouldSet1MinuteMaxSwapTime() {
      final var config = ComponentSwapConfig.builder().build();

      assertEquals(
          Duration.ofMinutes(1), config.getMaxSwapTime(), "Default max swap time should be 1 min");
    }

    @Test
    @DisplayName("default builder should preserve state")
    void defaultBuilderShouldPreserveState() {
      final var config = ComponentSwapConfig.builder().build();

      assertTrue(config.isPreserveState(), "Default should preserve state");
    }

    @Test
    @DisplayName("default builder should set automatic rollback")
    void defaultBuilderShouldSetAutomaticRollback() {
      final var config = ComponentSwapConfig.builder().build();

      assertEquals(
          RollbackStrategy.AUTOMATIC,
          config.getRollbackStrategy(),
          "Default rollback should be AUTOMATIC");
    }

    @Test
    @DisplayName("default builder should enable graceful shutdown")
    void defaultBuilderShouldEnableGracefulShutdown() {
      final var config = ComponentSwapConfig.builder().build();

      assertTrue(config.isGracefulShutdownEnabled(), "Default should enable graceful shutdown");
    }

    @Test
    @DisplayName("default builder should set 30 second graceful shutdown timeout")
    void defaultBuilderShouldSet30SecondGracefulShutdownTimeout() {
      final var config = ComponentSwapConfig.builder().build();

      assertEquals(
          Duration.ofSeconds(30),
          config.getGracefulShutdownTimeout(),
          "Default graceful shutdown timeout should be 30s");
    }

    @Test
    @DisplayName("default builder should set moderate compatibility check")
    void defaultBuilderShouldSetModerateCompatibilityCheck() {
      final var config = ComponentSwapConfig.builder().build();

      assertEquals(
          CompatibilityCheckLevel.MODERATE,
          config.getCompatibilityCheck(),
          "Default compatibility check should be MODERATE");
    }

    @Test
    @DisplayName("default builder should disable interface migration")
    void defaultBuilderShouldDisableInterfaceMigration() {
      final var config = ComponentSwapConfig.builder().build();

      assertFalse(
          config.isInterfaceMigrationAllowed(), "Default should disable interface migration");
    }
  }

  @Nested
  @DisplayName("Safe Production Configuration Tests")
  class SafeProductionConfigurationTests {

    @Test
    @DisplayName("safe production config should use blue green strategy")
    void safeProductionConfigShouldUseBlueGreenStrategy() {
      final var config = ComponentSwapConfig.safeProductionConfig();

      assertEquals(
          SwapStrategy.BLUE_GREEN, config.getStrategy(), "Production should use BLUE_GREEN");
    }

    @Test
    @DisplayName("safe production config should set 30 second max swap time")
    void safeProductionConfigShouldSet30SecondMaxSwapTime() {
      final var config = ComponentSwapConfig.safeProductionConfig();

      assertEquals(
          Duration.ofSeconds(30),
          config.getMaxSwapTime(),
          "Production max swap time should be 30s");
    }

    @Test
    @DisplayName("safe production config should preserve state")
    void safeProductionConfigShouldPreserveState() {
      final var config = ComponentSwapConfig.safeProductionConfig();

      assertTrue(config.isPreserveState(), "Production should preserve state");
    }

    @Test
    @DisplayName("safe production config should use automatic rollback")
    void safeProductionConfigShouldUseAutomaticRollback() {
      final var config = ComponentSwapConfig.safeProductionConfig();

      assertEquals(
          RollbackStrategy.AUTOMATIC,
          config.getRollbackStrategy(),
          "Production should use AUTOMATIC rollback");
    }

    @Test
    @DisplayName("safe production config should enable graceful shutdown")
    void safeProductionConfigShouldEnableGracefulShutdown() {
      final var config = ComponentSwapConfig.safeProductionConfig();

      assertTrue(config.isGracefulShutdownEnabled(), "Production should enable graceful shutdown");
    }

    @Test
    @DisplayName("safe production config should set strict compatibility")
    void safeProductionConfigShouldSetStrictCompatibility() {
      final var config = ComponentSwapConfig.safeProductionConfig();

      assertEquals(
          CompatibilityCheckLevel.STRICT,
          config.getCompatibilityCheck(),
          "Production should use STRICT compatibility");
    }
  }

  @Nested
  @DisplayName("Fast Development Configuration Tests")
  class FastDevelopmentConfigurationTests {

    @Test
    @DisplayName("fast development config should use direct replacement strategy")
    void fastDevelopmentConfigShouldUseDirectReplacementStrategy() {
      final var config = ComponentSwapConfig.fastDevelopmentConfig();

      assertEquals(
          SwapStrategy.DIRECT_REPLACEMENT,
          config.getStrategy(),
          "Development should use DIRECT_REPLACEMENT");
    }

    @Test
    @DisplayName("fast development config should set 5 second max swap time")
    void fastDevelopmentConfigShouldSet5SecondMaxSwapTime() {
      final var config = ComponentSwapConfig.fastDevelopmentConfig();

      assertEquals(
          Duration.ofSeconds(5), config.getMaxSwapTime(), "Development max swap time should be 5s");
    }

    @Test
    @DisplayName("fast development config should not preserve state")
    void fastDevelopmentConfigShouldNotPreserveState() {
      final var config = ComponentSwapConfig.fastDevelopmentConfig();

      assertFalse(config.isPreserveState(), "Development should not preserve state");
    }

    @Test
    @DisplayName("fast development config should use manual rollback")
    void fastDevelopmentConfigShouldUseManualRollback() {
      final var config = ComponentSwapConfig.fastDevelopmentConfig();

      assertEquals(
          RollbackStrategy.MANUAL,
          config.getRollbackStrategy(),
          "Development should use MANUAL rollback");
    }

    @Test
    @DisplayName("fast development config should set basic compatibility")
    void fastDevelopmentConfigShouldSetBasicCompatibility() {
      final var config = ComponentSwapConfig.fastDevelopmentConfig();

      assertEquals(
          CompatibilityCheckLevel.BASIC,
          config.getCompatibilityCheck(),
          "Development should use BASIC compatibility");
    }

    @Test
    @DisplayName("fast development config should allow interface migration")
    void fastDevelopmentConfigShouldAllowInterfaceMigration() {
      final var config = ComponentSwapConfig.fastDevelopmentConfig();

      assertTrue(
          config.isInterfaceMigrationAllowed(), "Development should allow interface migration");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("builder should set strategy")
    void builderShouldSetStrategy() {
      final var config = ComponentSwapConfig.builder().strategy(SwapStrategy.CANARY).build();

      assertEquals(SwapStrategy.CANARY, config.getStrategy(), "Strategy should be CANARY");
    }

    @Test
    @DisplayName("builder should set max swap time")
    void builderShouldSetMaxSwapTime() {
      final var config = ComponentSwapConfig.builder().maxSwapTime(Duration.ofMinutes(5)).build();

      assertEquals(Duration.ofMinutes(5), config.getMaxSwapTime(), "Max swap time should be 5 min");
    }

    @Test
    @DisplayName("builder should set preserve state")
    void builderShouldSetPreserveState() {
      final var config = ComponentSwapConfig.builder().preserveState(false).build();

      assertFalse(config.isPreserveState(), "Preserve state should be false");
    }

    @Test
    @DisplayName("builder should set rollback strategy")
    void builderShouldSetRollbackStrategy() {
      final var config =
          ComponentSwapConfig.builder()
              .rollbackStrategy(RollbackStrategy.AUTOMATIC_WITH_HEALTH_CHECKS)
              .build();

      assertEquals(
          RollbackStrategy.AUTOMATIC_WITH_HEALTH_CHECKS,
          config.getRollbackStrategy(),
          "Rollback should be AUTOMATIC_WITH_HEALTH_CHECKS");
    }

    @Test
    @DisplayName("builder should set state scopes")
    void builderShouldSetStateScopes() {
      final var scopes =
          Set.of(StatePreservationScope.COMPONENT_STATE, StatePreservationScope.CACHE);
      final var config = ComponentSwapConfig.builder().stateScopes(scopes).build();

      assertEquals(scopes, config.getStateScopes(), "State scopes should match");
    }

    @Test
    @DisplayName("builder should set swap parameters")
    void builderShouldSetSwapParameters() {
      final var params = Map.of("key1", (Object) "value1", "key2", 42);
      final var config = ComponentSwapConfig.builder().swapParameters(params).build();

      assertEquals(params, config.getSwapParameters(), "Swap parameters should match");
    }

    @Test
    @DisplayName("builder should set enable graceful shutdown")
    void builderShouldSetEnableGracefulShutdown() {
      final var config = ComponentSwapConfig.builder().enableGracefulShutdown(false).build();

      assertFalse(config.isGracefulShutdownEnabled(), "Graceful shutdown should be disabled");
    }

    @Test
    @DisplayName("builder should set graceful shutdown timeout")
    void builderShouldSetGracefulShutdownTimeout() {
      final var config =
          ComponentSwapConfig.builder().gracefulShutdownTimeout(Duration.ofMinutes(2)).build();

      assertEquals(
          Duration.ofMinutes(2),
          config.getGracefulShutdownTimeout(),
          "Graceful shutdown timeout should be 2 min");
    }

    @Test
    @DisplayName("builder should set compatibility check level")
    void builderShouldSetCompatibilityCheckLevel() {
      final var config =
          ComponentSwapConfig.builder()
              .compatibilityCheck(CompatibilityCheckLevel.COMPLETE)
              .build();

      assertEquals(
          CompatibilityCheckLevel.COMPLETE,
          config.getCompatibilityCheck(),
          "Compatibility check should be COMPLETE");
    }

    @Test
    @DisplayName("builder should set allow interface migration")
    void builderShouldSetAllowInterfaceMigration() {
      final var config = ComponentSwapConfig.builder().allowInterfaceMigration(true).build();

      assertTrue(config.isInterfaceMigrationAllowed(), "Interface migration should be allowed");
    }

    @Test
    @DisplayName("builder should allow chaining")
    void builderShouldAllowChaining() {
      final var config =
          ComponentSwapConfig.builder()
              .strategy(SwapStrategy.AB_TESTING)
              .maxSwapTime(Duration.ofSeconds(45))
              .preserveState(true)
              .rollbackStrategy(RollbackStrategy.NONE)
              .enableGracefulShutdown(true)
              .gracefulShutdownTimeout(Duration.ofSeconds(20))
              .compatibilityCheck(CompatibilityCheckLevel.NONE)
              .allowInterfaceMigration(true)
              .build();

      assertEquals(SwapStrategy.AB_TESTING, config.getStrategy());
      assertEquals(Duration.ofSeconds(45), config.getMaxSwapTime());
      assertTrue(config.isPreserveState());
      assertEquals(RollbackStrategy.NONE, config.getRollbackStrategy());
      assertTrue(config.isGracefulShutdownEnabled());
      assertEquals(Duration.ofSeconds(20), config.getGracefulShutdownTimeout());
      assertEquals(CompatibilityCheckLevel.NONE, config.getCompatibilityCheck());
      assertTrue(config.isInterfaceMigrationAllowed());
    }
  }

  @Nested
  @DisplayName("SwapStrategy Enum Tests")
  class SwapStrategyEnumTests {

    @Test
    @DisplayName("should have all swap strategies")
    void shouldHaveAllSwapStrategies() {
      final var strategies = SwapStrategy.values();
      assertEquals(5, strategies.length, "Should have 5 swap strategies");
    }

    @Test
    @DisplayName("should have DIRECT_REPLACEMENT strategy")
    void shouldHaveDirectReplacementStrategy() {
      assertEquals(SwapStrategy.DIRECT_REPLACEMENT, SwapStrategy.valueOf("DIRECT_REPLACEMENT"));
    }

    @Test
    @DisplayName("should have BLUE_GREEN strategy")
    void shouldHaveBlueGreenStrategy() {
      assertEquals(SwapStrategy.BLUE_GREEN, SwapStrategy.valueOf("BLUE_GREEN"));
    }

    @Test
    @DisplayName("should have ROLLING_UPDATE strategy")
    void shouldHaveRollingUpdateStrategy() {
      assertEquals(SwapStrategy.ROLLING_UPDATE, SwapStrategy.valueOf("ROLLING_UPDATE"));
    }

    @Test
    @DisplayName("should have CANARY strategy")
    void shouldHaveCanaryStrategy() {
      assertEquals(SwapStrategy.CANARY, SwapStrategy.valueOf("CANARY"));
    }

    @Test
    @DisplayName("should have AB_TESTING strategy")
    void shouldHaveAbTestingStrategy() {
      assertEquals(SwapStrategy.AB_TESTING, SwapStrategy.valueOf("AB_TESTING"));
    }
  }

  @Nested
  @DisplayName("RollbackStrategy Enum Tests")
  class RollbackStrategyEnumTests {

    @Test
    @DisplayName("should have all rollback strategies")
    void shouldHaveAllRollbackStrategies() {
      final var strategies = RollbackStrategy.values();
      assertEquals(4, strategies.length, "Should have 4 rollback strategies");
    }

    @Test
    @DisplayName("should have NONE strategy")
    void shouldHaveNoneStrategy() {
      assertEquals(RollbackStrategy.NONE, RollbackStrategy.valueOf("NONE"));
    }

    @Test
    @DisplayName("should have MANUAL strategy")
    void shouldHaveManualStrategy() {
      assertEquals(RollbackStrategy.MANUAL, RollbackStrategy.valueOf("MANUAL"));
    }

    @Test
    @DisplayName("should have AUTOMATIC strategy")
    void shouldHaveAutomaticStrategy() {
      assertEquals(RollbackStrategy.AUTOMATIC, RollbackStrategy.valueOf("AUTOMATIC"));
    }

    @Test
    @DisplayName("should have AUTOMATIC_WITH_HEALTH_CHECKS strategy")
    void shouldHaveAutomaticWithHealthChecksStrategy() {
      assertEquals(
          RollbackStrategy.AUTOMATIC_WITH_HEALTH_CHECKS,
          RollbackStrategy.valueOf("AUTOMATIC_WITH_HEALTH_CHECKS"));
    }
  }

  @Nested
  @DisplayName("StatePreservationScope Enum Tests")
  class StatePreservationScopeEnumTests {

    @Test
    @DisplayName("should have all state preservation scopes")
    void shouldHaveAllStatePreservationScopes() {
      final var scopes = StatePreservationScope.values();
      assertEquals(6, scopes.length, "Should have 6 state preservation scopes");
    }

    @Test
    @DisplayName("should have COMPONENT_STATE scope")
    void shouldHaveComponentStateScope() {
      assertEquals(
          StatePreservationScope.COMPONENT_STATE,
          StatePreservationScope.valueOf("COMPONENT_STATE"));
    }

    @Test
    @DisplayName("should have SHARED_RESOURCES scope")
    void shouldHaveSharedResourcesScope() {
      assertEquals(
          StatePreservationScope.SHARED_RESOURCES,
          StatePreservationScope.valueOf("SHARED_RESOURCES"));
    }

    @Test
    @DisplayName("should have CONNECTIONS scope")
    void shouldHaveConnectionsScope() {
      assertEquals(
          StatePreservationScope.CONNECTIONS, StatePreservationScope.valueOf("CONNECTIONS"));
    }

    @Test
    @DisplayName("should have CACHE scope")
    void shouldHaveCacheScope() {
      assertEquals(StatePreservationScope.CACHE, StatePreservationScope.valueOf("CACHE"));
    }

    @Test
    @DisplayName("should have CONFIGURATION scope")
    void shouldHaveConfigurationScope() {
      assertEquals(
          StatePreservationScope.CONFIGURATION, StatePreservationScope.valueOf("CONFIGURATION"));
    }

    @Test
    @DisplayName("should have ALL scope")
    void shouldHaveAllScope() {
      assertEquals(StatePreservationScope.ALL, StatePreservationScope.valueOf("ALL"));
    }
  }

  @Nested
  @DisplayName("CompatibilityCheckLevel Enum Tests")
  class CompatibilityCheckLevelEnumTests {

    @Test
    @DisplayName("should have all compatibility check levels")
    void shouldHaveAllCompatibilityCheckLevels() {
      final var levels = CompatibilityCheckLevel.values();
      assertEquals(5, levels.length, "Should have 5 compatibility check levels");
    }

    @Test
    @DisplayName("should have NONE level")
    void shouldHaveNoneLevel() {
      assertEquals(CompatibilityCheckLevel.NONE, CompatibilityCheckLevel.valueOf("NONE"));
    }

    @Test
    @DisplayName("should have BASIC level")
    void shouldHaveBasicLevel() {
      assertEquals(CompatibilityCheckLevel.BASIC, CompatibilityCheckLevel.valueOf("BASIC"));
    }

    @Test
    @DisplayName("should have MODERATE level")
    void shouldHaveModerateLevel() {
      assertEquals(CompatibilityCheckLevel.MODERATE, CompatibilityCheckLevel.valueOf("MODERATE"));
    }

    @Test
    @DisplayName("should have STRICT level")
    void shouldHaveStrictLevel() {
      assertEquals(CompatibilityCheckLevel.STRICT, CompatibilityCheckLevel.valueOf("STRICT"));
    }

    @Test
    @DisplayName("should have COMPLETE level")
    void shouldHaveCompleteLevel() {
      assertEquals(CompatibilityCheckLevel.COMPLETE, CompatibilityCheckLevel.valueOf("COMPLETE"));
    }
  }

  @Nested
  @DisplayName("Immutability Tests")
  class ImmutabilityTests {

    @Test
    @DisplayName("state scopes should be immutable")
    void stateScopesShouldBeImmutable() {
      final var config = ComponentSwapConfig.builder().build();
      final var scopes = config.getStateScopes();

      org.junit.jupiter.api.Assertions.assertThrows(
          UnsupportedOperationException.class,
          () -> scopes.add(StatePreservationScope.ALL),
          "State scopes should be immutable");
    }

    @Test
    @DisplayName("swap parameters should be immutable")
    void swapParametersShouldBeImmutable() {
      final var config =
          ComponentSwapConfig.builder().swapParameters(Map.of("key", "value")).build();
      final var params = config.getSwapParameters();

      org.junit.jupiter.api.Assertions.assertThrows(
          UnsupportedOperationException.class,
          () -> params.put("newKey", "newValue"),
          "Swap parameters should be immutable");
    }
  }
}
