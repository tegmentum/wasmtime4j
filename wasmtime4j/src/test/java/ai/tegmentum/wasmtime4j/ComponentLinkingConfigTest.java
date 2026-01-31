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
 * Tests for {@link ComponentLinkingConfig} class.
 *
 * <p>ComponentLinkingConfig controls advanced component linking operations including strategy,
 * shared resource policy, capabilities, timeout, version mismatch, compatibility, interface
 * adaptation, resource isolation, and hot swapping.
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
    @DisplayName("should have static builder() factory method")
    void shouldHaveStaticBuilderMethod() throws NoSuchMethodException {
      final var method = ComponentLinkingConfig.class.getMethod("builder");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder() should be static");
    }
  }

  @Nested
  @DisplayName("Default Configuration Tests")
  class DefaultConfigurationTests {

    @Test
    @DisplayName("defaultConfig should have default values")
    void defaultConfigShouldHaveDefaults() {
      final ComponentLinkingConfig config = ComponentLinkingConfig.defaultConfig();

      assertNotNull(config, "defaultConfig should not be null");
      assertEquals(
          ComponentLinkingConfig.LinkingStrategy.ADAPTIVE, config.getStrategy(),
          "Default strategy should be ADAPTIVE");
      assertEquals(
          ComponentLinkingConfig.SharedResourcePolicy.CONSERVATIVE,
          config.getSharedResourcePolicy(),
          "Default shared resource policy should be CONSERVATIVE");
      assertTrue(
          config.getRequiredCapabilities().isEmpty(),
          "Default required capabilities should be empty");
      assertTrue(
          config.getLinkingParameters().isEmpty(),
          "Default linking parameters should be empty");
      assertEquals(
          Duration.ofMinutes(1), config.getLinkingTimeout(),
          "Default linking timeout should be 1 minute");
      assertFalse(config.isAllowVersionMismatch(), "Default allowVersionMismatch should be false");
      assertEquals(
          ComponentLinkingConfig.CompatibilityLevel.MODERATE,
          config.getCompatibilityLevel(),
          "Default compatibility level should be MODERATE");
      assertFalse(
          config.isInterfaceAdaptationEnabled(),
          "Default interface adaptation should be disabled");
      assertEquals(
          ComponentLinkingConfig.ResourceIsolationLevel.MODERATE,
          config.getResourceIsolation(),
          "Default resource isolation should be MODERATE");
      assertFalse(config.isHotSwappingEnabled(), "Default hot swapping should be disabled");
    }
  }

  @Nested
  @DisplayName("Production Configuration Tests")
  class ProductionConfigurationTests {

    @Test
    @DisplayName("productionConfig should use production-appropriate values")
    void productionConfigShouldUseProductionValues() {
      final ComponentLinkingConfig config = ComponentLinkingConfig.productionConfig();

      assertEquals(
          ComponentLinkingConfig.LinkingStrategy.SHARED_EVERYTHING,
          config.getStrategy(),
          "Production strategy should be SHARED_EVERYTHING");
      assertEquals(
          ComponentLinkingConfig.SharedResourcePolicy.AGGRESSIVE_SHARING,
          config.getSharedResourcePolicy(),
          "Production resource policy should be AGGRESSIVE_SHARING");
      assertEquals(
          ComponentLinkingConfig.CompatibilityLevel.STRICT,
          config.getCompatibilityLevel(),
          "Production compatibility should be STRICT");
      assertEquals(
          Duration.ofSeconds(30), config.getLinkingTimeout(),
          "Production timeout should be 30 seconds");
    }
  }

  @Nested
  @DisplayName("Development Configuration Tests")
  class DevelopmentConfigurationTests {

    @Test
    @DisplayName("developmentConfig should use development-appropriate values")
    void developmentConfigShouldUseDevelopmentValues() {
      final ComponentLinkingConfig config = ComponentLinkingConfig.developmentConfig();

      assertEquals(
          ComponentLinkingConfig.LinkingStrategy.STATIC_LINKING,
          config.getStrategy(),
          "Development strategy should be STATIC_LINKING");
      assertTrue(
          config.isAllowVersionMismatch(),
          "Development should allow version mismatch");
      assertTrue(
          config.isInterfaceAdaptationEnabled(),
          "Development should enable interface adaptation");
      assertTrue(config.isHotSwappingEnabled(), "Development should enable hot swapping");
      assertEquals(
          ComponentLinkingConfig.CompatibilityLevel.LENIENT,
          config.getCompatibilityLevel(),
          "Development compatibility should be LENIENT");
      assertEquals(
          Duration.ofMinutes(5), config.getLinkingTimeout(),
          "Development timeout should be 5 minutes");
    }
  }

  @Nested
  @DisplayName("Builder Fluent API Tests")
  class BuilderFluentApiTests {

    @Test
    @DisplayName("should set strategy via builder")
    void shouldSetStrategy() {
      final ComponentLinkingConfig config = ComponentLinkingConfig.builder()
          .strategy(ComponentLinkingConfig.LinkingStrategy.DYNAMIC_LINKING)
          .build();

      assertEquals(
          ComponentLinkingConfig.LinkingStrategy.DYNAMIC_LINKING,
          config.getStrategy(),
          "Strategy should be DYNAMIC_LINKING");
    }

    @Test
    @DisplayName("should set shared resource policy")
    void shouldSetSharedResourcePolicy() {
      final ComponentLinkingConfig config = ComponentLinkingConfig.builder()
          .sharedResourcePolicy(ComponentLinkingConfig.SharedResourcePolicy.NO_SHARING)
          .build();

      assertEquals(
          ComponentLinkingConfig.SharedResourcePolicy.NO_SHARING,
          config.getSharedResourcePolicy(),
          "Policy should be NO_SHARING");
    }

    @Test
    @DisplayName("should set required capabilities")
    void shouldSetRequiredCapabilities() {
      final ComponentCapability cap =
          ComponentCapability.featureCapability("test-feature");
      final ComponentLinkingConfig config = ComponentLinkingConfig.builder()
          .requiredCapabilities(Set.of(cap))
          .build();

      assertEquals(
          1, config.getRequiredCapabilities().size(),
          "Should have 1 required capability");
    }

    @Test
    @DisplayName("should set linking parameters")
    void shouldSetLinkingParameters() {
      final ComponentLinkingConfig config = ComponentLinkingConfig.builder()
          .linkingParameters(Map.of("key", "value"))
          .build();

      assertEquals(
          1, config.getLinkingParameters().size(),
          "Should have 1 linking parameter");
      assertEquals(
          "value", config.getLinkingParameters().get("key"),
          "Parameter value should be 'value'");
    }

    @Test
    @DisplayName("should set linking timeout")
    void shouldSetLinkingTimeout() {
      final ComponentLinkingConfig config = ComponentLinkingConfig.builder()
          .linkingTimeout(Duration.ofSeconds(45))
          .build();

      assertEquals(
          Duration.ofSeconds(45), config.getLinkingTimeout(),
          "Timeout should be 45 seconds");
    }

    @Test
    @DisplayName("should set resource isolation level")
    void shouldSetResourceIsolation() {
      final ComponentLinkingConfig config = ComponentLinkingConfig.builder()
          .resourceIsolation(ComponentLinkingConfig.ResourceIsolationLevel.COMPLETE)
          .build();

      assertEquals(
          ComponentLinkingConfig.ResourceIsolationLevel.COMPLETE,
          config.getResourceIsolation(),
          "Resource isolation should be COMPLETE");
    }
  }

  @Nested
  @DisplayName("Enum Tests")
  class EnumTests {

    @Test
    @DisplayName("LinkingStrategy should have all expected values")
    void linkingStrategyShouldHaveAllValues() {
      final ComponentLinkingConfig.LinkingStrategy[] values =
          ComponentLinkingConfig.LinkingStrategy.values();
      assertEquals(4, values.length, "LinkingStrategy should have 4 values");
    }

    @Test
    @DisplayName("SharedResourcePolicy should have all expected values")
    void sharedResourcePolicyShouldHaveAllValues() {
      final ComponentLinkingConfig.SharedResourcePolicy[] values =
          ComponentLinkingConfig.SharedResourcePolicy.values();
      assertEquals(4, values.length, "SharedResourcePolicy should have 4 values");
    }

    @Test
    @DisplayName("CompatibilityLevel should have all expected values")
    void compatibilityLevelShouldHaveAllValues() {
      final ComponentLinkingConfig.CompatibilityLevel[] values =
          ComponentLinkingConfig.CompatibilityLevel.values();
      assertEquals(3, values.length, "CompatibilityLevel should have 3 values");
    }

    @Test
    @DisplayName("ResourceIsolationLevel should have all expected values")
    void resourceIsolationLevelShouldHaveAllValues() {
      final ComponentLinkingConfig.ResourceIsolationLevel[] values =
          ComponentLinkingConfig.ResourceIsolationLevel.values();
      assertEquals(5, values.length, "ResourceIsolationLevel should have 5 values");
    }
  }
}
