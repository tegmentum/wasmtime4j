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

import ai.tegmentum.wasmtime4j.component.ComponentEngineConfig;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentEngineConfig} class.
 *
 * <p>ComponentEngineConfig provides configuration options for WebAssembly Component Model engine
 * creation including orchestration settings, distributed support, and enterprise management.
 */
@DisplayName("ComponentEngineConfig Tests")
class ComponentEngineConfigTest {

  @Nested
  @DisplayName("Default Values Tests")
  class DefaultValuesTests {

    @Test
    @DisplayName("should have componentModelEnabled true by default")
    void shouldHaveComponentModelEnabledByDefault() {
      final ComponentEngineConfig config = new ComponentEngineConfig();
      assertTrue(config.isComponentModelEnabled(), "Component model should be enabled by default");
    }

    @Test
    @DisplayName("should have witInterfaceValidation true by default")
    void shouldHaveWitInterfaceValidationByDefault() {
      final ComponentEngineConfig config = new ComponentEngineConfig();
      assertTrue(
          config.isWitInterfaceValidation(),
          "WIT interface validation should be enabled by default");
    }

    @Test
    @DisplayName("should have advancedOrchestration false by default")
    void shouldHaveAdvancedOrchestrationFalseByDefault() {
      final ComponentEngineConfig config = new ComponentEngineConfig();
      assertFalse(
          config.isAdvancedOrchestration(), "Advanced orchestration should be disabled by default");
    }

    @Test
    @DisplayName("should have distributedSupport false by default")
    void shouldHaveDistributedSupportFalseByDefault() {
      final ComponentEngineConfig config = new ComponentEngineConfig();
      assertFalse(
          config.isDistributedSupport(), "Distributed support should be disabled by default");
    }

    @Test
    @DisplayName("should have enterpriseManagement false by default")
    void shouldHaveEnterpriseManagementFalseByDefault() {
      final ComponentEngineConfig config = new ComponentEngineConfig();
      assertFalse(
          config.isEnterpriseManagement(), "Enterprise management should be disabled by default");
    }

    @Test
    @DisplayName("should have capabilityBasedSecurity true by default")
    void shouldHaveCapabilityBasedSecurityByDefault() {
      final ComponentEngineConfig config = new ComponentEngineConfig();
      assertTrue(
          config.isCapabilityBasedSecurity(),
          "Capability-based security should be enabled by default");
    }

    @Test
    @DisplayName("should have resourceManagement true by default")
    void shouldHaveResourceManagementByDefault() {
      final ComponentEngineConfig config = new ComponentEngineConfig();
      assertTrue(config.isResourceManagement(), "Resource management should be enabled by default");
    }

    @Test
    @DisplayName("should have componentCaching true by default")
    void shouldHaveComponentCachingByDefault() {
      final ComponentEngineConfig config = new ComponentEngineConfig();
      assertTrue(config.isComponentCaching(), "Component caching should be enabled by default");
    }
  }

  @Nested
  @DisplayName("Configuration Method Tests")
  class ConfigurationMethodTests {

    @Test
    @DisplayName("componentModelEnabled should set and return config")
    void componentModelEnabledShouldSetAndReturnConfig() {
      final ComponentEngineConfig config = new ComponentEngineConfig();
      final ComponentEngineConfig result = config.componentModelEnabled(false);

      assertFalse(config.isComponentModelEnabled(), "Component model should be disabled");
      assertEquals(config, result, "Should return this for method chaining");
    }

    @Test
    @DisplayName("enableAdvancedOrchestration should set and return config")
    void enableAdvancedOrchestrationShouldSetAndReturnConfig() {
      final ComponentEngineConfig config = new ComponentEngineConfig();
      final ComponentEngineConfig result = config.enableAdvancedOrchestration(true);

      assertTrue(config.isAdvancedOrchestration(), "Advanced orchestration should be enabled");
      assertEquals(config, result, "Should return this for method chaining");
    }

    @Test
    @DisplayName("enableDistributedSupport should set and return config")
    void enableDistributedSupportShouldSetAndReturnConfig() {
      final ComponentEngineConfig config = new ComponentEngineConfig();
      final ComponentEngineConfig result = config.enableDistributedSupport(true);

      assertTrue(config.isDistributedSupport(), "Distributed support should be enabled");
      assertEquals(config, result, "Should return this for method chaining");
    }

    @Test
    @DisplayName("enableEnterpriseManagement should set and return config")
    void enableEnterpriseManagementShouldSetAndReturnConfig() {
      final ComponentEngineConfig config = new ComponentEngineConfig();
      final ComponentEngineConfig result = config.enableEnterpriseManagement(true);

      assertTrue(config.isEnterpriseManagement(), "Enterprise management should be enabled");
      assertEquals(config, result, "Should return this for method chaining");
    }

    @Test
    @DisplayName("enableWitInterfaceEnhancement should set and return config")
    void enableWitInterfaceEnhancementShouldSetAndReturnConfig() {
      final ComponentEngineConfig config = new ComponentEngineConfig();
      final ComponentEngineConfig result = config.enableWitInterfaceEnhancement(true);

      assertTrue(config.isWitInterfaceEnhancement(), "WIT interface enhancement should be enabled");
      assertEquals(config, result, "Should return this for method chaining");
    }

