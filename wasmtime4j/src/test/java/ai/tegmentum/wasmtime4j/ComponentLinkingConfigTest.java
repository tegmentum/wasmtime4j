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

import ai.tegmentum.wasmtime4j.ComponentLinkingConfig.CompatibilityLevel;
import ai.tegmentum.wasmtime4j.ComponentLinkingConfig.LinkingStrategy;
import ai.tegmentum.wasmtime4j.ComponentLinkingConfig.ResourceIsolationLevel;
import ai.tegmentum.wasmtime4j.ComponentLinkingConfig.SharedResourcePolicy;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentLinkingConfig} class.
 *
 * <p>ComponentLinkingConfig provides configuration for advanced component linking operations.
 */
@DisplayName("ComponentLinkingConfig Tests")
class ComponentLinkingConfigTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(ComponentLinkingConfig.class.getModifiers()),
          "ComponentLinkingConfig should be public");
      assertTrue(
          Modifier.isFinal(ComponentLinkingConfig.class.getModifiers()),
          "ComponentLinkingConfig should be final");
    }

    @Test
    @DisplayName("should have builder method")
    void shouldHaveBuilderMethod() throws NoSuchMethodException {
      final Method method = ComponentLinkingConfig.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
    }

    @Test
    @DisplayName("should have defaultConfig method")
    void shouldHaveDefaultConfigMethod() throws NoSuchMethodException {
      final Method method = ComponentLinkingConfig.class.getMethod("defaultConfig");
      assertNotNull(method, "defaultConfig method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "defaultConfig should be static");
    }

    @Test
    @DisplayName("should have productionConfig method")
    void shouldHaveProductionConfigMethod() throws NoSuchMethodException {
      final Method method = ComponentLinkingConfig.class.getMethod("productionConfig");
      assertNotNull(method, "productionConfig method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "productionConfig should be static");
    }

    @Test
    @DisplayName("should have developmentConfig method")
    void shouldHaveDevelopmentConfigMethod() throws NoSuchMethodException {
      final Method method = ComponentLinkingConfig.class.getMethod("developmentConfig");
      assertNotNull(method, "developmentConfig method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "developmentConfig should be static");
    }
  }

  @Nested
  @DisplayName("Default Configuration Tests")
  class DefaultConfigurationTests {

    @Test
    @DisplayName("default config should have adaptive strategy")
    void defaultConfigShouldHaveAdaptiveStrategy() {
      final var config = ComponentLinkingConfig.defaultConfig();

      assertEquals(
          LinkingStrategy.ADAPTIVE, config.getStrategy(), "Default strategy should be ADAPTIVE");
    }

    @Test
    @DisplayName("default config should have conservative resource policy")
    void defaultConfigShouldHaveConservativeResourcePolicy() {
      final var config = ComponentLinkingConfig.defaultConfig();

      assertEquals(
          SharedResourcePolicy.CONSERVATIVE,
          config.getSharedResourcePolicy(),
          "Default resource policy should be CONSERVATIVE");
    }

    @Test
    @DisplayName("default config should have moderate compatibility level")
    void defaultConfigShouldHaveModerateCompatibilityLevel() {
      final var config = ComponentLinkingConfig.defaultConfig();

      assertEquals(
          CompatibilityLevel.MODERATE,
          config.getCompatibilityLevel(),
          "Default compatibility should be MODERATE");
    }

    @Test
    @DisplayName("default config should have version mismatch disabled")
    void defaultConfigShouldHaveVersionMismatchDisabled() {
      final var config = ComponentLinkingConfig.defaultConfig();

      assertFalse(config.isAllowVersionMismatch(), "Version mismatch should be disabled");
    }

    @Test
    @DisplayName("default config should have interface adaptation disabled")
    void defaultConfigShouldHaveInterfaceAdaptationDisabled() {
      final var config = ComponentLinkingConfig.defaultConfig();

      assertFalse(config.isInterfaceAdaptationEnabled(), "Interface adaptation should be disabled");
    }

    @Test
    @DisplayName("default config should have hot swapping disabled")
    void defaultConfigShouldHaveHotSwappingDisabled() {
      final var config = ComponentLinkingConfig.defaultConfig();

      assertFalse(config.isHotSwappingEnabled(), "Hot swapping should be disabled");
    }
  }

  @Nested
  @DisplayName("Production Configuration Tests")
  class ProductionConfigurationTests {

    @Test
    @DisplayName("production config should have shared everything strategy")
    void productionConfigShouldHaveSharedEverythingStrategy() {
      final var config = ComponentLinkingConfig.productionConfig();

      assertEquals(
          LinkingStrategy.SHARED_EVERYTHING,
          config.getStrategy(),
          "Production should use SHARED_EVERYTHING");
    }

    @Test
    @DisplayName("production config should have strict compatibility")
    void productionConfigShouldHaveStrictCompatibility() {
      final var config = ComponentLinkingConfig.productionConfig();

      assertEquals(
          CompatibilityLevel.STRICT,
          config.getCompatibilityLevel(),
          "Production should use STRICT compatibility");
    }

    @Test
    @DisplayName("production config should have 30 second timeout")
    void productionConfigShouldHave30SecondTimeout() {
      final var config = ComponentLinkingConfig.productionConfig();

      assertEquals(
          Duration.ofSeconds(30),
          config.getLinkingTimeout(),
          "Production timeout should be 30 seconds");
    }
  }

  @Nested
  @DisplayName("Development Configuration Tests")
  class DevelopmentConfigurationTests {

    @Test
    @DisplayName("development config should have static linking strategy")
    void developmentConfigShouldHaveStaticLinkingStrategy() {
      final var config = ComponentLinkingConfig.developmentConfig();

      assertEquals(
          LinkingStrategy.STATIC_LINKING,
          config.getStrategy(),
          "Development should use STATIC_LINKING");
    }

    @Test
    @DisplayName("development config should allow version mismatch")
    void developmentConfigShouldAllowVersionMismatch() {
      final var config = ComponentLinkingConfig.developmentConfig();

      assertTrue(config.isAllowVersionMismatch(), "Development should allow version mismatch");
    }

    @Test
    @DisplayName("development config should enable interface adaptation")
    void developmentConfigShouldEnableInterfaceAdaptation() {
      final var config = ComponentLinkingConfig.developmentConfig();

      assertTrue(
          config.isInterfaceAdaptationEnabled(), "Development should enable interface adaptation");
    }

    @Test
    @DisplayName("development config should enable hot swapping")
    void developmentConfigShouldEnableHotSwapping() {
      final var config = ComponentLinkingConfig.developmentConfig();

      assertTrue(config.isHotSwappingEnabled(), "Development should enable hot swapping");
    }

    @Test
    @DisplayName("development config should have lenient compatibility")
    void developmentConfigShouldHaveLenientCompatibility() {
      final var config = ComponentLinkingConfig.developmentConfig();

      assertEquals(
          CompatibilityLevel.LENIENT,
          config.getCompatibilityLevel(),
          "Development should use LENIENT compatibility");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("builder should create config")
    void builderShouldCreateConfig() {
      final var config = ComponentLinkingConfig.builder().build();

      assertNotNull(config, "Builder should create config");
    }

    @Test
    @DisplayName("builder should set strategy")
    void builderShouldSetStrategy() {
      final var config =
          ComponentLinkingConfig.builder().strategy(LinkingStrategy.DYNAMIC_LINKING).build();

      assertEquals(LinkingStrategy.DYNAMIC_LINKING, config.getStrategy(), "Strategy should match");
    }

    @Test
    @DisplayName("builder should set shared resource policy")
    void builderShouldSetSharedResourcePolicy() {
      final var config =
          ComponentLinkingConfig.builder()
              .sharedResourcePolicy(SharedResourcePolicy.AGGRESSIVE_SHARING)
              .build();

      assertEquals(
          SharedResourcePolicy.AGGRESSIVE_SHARING,
          config.getSharedResourcePolicy(),
          "Policy should match");
    }

    @Test
    @DisplayName("builder should set linking timeout")
    void builderShouldSetLinkingTimeout() {
      final var config =
          ComponentLinkingConfig.builder().linkingTimeout(Duration.ofMinutes(5)).build();

      assertEquals(Duration.ofMinutes(5), config.getLinkingTimeout(), "Timeout should be 5 min");
    }

    @Test
    @DisplayName("builder should set allow version mismatch")
    void builderShouldSetAllowVersionMismatch() {
      final var config = ComponentLinkingConfig.builder().allowVersionMismatch(true).build();

      assertTrue(config.isAllowVersionMismatch(), "Should allow version mismatch");
    }

    @Test
    @DisplayName("builder should set compatibility level")
    void builderShouldSetCompatibilityLevel() {
      final var config =
          ComponentLinkingConfig.builder().compatibilityLevel(CompatibilityLevel.STRICT).build();

      assertEquals(CompatibilityLevel.STRICT, config.getCompatibilityLevel(), "Should be STRICT");
    }

    @Test
    @DisplayName("builder should set resource isolation level")
    void builderShouldSetResourceIsolationLevel() {
      final var config =
          ComponentLinkingConfig.builder()
              .resourceIsolation(ResourceIsolationLevel.COMPLETE)
              .build();

      assertEquals(
          ResourceIsolationLevel.COMPLETE,
          config.getResourceIsolation(),
          "Should be COMPLETE isolation");
    }

    @Test
    @DisplayName("builder should allow chaining")
    void builderShouldAllowChaining() {
      final var config =
          ComponentLinkingConfig.builder()
              .strategy(LinkingStrategy.SHARED_EVERYTHING)
              .sharedResourcePolicy(SharedResourcePolicy.MODERATE)
              .linkingTimeout(Duration.ofSeconds(45))
              .allowVersionMismatch(true)
              .compatibilityLevel(CompatibilityLevel.LENIENT)
              .enableInterfaceAdaptation(true)
              .resourceIsolation(ResourceIsolationLevel.BASIC)
              .enableHotSwapping(true)
              .build();

      assertEquals(LinkingStrategy.SHARED_EVERYTHING, config.getStrategy());
      assertEquals(SharedResourcePolicy.MODERATE, config.getSharedResourcePolicy());
      assertEquals(Duration.ofSeconds(45), config.getLinkingTimeout());
      assertTrue(config.isAllowVersionMismatch());
      assertEquals(CompatibilityLevel.LENIENT, config.getCompatibilityLevel());
      assertTrue(config.isInterfaceAdaptationEnabled());
      assertEquals(ResourceIsolationLevel.BASIC, config.getResourceIsolation());
      assertTrue(config.isHotSwappingEnabled());
    }
  }

  @Nested
  @DisplayName("LinkingStrategy Enum Tests")
  class LinkingStrategyEnumTests {

    @Test
    @DisplayName("should have all linking strategies")
    void shouldHaveAllLinkingStrategies() {
      final var strategies = LinkingStrategy.values();
      assertEquals(4, strategies.length, "Should have 4 linking strategies");
    }

    @Test
    @DisplayName("should have STATIC_LINKING strategy")
    void shouldHaveStaticLinkingStrategy() {
      assertEquals(LinkingStrategy.STATIC_LINKING, LinkingStrategy.valueOf("STATIC_LINKING"));
    }

    @Test
    @DisplayName("should have DYNAMIC_LINKING strategy")
    void shouldHaveDynamicLinkingStrategy() {
      assertEquals(LinkingStrategy.DYNAMIC_LINKING, LinkingStrategy.valueOf("DYNAMIC_LINKING"));
    }

    @Test
    @DisplayName("should have SHARED_EVERYTHING strategy")
    void shouldHaveSharedEverythingStrategy() {
      assertEquals(LinkingStrategy.SHARED_EVERYTHING, LinkingStrategy.valueOf("SHARED_EVERYTHING"));
    }

    @Test
    @DisplayName("should have ADAPTIVE strategy")
    void shouldHaveAdaptiveStrategy() {
      assertEquals(LinkingStrategy.ADAPTIVE, LinkingStrategy.valueOf("ADAPTIVE"));
    }
  }

  @Nested
  @DisplayName("SharedResourcePolicy Enum Tests")
  class SharedResourcePolicyEnumTests {

    @Test
    @DisplayName("should have all shared resource policies")
    void shouldHaveAllSharedResourcePolicies() {
      final var policies = SharedResourcePolicy.values();
      assertEquals(4, policies.length, "Should have 4 shared resource policies");
    }

    @Test
    @DisplayName("should have NO_SHARING policy")
    void shouldHaveNoSharingPolicy() {
      assertEquals(SharedResourcePolicy.NO_SHARING, SharedResourcePolicy.valueOf("NO_SHARING"));
    }

    @Test
    @DisplayName("should have CONSERVATIVE policy")
    void shouldHaveConservativePolicy() {
      assertEquals(SharedResourcePolicy.CONSERVATIVE, SharedResourcePolicy.valueOf("CONSERVATIVE"));
    }

    @Test
    @DisplayName("should have MODERATE policy")
    void shouldHaveModeratePolicy() {
      assertEquals(SharedResourcePolicy.MODERATE, SharedResourcePolicy.valueOf("MODERATE"));
    }

    @Test
    @DisplayName("should have AGGRESSIVE_SHARING policy")
    void shouldHaveAggressiveSharingPolicy() {
      assertEquals(
          SharedResourcePolicy.AGGRESSIVE_SHARING,
          SharedResourcePolicy.valueOf("AGGRESSIVE_SHARING"));
    }
  }

  @Nested
  @DisplayName("CompatibilityLevel Enum Tests")
  class CompatibilityLevelEnumTests {

    @Test
    @DisplayName("should have all compatibility levels")
    void shouldHaveAllCompatibilityLevels() {
      final var levels = CompatibilityLevel.values();
      assertEquals(3, levels.length, "Should have 3 compatibility levels");
    }

    @Test
    @DisplayName("should have STRICT level")
    void shouldHaveStrictLevel() {
      assertEquals(CompatibilityLevel.STRICT, CompatibilityLevel.valueOf("STRICT"));
    }

    @Test
    @DisplayName("should have MODERATE level")
    void shouldHaveModerateLevel() {
      assertEquals(CompatibilityLevel.MODERATE, CompatibilityLevel.valueOf("MODERATE"));
    }

    @Test
    @DisplayName("should have LENIENT level")
    void shouldHaveLenientLevel() {
      assertEquals(CompatibilityLevel.LENIENT, CompatibilityLevel.valueOf("LENIENT"));
    }
  }

  @Nested
  @DisplayName("ResourceIsolationLevel Enum Tests")
  class ResourceIsolationLevelEnumTests {

    @Test
    @DisplayName("should have all resource isolation levels")
    void shouldHaveAllResourceIsolationLevels() {
      final var levels = ResourceIsolationLevel.values();
      assertEquals(5, levels.length, "Should have 5 resource isolation levels");
    }

    @Test
    @DisplayName("should have NONE level")
    void shouldHaveNoneLevel() {
      assertEquals(ResourceIsolationLevel.NONE, ResourceIsolationLevel.valueOf("NONE"));
    }

    @Test
    @DisplayName("should have BASIC level")
    void shouldHaveBasicLevel() {
      assertEquals(ResourceIsolationLevel.BASIC, ResourceIsolationLevel.valueOf("BASIC"));
    }

    @Test
    @DisplayName("should have MODERATE level")
    void shouldHaveModerateLevel() {
      assertEquals(ResourceIsolationLevel.MODERATE, ResourceIsolationLevel.valueOf("MODERATE"));
    }

    @Test
    @DisplayName("should have STRONG level")
    void shouldHaveStrongLevel() {
      assertEquals(ResourceIsolationLevel.STRONG, ResourceIsolationLevel.valueOf("STRONG"));
    }

    @Test
    @DisplayName("should have COMPLETE level")
    void shouldHaveCompleteLevel() {
      assertEquals(ResourceIsolationLevel.COMPLETE, ResourceIsolationLevel.valueOf("COMPLETE"));
    }
  }
}