    @Test
    @DisplayName("enableCapabilityBasedSecurity should set and return config")
    void enableCapabilityBasedSecurityShouldSetAndReturnConfig() {
      final ComponentEngineConfig config = new ComponentEngineConfig();
      final ComponentEngineConfig result = config.enableCapabilityBasedSecurity(false);

      assertFalse(
          config.isCapabilityBasedSecurity(), "Capability-based security should be disabled");
      assertEquals(config, result, "Should return this for method chaining");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("builder should create config with defaults")
    void builderShouldCreateConfigWithDefaults() {
      final ComponentEngineConfig config = ComponentEngineConfig.builder().build();

      assertNotNull(config, "Builder should create config");
      assertTrue(config.isComponentModelEnabled(), "Component model should be enabled by default");
    }

    @Test
    @DisplayName("builder should chain methods correctly")
    void builderShouldChainMethodsCorrectly() {
      final ComponentEngineConfig config =
          ComponentEngineConfig.builder()
              .enableAdvancedOrchestration(true)
              .enableDistributedSupport(true)
              .enableEnterpriseManagement(true)
              .enableWitInterfaceEnhancement(true)
              .enableCapabilityBasedSecurity(false)
              .build();

      assertTrue(config.isAdvancedOrchestration(), "Advanced orchestration should be enabled");
      assertTrue(config.isDistributedSupport(), "Distributed support should be enabled");
      assertTrue(config.isEnterpriseManagement(), "Enterprise management should be enabled");
      assertTrue(config.isWitInterfaceEnhancement(), "WIT interface enhancement should be enabled");
      assertFalse(
          config.isCapabilityBasedSecurity(), "Capability-based security should be disabled");
    }
  }

  @Nested
  @DisplayName("toEngineConfig Tests")
  class ToEngineConfigTests {

    @Test
    @DisplayName("should have toEngineConfig method")
    void shouldHaveToEngineConfigMethod() throws NoSuchMethodException {
      final Method method = ComponentEngineConfig.class.getMethod("toEngineConfig");
      assertNotNull(method, "toEngineConfig method should exist");
      assertEquals(EngineConfig.class, method.getReturnType(), "Should return EngineConfig");
    }

    @Test
    @DisplayName("toEngineConfig should return valid EngineConfig")
    void toEngineConfigShouldReturnValidEngineConfig() {
      final ComponentEngineConfig componentConfig = new ComponentEngineConfig();
      final EngineConfig engineConfig = componentConfig.toEngineConfig();

      assertNotNull(engineConfig, "Should return non-null EngineConfig");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle enabling and disabling same feature")
    void shouldHandleEnablingAndDisablingSameFeature() {
      final ComponentEngineConfig config = new ComponentEngineConfig();
      config.enableAdvancedOrchestration(true);
      config.enableAdvancedOrchestration(false);

      assertFalse(config.isAdvancedOrchestration(), "Feature should be disabled after toggling");
    }

    @Test
    @DisplayName("should handle all boolean configurations as false")
    void shouldHandleAllBooleanConfigurationsAsFalse() {
      final ComponentEngineConfig config =
          new ComponentEngineConfig()
              .componentModelEnabled(false)
              .enableAdvancedOrchestration(false)
              .enableDistributedSupport(false)
              .enableEnterpriseManagement(false)
              .enableWitInterfaceEnhancement(false)
              .enableCapabilityBasedSecurity(false);

      assertFalse(config.isComponentModelEnabled(), "Component model should be disabled");
      assertFalse(config.isAdvancedOrchestration(), "Advanced orchestration should be disabled");
      assertFalse(config.isDistributedSupport(), "Distributed support should be disabled");
      assertFalse(config.isEnterpriseManagement(), "Enterprise management should be disabled");
      assertFalse(
          config.isWitInterfaceEnhancement(), "WIT interface enhancement should be disabled");
      assertFalse(
          config.isCapabilityBasedSecurity(), "Capability-based security should be disabled");
    }

    @Test
    @DisplayName("should handle all boolean configurations as true")
    void shouldHandleAllBooleanConfigurationsAsTrue() {
      final ComponentEngineConfig config =
          new ComponentEngineConfig()
              .componentModelEnabled(true)
              .enableAdvancedOrchestration(true)
              .enableDistributedSupport(true)
              .enableEnterpriseManagement(true)
              .enableWitInterfaceEnhancement(true)
              .enableCapabilityBasedSecurity(true);

      assertTrue(config.isComponentModelEnabled(), "Component model should be enabled");
      assertTrue(config.isAdvancedOrchestration(), "Advanced orchestration should be enabled");
      assertTrue(config.isDistributedSupport(), "Distributed support should be enabled");
      assertTrue(config.isEnterpriseManagement(), "Enterprise management should be enabled");
      assertTrue(config.isWitInterfaceEnhancement(), "WIT interface enhancement should be enabled");
      assertTrue(config.isCapabilityBasedSecurity(), "Capability-based security should be enabled");
    }
  }
}
